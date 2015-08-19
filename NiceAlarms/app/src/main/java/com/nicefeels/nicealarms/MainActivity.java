package com.nicefeels.nicealarms;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

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

import java.util.Calendar;

public class MainActivity extends Activity implements
        GoogleMap.OnMapLongClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /**
     * Constants
     */
    private final LatLng DEFAULT_LOCATION = new LatLng(53.3096163, -6.3123088);
    private static final long MINIMUM_DISTANCECHANGE_FOR_UPDATE = 1; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATE = 1000; // in Milliseconds
    private static final long POINT_RADIUS = 1000; // in Meters
    private static final long PROX_ALERT_EXPIRATION = -1;
    private static final String POINT_LATITUDE_KEY = "POINT_LATITUDE_KEY";
    private static final String POINT_LONGITUDE_KEY = "POINT_LONGITUDE_KEY";
    private static final String PROX_ALERT_INTENT = "com.nicefeels.nicealarms.AlarmReceiver";

    public final static String TAG = "NiceFeelsApp";
    private final int DEFAULT_ZOOM = 10;
    /**
     * Local variables *
     */
    private Context context  = this;
    private LatLng userAlarmLocation;
    private GoogleApiClient mGoogleApiClient;
    private Button locationButton, resetButton;
    public static GoogleMap mMap;
    private boolean markerClicked;
    public static boolean marker;
    private Location mLastLocation;
    private float distanceBetween[];
    private MediaPlayer mp;
    AlertDialog.Builder alertDialogBuilder;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private LocationManager locationManager;


    /***
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mp = MediaPlayer.create(this, R.raw.chime);
        alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        setContentView(R.layout.activity_main);
        setUpMap();
        addListenerOnButton();
        buildGoogleApiClient();
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
                if (mLastLocation == null) {
                    Log.i(TAG, "No location yet");
                } else {
                    Log.i(TAG, "User Location: " + mLastLocation.getLatitude() + " " + mLastLocation.getLongitude());
                    Log.i(TAG, "Requested Location: " + userAlarmLocation.latitude + " " + userAlarmLocation.longitude);
                    Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), userAlarmLocation.latitude, userAlarmLocation.longitude, distanceBetween);
                    Log.i(TAG, "Distance " + distanceBetween[0] + " " + distanceBetween[1] + " " + distanceBetween[2] + " " + distanceBetween[3]);
                    mp.start();
                    if (distanceBetween[0] < 1000) {
                        Log.i(TAG, "You're nearly there, sorry haha");

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
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null){
            Log.i(TAG,""+mLastLocation.getLatitude());
            Log.i(TAG, "" + mLastLocation.getLongitude());
        };
    }

    /***
     * TODO
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {}


}
