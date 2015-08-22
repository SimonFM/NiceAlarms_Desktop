package com.nicefeels.nicealarms;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class MainActivity extends Activity implements
        GoogleMap.OnMapLongClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /**
     * Constants
     */
    private final LatLng DEFAULT_LOCATION = new LatLng(53.3096163, -6.3123088);
    public final static String TAG = "NiceFeelsApp";
    private final int DEFAULT_ZOOM = 10;
    /**
     * Local variables *
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
    public static AlarmManager manager;


    /***
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mp = MediaPlayer.create(this, R.raw.chime);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "Just before addMap");
        setUpMap();
        Log.i(TAG, "Just before addListener");
        addListenerOnButton();
        Log.i(TAG,"Just before buildAPI");
        buildGoogleApiClient();

    }

    /***
     *
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
                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(),
                            userAlarmLocation.latitude, userAlarmLocation.longitude, distanceBetween);
                } else {
                    Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(),
                            userAlarmLocation.latitude, userAlarmLocation.longitude, distanceBetween);
                    if (distanceBetween[0] < 1000) {
                        tooClose();
                    } else {
                        Log.i(TAG,"Just before start()");
                        start();
                    }
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
     *
     */
    public void tooClose(){
        Log.i(TAG, "You're nearly there, sorry haha");
        Toast.makeText(this, "Too close to your stop", Toast.LENGTH_SHORT).show();
        mMap.clear();
        marker = false;
    }

    /***
     *
     */
    public void cancel() {
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
        Toast.makeText(this, "Alarm Cancelled", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Alarm Cancelled");
    }

    /***
     *
     */
    public void cancel_1() {
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);

    }

    /***
     *
     */
    public void startAt10() {
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int interval = 1000 * 60 * 20;

        /* Set the alarm to start at 10:30 AM */
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        calendar.set(Calendar.MINUTE, 5);

        /* Repeating on every 20 minutes interval */
        manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 1000 * 60 * 20, pendingIntent);
    }
    /**
     *
     */
    public void start() {
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        GregorianCalendar time = new GregorianCalendar();
        /* Set the alarm to start at 10:30 AM */
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());


        //Toast.makeText(this, ""+time.HOUR_OF_DAY, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "lulz_1");
        //manager.set(AlarmManager.RTC_WAKEUP, time, PendingIntent.getBroadcast(this, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
        manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000*5, pendingIntent);
        Log.i(TAG, "lulz_2");
        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
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
        Log.i(TAG, "Connected: " + String.valueOf(mGoogleApiClient.isConnected()));
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
        //mLastLocation = null;
        if (mLastLocation != null){
            Log.i(TAG,""+mLastLocation.getLatitude());
            Log.i(TAG, "" + mLastLocation.getLongitude());
        }
        else{
            Log.i(TAG,"NOPE");
        }
    }

    /***
     * TODO
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {}


}
