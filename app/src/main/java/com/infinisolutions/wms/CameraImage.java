package com.infinisolutions.wms;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class CameraImage {
    final String TAG = this.getClass().getSimpleName();
    private String photoPath;
    private Context context;

    public String getPhotoPath() {
        return this.photoPath;
    }

    public CameraImage(Context context) {
        this.context = context;
    }

    public Intent takePhotoIntent() throws IOException {
        Intent in = new Intent("android.media.action.IMAGE_CAPTURE");
        if (in.resolveActivity(this.context.getPackageManager()) != null) {
            File photoFile = this.createImageFile();
            if (photoFile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                    in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    in.putExtra("output", FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", photoFile));
                }
                else {
                    in.putExtra("output", Uri.fromFile(photoFile));
                }
            }
        }

        return in;
    }

//    private File createImageFile() throws IOException {
//        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
//        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
//        this.photoPath = image.getAbsolutePath();
//        return image;
//    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        //TODO - version controlling for android 11
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        this.photoPath = image.getAbsolutePath();
        return image;
    }

    public void addToGallery() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(photoPath);
        Uri contentUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            mediaScanIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            contentUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", f);
        }
        else {
            contentUri = Uri.fromFile(f);
        }
        mediaScanIntent.setData(contentUri);
        this.context.sendBroadcast(mediaScanIntent);
    }
}