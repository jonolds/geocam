package com.jonolds.geocam;

import android.graphics.BitmapFactory;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String LOGTAG = "MainActivity";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private String mCurrentPhotoPath;
    private ImageView mImageView;
    static final int REQUEST_TAKE_PHOTO = 1;
    private static final String CAMERA_FP_AUTHORITY = "com.jonolds.fileprovider";
    private FusedLocationProviderClient myLocPro;

    public void getLong(View view) {
    }

    /*onCreate callback.
    Set up all private instances and check for external write permissions
    @param savedInstanceState*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myLocPro = LocationServices.getFusedLocationProviderClient(this);

        //This function returns a boolean for if you have permissions
        isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //Set the mImageView private member to associate with the ImageView widget on the MainLayout
        mImageView = findViewById(R.id.thumb);
    }

    //Boolean function to check if permissions are granted. If not, create an activity to request permissions
    public boolean isPermissionGranted(String permission) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(String.valueOf("android." + permission)) == PackageManager.PERMISSION_GRANTED) {
                Log.e(LOGTAG,"Permission is granted");
                return true;
            } else {
                Log.e(LOGTAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{permission}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(LOGTAG,"Permission is granted");
            return true;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.e(LOGTAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }

    public void takePic(View view) {
        dispatchTakePic();
    }

    //dispatchTakePictureIntent() -- Start the camera Intent
    private void dispatchTakePic() {
        //Create an Intent to use the default Camera Application
        Intent takePicIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePicIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(LOGTAG,ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                //Use the FileProvider defined in the Manifest as the authority for sharing across the Intent
                //Provides a content:// URI instead of a file:// URI which throws an error post API 24
                Uri photoURI = FileProvider.getUriForFile(this,CAMERA_FP_AUTHORITY,photoFile);
                //Put the content:// URI as the output location for the photo
                takePicIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                //Start the Camera Application for a result
                startActivityForResult(takePicIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        //Use ExternalStoragePublicDirectory so that it is accessible for the MediaScanner
        //Associate the directory with your application by adding an additional subdirectory
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyCamera");
        if(!storageDir.exists()){
            storageDir.mkdir();
        }
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.d(LOGTAG,"Storage Directory: " + storageDir.getAbsolutePath());
        return image;
    }

    //This function puts photo from mCurrentPhotoPath and allows the Media Gallery to access it. Photo must be publicly accessible
    private void galleryAddPic() {
        //Two ways to use the MediaScanner
        //Use the MediaScannerConnection API
        //Has callback to see if it works. URI returns false if does not work
//        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{mCurrentPhotoPath}, null, new MediaScannerConnection.OnScanCompletedListener() {
//            @Override
//            public void onScanCompleted(String path, Uri uri) {
//                Log.d(LOGTAG,path);
//                Log.d(LOGTAG,uri.toString());
//            }
//        });

        //Fire off an Intent to use the MediaScanner
        //Place the URI of the file as the data
        File f = new File(mCurrentPhotoPath);
        Intent myMediaIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        myMediaIntent.setData(Uri.fromFile(f));
        getApplicationContext().sendBroadcast(myMediaIntent);
    }

    /**
     * onActivityResult callback fires after startActivityForResult finishes the new activity
     * @param requestCode -- Int associated with the activity request for switching activity
     * @param resultCode -- Result value from the startedActivity
     * @param data -- Return Values
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //If the Request is to take a photo and it returned OK
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //Extras does not contain bitmap if Image is saved to a file
            //Bundle extras = data.getExtras();
            //Create a BitmapFactoryOptions object to get Bitmap from a file
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            //Load the Bitmap from the Image file created by the camera
            Bitmap imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath,options);
            //Set the ImageView to the bitmap
            mImageView.setImageBitmap(imageBitmap);
            //Add the photo to the gallery
            galleryAddPic();
        }
    }
}