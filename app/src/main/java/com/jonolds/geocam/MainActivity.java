package com.jonolds.geocam;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, OnConnectionFailedListener, LocationListener {
    private static final String LOGTAG = "MainActivity";
    private FusedLocationProviderClient myLocPro;
    public static Location loc;
    private LocationRequest locReq;
    private LocationCallback myLocCB;
    public boolean boolUpdateLoc = false;
    String[] projection = {
            MarkerProvider.TODO_TABLE_COL_ID,
            MarkerProvider.TODO_TABLE_COL_TITLE,
            MarkerProvider.TODO_TABLE_COL_LATITUDE,
            MarkerProvider.TODO_TABLE_COL_LONGITUDE,
            MarkerProvider.TODO_TABLE_COL_ADDRESS
    };

    /*onCreate callback.
    Set up all private instances and check for external write permissions
    @param savedInstanceState*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        checkPermissions();
        loc = new Location("");
        myLocPro = LocationServices.getFusedLocationProviderClient(this);
        myLocCB = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    loc.setLatitude(location.getLatitude());
                    loc.setLongitude(location.getLongitude());
                    TextView lat = findViewById(R.id.latView);
                    TextView lon = findViewById(R.id.longView);
                    lat.setText(String.valueOf(loc.getLatitude()));
                    lon.setText(String.valueOf(loc.getLongitude()));

                }
            }
        };

        //Set the mImageView private member to associate with the ImageView widget on the MainLayout
    }

    public void takePic(View view) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.cam_placeholder, new CamFragment());
        ft.addToBackStack(null);
        ft.commit();
    }

    public void openMap(View view) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.map_placeholder, new MapFragment());
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(boolUpdateLoc)
        startLocationUpdates();
        tableData();
    }

    public void locSetup() {
        if(PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == 0) {

            myLocPro.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>(){
                @Override
                public void onSuccess(Location location) {
                    if(location != null) {
                        loc.setLatitude(location.getLatitude());
                        loc.setLongitude(location.getLongitude());
                    }
                }
            });
            //
            locReq = new LocationRequest().setInterval(5000).setFastestInterval(2000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationSettingsRequest.Builder LSRB = new LocationSettingsRequest.Builder();
            LSRB.addLocationRequest(locReq);

            SettingsClient client = LocationServices.getSettingsClient(this);
            Task<LocationSettingsResponse> task = client.checkLocationSettings(LSRB.build());

            task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                @Override
                public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                    Log.e("on", "Success");
                }
            });

            task.addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("onFail", String.valueOf(e));
                    int statusCode = ((ApiException) e).getStatusCode();
                    switch(statusCode) {
                        case CommonStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvable = (ResolvableApiException) e;
                                resolvable.startResolutionForResult(MainActivity.this, 1);
                            } catch (IntentSender.SendIntentException sendEx) {
                                Log.e("statusCode ", String.valueOf(sendEx));
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            break;
                    }
                }
            });

        }

    }


    public void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION};
            ArrayList<String> permsNeeded = new ArrayList<>();

            for (int i = 0; i < permissions.length; i++) {
                if (checkSelfPermission(String.valueOf("android." + permissions[i])) == PackageManager.PERMISSION_GRANTED) {
                    Log.e(permissions[i], " is granted");
                } else {
                    Log.e(permissions[i], " is revoked");
                    permsNeeded.add(permissions[i]);
                }
            }
            if(permsNeeded.size() > 0) {
                String[] perms = permsNeeded.toArray(new String[permsNeeded.size()]);
                ActivityCompat.requestPermissions(this, perms, 1);
            }
        }
        else
            Log.e("api greater or ", "equal to 23");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i = 0; i < permissions.length; i++)
            if(grantResults[i]== PackageManager.PERMISSION_GRANTED){
                Log.e(LOGTAG,"Permission: "+permissions[i]+ " was "+grantResults[i]);
            }
            boolUpdateLoc = true;
        locSetup();
    }

    public static Location getLoc() {
        return loc;
    }

    public void setLatLong(View view) {
        ((TextView)findViewById(R.id.longView)).setText(String.valueOf(getLoc().getLongitude()));
        ((TextView)findViewById(R.id.latView)).setText(String.valueOf(getLoc().getLatitude()));
    }

    public void startLocationUpdates() {
        if(PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == 0)
            myLocPro.requestLocationUpdates(locReq, myLocCB, null);
    }

    public void tableData() {
        Cursor cursor = getContentResolver().query(MarkerProvider.CONTENT_URI,projection,null,null,"_ID ASC");
        if(cursor != null) {
            if(cursor.getCount() > 0){
                for(int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    Log.e("Cursor pos(i) ", String.valueOf(i));
                    Log.e("ID ", String.valueOf(cursor.getInt(0)));
                    Log.e("Title ", cursor.getString(1));
                    Log.e("Latitude ", cursor.getString(2));
                    Log.e("Longitude ", String.valueOf(cursor.getInt(3)));
                    Log.e("Address ", cursor.getString(4));
                }
            }
            cursor.close();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if(boolUpdateLoc)
            startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
    }
}