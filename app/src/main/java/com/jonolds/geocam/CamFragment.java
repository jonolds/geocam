package com.jonolds.geocam;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class CamFragment extends Fragment {
    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String CAMERA_FP_AUTHORITY = "com.jonolds.fileprovider";
    private static final String LOGTAG = "MainActivity";
    private String mCurrentPhotoPath;
    private ImageView mImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cam, container, false);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        takePic();
    }

    @Override
    public void onStart() {
        super.onStart();
        mImageView = getView().findViewById(R.id.thumb);
    }
    //takePic() -- Start the camera Intent
    public void takePic() {
        //Create an Intent to use the default Camera Application
        Intent takePicIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePicIntent.resolveActivity(getActivity().getPackageManager()) != null) {
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
                Uri photoURI = FileProvider.getUriForFile(getContext(),CAMERA_FP_AUTHORITY,photoFile);
                //Put the content:// URI as the output location for the photo
                takePicIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                //Start the Camera Application for a result
                startActivityForResult(takePicIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    /**
     * onActivityResult callback fires after startActivityForResult finishes the new activity
     * @param requestCode -- Int associated with the activity request for switching activity
     * @param resultCode -- Result value from the startedActivity
     * @param data -- Return Values
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + "_";
        //Use ExternalStoragePublicDirectory so that it is accessible for the MediaScanner
        //Associate the directory with your application by adding an additional subdirectory
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyCamera");
        if(!storageDir.exists()){
            System.out.println(storageDir.mkdir());
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
        Location curLoc = MainActivity.getLoc();
        String lat = curLoc.convert(curLoc.getLatitude(), 1);
        Log.e(String.valueOf(lat), "lat");
      //  Log.e(String.valueOf(latS[0]), "latS[0]");


        File f = new File(mCurrentPhotoPath);


        try {
            Log.e(mCurrentPhotoPath, String.valueOf(curLoc.getLongitude()));
            ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, String.valueOf("36/1,6/1,5/1"));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, String.valueOf("94/1,9/1,27/1"));
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent myMediaIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        myMediaIntent.setData(Uri.fromFile(f));
        getActivity().getApplicationContext().sendBroadcast(myMediaIntent);
    }
}