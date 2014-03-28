package org.datakick.app;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class DatakickService extends IntentService {
    private static final String TAG = "DatakickService";
    private static final String ACTION_UPLOAD_PHOTO = "org.datakick.app.action.UPLOAD_PHOTO";
    private static final String EXTRA_GTIN = "org.datakick.app.extra.GTIN";
    private static final String EXTRA_PHOTO_PATH = "org.datakick.app.extra.PHOTO_PATH";

    /**
     * Starts this service to upload a photo to Datakick. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void uploadPhoto(Context context, String gtin, String photoPath) {
        Intent intent = new Intent(context, DatakickService.class);
        intent.setAction(ACTION_UPLOAD_PHOTO);
        intent.putExtra(EXTRA_GTIN, gtin);
        intent.putExtra(EXTRA_PHOTO_PATH, photoPath);
        context.startService(intent);
    }

    public DatakickService() {
        super("DatakickService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPLOAD_PHOTO.equals(action)) {
                final String photoPath = intent.getStringExtra(EXTRA_PHOTO_PATH);
                final String gtin = intent.getStringExtra(EXTRA_GTIN);
                handlePhotoUpload(gtin, photoPath);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handlePhotoUpload(String gtin, String photoPath) {
        //Scale image down 50%
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap photoBitmap = BitmapFactory.decodeFile(photoPath, options);
        File output = null;
        FileOutputStream out = null;

        try {
            output = createImageFile();
        } catch (IOException e) {
            Log.e(TAG,"Couldn't create new output file for scaled image");
            return;
        }

        try {
            out = new FileOutputStream(output.getAbsolutePath());
        } catch (FileNotFoundException e) {
            Log.e(TAG,"Couldn't find output file for scaled image");
            return;
        }

        try {
            photoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't write bitmap to ByteStream");
            return;
        } finally {
            try{
                out.close();
            } catch(Throwable ignore) {}
        }

        String url = "https://www.datakick.org/api/items/" + gtin + "/images";

        HttpResponse response = null;
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(url);
        MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
        multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        multipartEntity.addPart("image", new FileBody(new File(output.getAbsolutePath())));
        post.setEntity(multipartEntity.build());

        try {
            response = client.execute(post);
        } catch (IOException ex) {
            Log.e(TAG, "Failed uploading " + photoPath, ex);
        }

        if (response != null ) {
            HttpEntity entity = response.getEntity();
            try {
                entity.consumeContent();
            } catch (IOException ex) {
                Log.e(TAG, "Couldn't consume the response body");
            }
            client.getConnectionManager().shutdown();
        }

        // Delete the temporary file
        output.delete();
    }

    public static File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_DATAKICK_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }
}
