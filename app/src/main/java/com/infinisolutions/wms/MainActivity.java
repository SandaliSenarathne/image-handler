package com.infinisolutions.wms;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 2;
    private static final int CAMERA_PERMISSION = 1;

    private File outputFileName;
    private File mLastTakenImageAsJPEGFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch Camera
                checkPermission();
            }
        });
    }

    public  void checkPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            try {
                outputFileName = createImageFile(".tmp");
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputFileName));
                // TODO
                startActivityForResult(intent, CAMERA_REQUEST);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                try {
                    outputFileName = createImageFile(".tmp");
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputFileName));
                    // TODO
                    startActivityForResult(intent, CAMERA_REQUEST);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                Toast.makeText(this, "Set permission to use camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == CAMERA_REQUEST){
                processPhoto(data);
            }
        }
    }

    protected void processPhoto(Intent i)
    {
        int imageExifOrientation = 0;

        try
        {
            ExifInterface exif;
            exif = new ExifInterface(outputFileName.getAbsolutePath());
            imageExifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }

        int rotationAmount = 0;

        if (imageExifOrientation == ExifInterface.ORIENTATION_ROTATE_270)
        {
            // Need to do some rotating here...
            rotationAmount = 270;
        }
        if (imageExifOrientation == ExifInterface.ORIENTATION_ROTATE_90)
        {
            // Need to do some rotating here...
            rotationAmount = 90;
        }
        if (imageExifOrientation == ExifInterface.ORIENTATION_ROTATE_180)
        {
            // Need to do some rotating here...
            rotationAmount = 180;
        }

        int targetW = 240;
        int targetH = 320;

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(outputFileName.getAbsolutePath(), bmOptions);
        int photoWidth = bmOptions.outWidth;
        int photoHeight = bmOptions.outHeight;

        int scaleFactor = Math.min(photoWidth/targetW, photoHeight/targetH);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap scaledDownBitmap = BitmapFactory.decodeFile(outputFileName.getAbsolutePath(), bmOptions);

        if (rotationAmount != 0)
        {
            Matrix mat = new Matrix();
            mat.postRotate(rotationAmount);
            scaledDownBitmap = Bitmap.createBitmap(scaledDownBitmap, 0, 0, scaledDownBitmap.getWidth(), scaledDownBitmap.getHeight(), mat, true);
        }

        ImageView iv2 = (ImageView) findViewById(R.id.photoImageView);
        iv2.setImageBitmap(scaledDownBitmap);

        FileOutputStream outFileStream = null;
        try
        {
            mLastTakenImageAsJPEGFile = createImageFile(".jpg");
            outFileStream = new FileOutputStream(mLastTakenImageAsJPEGFile);
            scaledDownBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outFileStream);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private File createImageFile(String fileExtensionToUse) throws IOException
    {

        File storageDir = new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                ),
                "whatsapp"
        );

        if(!storageDir.exists())
        {
            if (!storageDir.mkdir())
            {
                Toast.makeText(this, "was not able to create it", Toast.LENGTH_SHORT).show();
            }
        }
        if (!storageDir.isDirectory())
        {
            Toast.makeText(this, "Don't think there is a dir there.", Toast.LENGTH_SHORT).show();
        }

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "FOO_" + timeStamp + "_image";

        File image = File.createTempFile(
                imageFileName,
                fileExtensionToUse,
                storageDir
        );

        return image;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}