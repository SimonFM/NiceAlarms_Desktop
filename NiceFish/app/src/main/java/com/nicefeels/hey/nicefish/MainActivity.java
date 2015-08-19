package com.nicefeels.hey.nicefish;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.GoogleApiClient;


import java.io.FileOutputStream;
import java.util.Calendar;

public class MainActivity extends Activity implements
        GoogleMap.OnMapLongClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /**
     * Constants
     */
    private final LatLng DEFAULT_LOCATION = new LatLng(53.3096163, -6.3123088);
    private final String TAG = "NiceFish";
    private final int DEFAULT_ZOOM = 10;
    private Context context  = this;
    private LatLng userAlarmLocation;
    private Calendar myCalendar;
    private GoogleApiClient mGoogleApiClient;
    /**
     * Local variables *
     */
    private Button locationButton, resetButton;
    public static GoogleMap mMap;
    private boolean markerClicked;
    public static boolean marker;
    private Location mLastLocation;
    private float distanceBetween[];
    private MediaPlayer mp;
    AlertDialog.Builder alertDialogBuilder;

    /***
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpMap();
        addListenerOnButton();
        buildGoogleApiClient();

        String filename = "testing/locations.txt";
        String string = "Hello world!";
        Log.i(TAG, "Hello world!");
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
            Log.i(TAG, "Hello world! 2");
        } catch (Exception e) {
            e.printStackTrace();
            //Log.i(TAG, e.printStackTrace());
        }

    }
    /***
     *
     */
    public void addListenerOnButton() {
        locationButton = (Button) findViewById(R.id.locationButton);
        resetButton = (Button) findViewById(R.id.resetButton);
        distanceBetween = new float[4];
        locationButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if(mLastLocation == null){
//                    Log.i(TAG, "No location yet");
//                    AlertDialog alertDialog = alertDialogBuilder.create();
//                    alertDialog.show();
                }
                else{
                    Log.i(TAG, "User Location: "+ mLastLocation.getLatitude()+" "+mLastLocation.getLongitude());
                    Log.i(TAG, "Requested Location: " + userAlarmLocation.latitude + " " + userAlarmLocation.longitude);
                    Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(),userAlarmLocation.latitude, userAlarmLocation.longitude, distanceBetween);
                    Log.i(TAG, "Distance " + distanceBetween[0] + " " + distanceBetween[1] + " "+ distanceBetween[2]+ " "+ distanceBetween[3]);
                    mp.start();
                    if(distanceBetween[0] < 1000) {
                        Log.i(TAG,"You're nearly there, sorry haha");
                        mMap.clear();
                        marker = false;
                    }

                }

            }
        });

        resetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                marker = false;
            }
        });
    }


    /***
     *
     */
    public void setUpMap(){
        FragmentManager myFragmentManager = getFragmentManager();
        MapFragment myMapFragment = (MapFragment) myFragmentManager.findFragmentById(R.id.mapFragment);
        mMap = myMapFragment.getMap();
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMyLocationEnabled(true);
        mMap.setOnMapLongClickListener(this);
        markerClicked = false;
        marker = false;
        mMap.setTrafficEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Handles A long press on the screen
     * @param point
     */
    @Override
    public void onMapLongClick(LatLng point) {
        if (!marker) {
            mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .draggable(true));
            markerClicked = false;
            marker = true;
            userAlarmLocation = point;

            CustomDialog dialog = new CustomDialog(this,"Would you like to replace the tag?");
            dialog.show();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        Log.i("Connected?", String.valueOf(mGoogleApiClient.isConnected()));
        //new Thread(new GetContent()).start();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}
    /***
     * TODO
     * @param connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Intent intent = new Intent(this, LocationReceiver.class);
        PendingIntent locationIntent = PendingIntent.getBroadcast(getApplicationContext(), 14872, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        mLocationClient.requestLocationUpdates(mLocationRequest, locationIntent);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null){
            Log.i(TAG,""+mLastLocation.getLatitude());
            Log.i(TAG,""+mLastLocation.getLongitude());
        };
    }

    /***
     * TODO
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {}
}
