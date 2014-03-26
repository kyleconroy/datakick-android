package org.datakick.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;


public class PhotoActivity extends Activity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "Datakick";
    private static final String STATE_FILES = "DatakickFiles";

    private ArrayAdapter<String> photoAdapter;
    private ArrayList<String> photoPaths;
    private String gtin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        // Set name here or something
        ListView listview = (ListView)findViewById(R.id.listView);

        gtin = getIntent().getStringExtra("gtin");

        if (savedInstanceState == null) {
            photoPaths = new ArrayList<String>();
        } else {
            photoPaths = savedInstanceState.getStringArrayList(STATE_FILES);
        }

        photoAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, photoPaths);
        photoAdapter.setNotifyOnChange(true);
        listview.setAdapter(photoAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putStringArrayList(STATE_FILES, photoPaths);
        super.onSaveInstanceState(savedInstanceState);
    }

    private File createImageFile() throws IOException {
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
        photoAdapter.add(image.getAbsolutePath());
        return image;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.photo, menu);
        return true;
    }

    private class UploadPhotosTask extends AsyncTask<String, Integer, Integer> {
        private String gtin;

        public UploadPhotosTask(String gtin) {
            this.gtin = gtin;
        }

        protected Integer doInBackground(String... paths) {
            int count = paths.length;
            String url = "https://www.datakick.org/api/items/" + gtin + "/images";
            for (int i = 0; i < count; i++) {
                HttpResponse response = null;
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(url);
                MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
                multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                multipartEntity.addPart("image", new FileBody(new File(paths[i])));
                post.setEntity(multipartEntity.build());

                try {
                    response = client.execute(post);
                } catch (IOException ex) {
                    Log.e(TAG, "Failed uploading " + paths[i], ex);
                    continue;
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
            }
            return 0;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Long result) {
        }
    }

    public void uploadProduct(View view) {
        String[] paths = photoPaths.toArray(new String[photoPaths.size()]);
        new UploadPhotosTask(gtin).execute(paths);
    }

    public void takePhoto(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Couldn't create image file");
                return;
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
