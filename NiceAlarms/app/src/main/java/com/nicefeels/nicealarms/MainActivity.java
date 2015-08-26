package com.nicefeels.nicealarms;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.GoogleApiClient;

public class MainActivity extends Activity implements
        GoogleMap.OnMapLongClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    /**
     * Constants
     */
    private final LatLng TEST_LOCATION = new LatLng(53.3096163, -6.3123088);
    public final static String TAG = "NiceFeelsApp";
    public final int MINIMUM_DISTANCE = 1000;
    private final int DEFAULT_ZOOM = 10;
    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    /**
     * Instance variables
     */
    private Context context  = this;
    public static LatLng userAlarmLocation;
    public static GoogleApiClient mGoogleApiClient;
    private Button locationButton, resetButton;
    public static GoogleMap mMap;
    private boolean markerClicked;
    public static boolean marker;
    public static Location mLastLocation;
    public static float distanceBetween[];
    public static MediaPlayer mp;
    public static PendingIntent pendingIntent;
    private AlarmManager manager;
    private LocationManager locationMan;
    private Criteria criteria;
    private String provider;


    /***
     * Creates the app in order to be used.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildGoogleApiClient();
        locationMan = (LocationManager)getSystemService(LOCATION_SERVICE);
        criteria = new Criteria();
        provider = locationMan.getBestProvider(criteria, true);
        mLastLocation = locationMan.getLastKnownLocation(provider);
        mp = MediaPlayer.create(this, R.raw.chime);

        setContentView(R.layout.activity_main);
        addListenerOnButton();
        setUpMap();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /***
     * A method that adds the various buttons to the activity. If the reset button is pressed,
     * then the alarm is cancelled, or when the set Location is pressed, then the alarm is set.
     *
     * If the user doesn't have their location services activated, then a small message is displayed
     * and nothing else can be done.
     */
    public void addListenerOnButton() {
        locationButton = (Button) findViewById(R.id.locationButton);
        resetButton = (Button) findViewById(R.id.resetButton);
        distanceBetween = new float[4];
        Intent alarmIntent = new Intent(MainActivity.this, Alarm.class);
        pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, alarmIntent, 0);
        locationButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mLastLocation == null) {
                    Log.i(TAG, "No location yet");
                    mLastLocation = locationMan.getLastKnownLocation(provider);
                } else {
                    Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(),
                            userAlarmLocation.latitude, userAlarmLocation.longitude, distanceBetween);
                    if (distanceBetween[0] < MINIMUM_DISTANCE) tooClose();
                    else start();

                }
            }
        });

        resetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                marker = false;
                cancel();
            }
        });
    }

    /**
     * Tells the user they are too close to their stop.
     */
    public void tooClose(){
        Toast.makeText(this, "Too close to your stop", Toast.LENGTH_SHORT).show();
        mMap.clear();
        marker = false;
    }

    /***
     * A method that cancells the alarm that was set and displays a message saying
     * this to the user.
     */
    public void cancel() {
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
        Toast.makeText(this, "Alarm Cancelled", Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a message saying the alarm was started, also starts the alarm
     */
    public void start() {
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 5000, pendingIntent);
        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
    }

    /***
     *  Sets up the map, taken from google's tutorials
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
        if(mLastLocation != null) {
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
            mMap.animateCamera(cameraUpdate);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            Log.i(TAG,"Building!");
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        }
        else mGoogleApiClient.reconnect();


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
            CustomDialog dialog = new CustomDialog(this,"Would you like to replace the tag?");
            dialog.show();
            userAlarmLocation = point;
        }

    }

    /***
     * A method that is run when the app starts
     * purely for debugging atm.
     */
    @Override
    protected void onStart() {
        super.onStart();

        Log.i(TAG, "Connected: " + String.valueOf(mGoogleApiClient.isConnected()));
        //new Thread(new GetContent()).start();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("Connected failed", String.valueOf(mGoogleApiClient.isConnected()));
    }
    /***
     * A method that runs when the API is connected to.
     * @param connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
    }

    /***
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {}
}
