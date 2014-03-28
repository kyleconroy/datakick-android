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
    private static final String STATE_CURRENT_FILE = "DatakickFile";

    private ArrayAdapter<String> photoAdapter;
    private ArrayList<String> photoPaths;
    private String currentPath;
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
            currentPath = "";
        } else {
            photoPaths = savedInstanceState.getStringArrayList(STATE_FILES);
            currentPath = savedInstanceState.getString(STATE_CURRENT_FILE);
        }

        photoAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, photoPaths);
        photoAdapter.setNotifyOnChange(true);
        listview.setAdapter(photoAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putStringArrayList(STATE_FILES, photoPaths);
        savedInstanceState.putString(STATE_CURRENT_FILE, currentPath);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.photo, menu);
        return true;
    }

    public void takePhoto(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = DatakickService.createImageFile();
                currentPath = photoFile.getAbsolutePath();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                photoAdapter.add(currentPath);
                DatakickService.uploadPhoto(getApplicationContext(), gtin, currentPath);
                currentPath = "";
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
