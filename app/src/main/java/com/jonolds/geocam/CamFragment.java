package com.jonolds.geocam;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class CamFragment extends Fragment {
    static final int REQUEST_TAKE_PHOTO = 20;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    private void takePic() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            File image = null;
            try {
                image = getTempFile();
            } catch (IOException e) {
                Log.e("IOException", "getTempFile");
            }

            if(image != null) {
                Uri picURI = FileProvider.getUriForFile(getContext(), "com.jonolds.android.fileprovider", image);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, picURI);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File getTempFile() throws IOException {
        String imgFileName = "JPEG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File dir = getContext().getFilesDir();
        return File.createTempFile(imgFileName, ".jpg", dir);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                ImageView myImageView = getActivity().findViewById(R.id.pic);
                myImageView.setImageBitmap(imageBitmap);
            }
        }
    }
}