package com.infinisolutions.wms;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.squareup.picasso.Picasso;
import com.yalantis.ucrop.UCrop;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;

public class MainActivity extends AppCompatActivity {

    final public int CAMERA_REQUEST_PROFILE_PICTURE = 2;
    final public int GALLERY_REQUEST_PROFILE_PICTURE = 4;
    final public int CROP_REQUEST_PROFILE_PICTURE = 6;

    public CameraImage cameraProfilePicture;
    private String currentPhotoPath = "";

    ImageView imgProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imgProfile = findViewById(R.id.imgProfile);

        cameraProfilePicture = new CameraImage(this);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch Camera
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                    AlertDialog alertDialog = new AlertDialog.Builder(getApplicationContext()).create();
                    alertDialog.setTitle("Sorry !");
                    alertDialog.setMessage("As you are using a latest version of android this feature is currently not available. Please visit our website to update your profile picture.");
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
                else {
                    checkPermission(CAMERA_REQUEST_PROFILE_PICTURE);
                }
            }
        });
    }

    public  void checkPermission(int cameraRequest){
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            showImagePickerOptions(MainActivity.this, cameraRequest);
                        }

                        if (report.isAnyPermissionPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    public void showImagePickerOptions(Context context, int cameraRequest) {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (cameraRequest == CAMERA_REQUEST_PROFILE_PICTURE)
            builder.setTitle("Set Profile Picture");

        String[] options = {"Take a picture", "Choose from gallery"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    try {
                        if (cameraRequest == CAMERA_REQUEST_PROFILE_PICTURE){
                            startActivityForResult(cameraProfilePicture.takePhotoIntent(), CAMERA_REQUEST_PROFILE_PICTURE);
                            cameraProfilePicture.addToGallery();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Something went wrong! Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 1:
                    if (cameraRequest == CAMERA_REQUEST_PROFILE_PICTURE){
                        openImagesDocument(GALLERY_REQUEST_PROFILE_PICTURE);
                    }
                    break;
            }
        });

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Grant Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("go to settings", (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton("cancel", (dialog, which) -> dialog.cancel());
        builder.show();

    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", this.getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }


    //For choose image from gallery
    private void openImagesDocument(int request) {
        Intent pictureIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pictureIntent.setType("image/*");
        pictureIntent.addCategory(Intent.CATEGORY_OPENABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String[] mimeTypes = new String[]{"image/jpeg", "image/png"};
            pictureIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        }
        startActivityForResult(Intent.createChooser(pictureIntent, "Select Picture"), request);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == CAMERA_REQUEST_PROFILE_PICTURE){
                String photoPath = cameraProfilePicture.getPhotoPath();
                Uri uri=Uri.fromFile(new File((photoPath)));
                cropImage(uri, uri, 2, 1, CROP_REQUEST_PROFILE_PICTURE, false);
            }else if(requestCode == GALLERY_REQUEST_PROFILE_PICTURE){
                if (data != null) {
                    try {
                        Uri sourceUri = data.getData();
                        File file = getImageFile();
                        Uri destinationUri = Uri.fromFile(file);
                        cropImage(sourceUri, destinationUri, 2, 1, CROP_REQUEST_PROFILE_PICTURE, false);
                    } catch (Exception e) {
                        Toast.makeText(this, "Something went wrong! Choose another image.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if(requestCode == CROP_REQUEST_PROFILE_PICTURE){
                if (data != null) {
                    Uri uri = UCrop.getOutput(data);
                    //TODO - what to do next, API call or other
                    Picasso.get().load(uri).placeholder(R.drawable.ic_launcher_background).fit().centerInside().into(imgProfile);
                }
            }
        }
    }

    private void cropImage(Uri sourceUri, Uri destinationUri, int aspectRatioX, int aspectRatioY,int cropRequest, boolean isCircular) {
        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(isCircular);
        options.setCropFrameColor(ContextCompat.getColor(this, R.color.design_default_color_background));
        UCrop.of(sourceUri, destinationUri)
                .withMaxResultSize(1000, 500)
                .withAspectRatio(aspectRatioX, aspectRatioY)
                .withOptions(options)
                .start(this, cropRequest);

    }

    private File getImageFile() throws IOException {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        //TODO - version controlling using MediaStore
        File storageDir = new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM
                ), "Camera"
        );
        System.out.println(storageDir.getAbsolutePath());
        if (storageDir.exists())
            System.out.println("File exists");
        else
            System.out.println("File not exists");
        File file = File.createTempFile(
                imageFileName, ".jpg", storageDir
        );
        currentPhotoPath = "file:" + file.getAbsolutePath();
        return file;
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