package com.nicefeels.nicealarms;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity implements
        GoogleMap.OnMapLongClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{

    /**
     * Constants
     */
    public final String TAG = "NiceFeelsApp";
    public final int MINIMUM_DISTANCE = 1000;
    private final int DEFAULT_ZOOM = 14;
    private final int SECONDS = 1;
    private final long MIN_TIME = 1000 * SECONDS;
    static final String STATE_USER_LOCATION = "mLastLocation";
    static final String STATE_TARGET_LOCATION_LATITUDE = "mTargetLocation_Latitude";
    static final String STATE_TARGET_LOCATION_LONGITUDE = "mTargetLocation_Longitude";
    static final String STATE_MARKER = "marker";
    static final String STATE_DISTANCE_VIEW = "distanceView";
    static final String STATE_IS_SET = "isSet";
    /**
     * Instance variables
     */
    //Managers
    private AlarmManager manager;
    private LocationManager locationMan;
    private Criteria criteria;

    //Google Things
    public static GoogleMap mMap;
    public static boolean marker, alarmSet;
    private GoogleApiClient mGoogleApiClient;

    //Locations
    public static Location mLastLocation,targetLocation;
    public static LatLng userAlarmLocation,mLastLocation_LtLn;
    private static float distanceBetween;
    private boolean isGPSEnabled,isNetworkEnabled,canGetLocation;

    //Misc
    private final Context context = this;
    public static Button locationButton, resetButton, searchButton;
    public static ImageView locationImage, resetImage, searchImage;
    private boolean markerClicked;
    private String provider, m_Text = "";
    public static MediaPlayer mp;
    public static PendingIntent pendingIntent;
    private Calendar cal;
    public static TextView distanceView;
    private BroadcastReceiver uiUpdated ;
    private LocationRequest mLocationRequest;
    private SharedPreferences settings;




    /***
     * Creates the app in order to be used.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildGoogleApiClient();
        setContentView(R.layout.activity_main);

        locationMan = (LocationManager)getSystemService(LOCATION_SERVICE);
        criteria = new Criteria();
        provider = locationMan.getBestProvider(criteria, true);
        checkLocationIsEnabled();
        distanceView = new TextView(this);
        distanceView = (TextView) findViewById(R.id.distanceView);
        distanceView.setText("Distance: 0m");

        uiUpdated = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                distanceView.setText(intent.getExtras().getString("LOCATION_DATA"));
            }
        };
        registerReceiver(uiUpdated, new IntentFilter("LOCATION_UPDATED"));

        mp = MediaPlayer.create(this, R.raw.chime);

        cal = Calendar.getInstance();
        setUpMap();
        addListenerOnImage();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        alarmSet = false;
    }

    private void checkLocationIsEnabled(){
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = locationMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = locationMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}
        if(!gps_enabled && !network_enabled){
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setMessage("Please turn on Location services");
            dialog.setPositiveButton("Open Location services", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(myIntent);
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                }
            });
            dialog.show();

        }
    }



    /***
     * A method that adds the various buttons to the activity. If the reset button is pressed,
     * then the alarm is cancelled, or when the set Location is pressed, then the alarm is set.
     *
     * If the user doesn't have their location services activated, then a small message is displayed
     * and nothing else can be done.
     */
    public void addListenerOnImage() {
        //Images
        locationImage = (ImageView) findViewById(R.id.yesImage);
        resetImage = (ImageView) findViewById(R.id.cancelIamge);
        searchImage = (ImageView) findViewById(R.id.searchImage);

        pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(getActivity(), WakefulReceiver.class), 0);

        locationImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (targetLocation == null || userAlarmLocation == null) {
                    Toast.makeText(context, "Please choose a location first", Toast.LENGTH_SHORT).show();
                } else {
                    if (mLastLocation == null) {
                        if (mMap.getMyLocation() == null) {
                            Toast.makeText(context, "Unable to get location. Please Try again.", Toast.LENGTH_SHORT).show();
                        } else {
                            getLastLocation();
                            distanceBetween = mLastLocation.distanceTo(targetLocation);
                            if (distanceBetween < MINIMUM_DISTANCE) tooClose();
                            else start();
                        }
                    } else {
                        if (targetLocation != null) {
                            distanceBetween = mLastLocation.distanceTo(targetLocation);
                            if (distanceBetween < MINIMUM_DISTANCE) tooClose();
                            else start();

                        } else {
                            Toast.makeText(context, "Oops, something happened. Please try again :) ERROR 3", Toast.LENGTH_SHORT).show();
                            mLastLocation = locationMan.getLastKnownLocation(provider);
                        }


                    }
                }
            }
        });

        resetImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                marker = false;
                userAlarmLocation = null;
                targetLocation = null;
                cancel();
            }
        });

        searchImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastLocation != null) {
                    searchFunction();
                } else {
                    Toast.makeText(context, "Oops, something happened. Please try again :), Error 2", Toast.LENGTH_SHORT).show();
                    mLastLocation = locationMan.getLastKnownLocation(provider);
                    searchFunction();
                }
            }


        });
    }
    /***
     * A method that adds the various buttons to the activity. If the reset button is pressed,
     * then the alarm is cancelled, or when the set Location is pressed, then the alarm is set.
     *
     * If the user doesn't have their location services activated, then a small message is displayed
     * and nothing else can be done.
     */
    public void addListenerOnButton() {

//        locationButton = (Button) findViewById(R.id.locationButton);
//        resetButton = (Button) findViewById(R.id.resetButton);
//        searchButton = (Button) findViewById(R.id.searchButton);
        pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(getActivity(), WakefulReceiver.class), 0);
        locationButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (targetLocation == null || userAlarmLocation == null) {
                    Toast.makeText(context, "Please choose a location first", Toast.LENGTH_SHORT).show();
                } else {
                    if (mLastLocation == null) {
                        if (mMap.getMyLocation() == null) {
                            Toast.makeText(context, "Unable to get location. Please Try again.", Toast.LENGTH_SHORT).show();
                        } else {
                            getLastLocation();
                            distanceBetween = mLastLocation.distanceTo(targetLocation);
                            if (distanceBetween < MINIMUM_DISTANCE) tooClose();
                            else start();
                        }
                    } else {
                        if (targetLocation != null) {
                            distanceBetween = mLastLocation.distanceTo(targetLocation);
                            if (distanceBetween < MINIMUM_DISTANCE) tooClose();
                            else start();

                        } else {
                            Toast.makeText(context, "Oops, something happened. Please try again :) ERROR 3", Toast.LENGTH_SHORT).show();
                            mLastLocation = locationMan.getLastKnownLocation(provider);
                        }


                    }
                }
            }

        });

        resetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                marker = false;
                userAlarmLocation = null;
                targetLocation = null;
                cancel();
            }
        });

        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastLocation != null) {
                    searchFunction();
                } else {
                    Toast.makeText(context, "Oops, something happened. Please try again :), Error 2", Toast.LENGTH_SHORT).show();
                    mLastLocation = locationMan.getLastKnownLocation(provider);
                    searchFunction();
                }
            }


        });

    }

    public void searchFunction(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // Set up the input
        final EditText input = new EditText(context);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Search!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = input.getText().toString();
                userAlarmLocation = getLocationFromAddress(m_Text);
                if (userAlarmLocation != null) {
                    mMap.addMarker(new MarkerOptions()
                            .position(userAlarmLocation)
                            .draggable(false));
                    setTargetLocation();
                    markerClicked = false;
                    marker = true;
                    LatLngBounds pos;
                    // Animates camera to include both points hopefully :P
                    if (userAlarmLocation.latitude < mLastLocation.getLatitude())
                        pos = new LatLngBounds(userAlarmLocation, mLastLocation_LtLn);
                    else pos = new LatLngBounds(mLastLocation_LtLn, userAlarmLocation);
                    LatLng temp = midPoint(userAlarmLocation.latitude, userAlarmLocation.longitude,
                            mLastLocation_LtLn.latitude, mLastLocation_LtLn.longitude);
                    float dist = mLastLocation.distanceTo(targetLocation) / 1000;
                    int zoom = 15;

                    if (dist < 2) zoom = 10;
                    else if (dist < 5) zoom = 13;
                    else if (dist < 20) zoom = 11;
                    else if (dist < 50) zoom = 10;
                    else if (dist < 100) zoom = 9;
                    else if (dist > 200) zoom = 8;
                    else if (dist > 10000) zoom = 3;

                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(temp, zoom);
                    mMap.animateCamera(cameraUpdate);
                } else {
                    Toast.makeText(context, "Unable to find " + m_Text, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                m_Text = "";
            }
        });

        builder.show();
    }

    public void setTargetLocation(){
        targetLocation = new Location("");//provider name is unecessary
        targetLocation.setLatitude(userAlarmLocation.latitude);//your coords of course
        targetLocation.setLongitude(userAlarmLocation.longitude);
    }

    /***
     * Found from Stackoverflow:
     * http://stackoverflow.com/questions/3574644/how-can-i-find-the-latitude-and-longitude-from-address
     *
     * @param strAddress
     * @return
     */
    public LatLng getLocationFromAddress(String strAddress) {

        Geocoder coder = new Geocoder(this);
        List<Address> address;
        LatLng p1 = null;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            p1 = new LatLng(location.getLatitude(), location.getLongitude() );

        } catch (Exception ex) {

            ex.printStackTrace();
        }

        return p1;
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
     * Returns the current activity
     * @return
     */
    public Activity getActivity(){
        return MainActivity.this;
    }



    /***
     * A method that cancells the alarm that was set and displays a message saying
     * this to the user.
     */
    public void cancel() {
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
        distanceView.setText("Distance: 0m");
        alarmSet = false;
        Toast.makeText(this, "Alarm Cancelled", Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a message saying the alarm was started, also starts the alarm
     */
    public void start() {
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.SECOND, 1);
        alarmSet = true;
        manager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), MIN_TIME, pendingIntent);
        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
        mLastLocation_LtLn = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());


    }

    /***
     * Gets the user's last known position.
     */
    private void getLastLocation(){
        mLastLocation = locationMan.getLastKnownLocation(provider);
        if(mLastLocation != null) {
            mLastLocation_LtLn = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            CameraUpdate userLocation = CameraUpdateFactory.newLatLngZoom(mLastLocation_LtLn, DEFAULT_ZOOM);
            mMap.animateCamera(userLocation);
        }
        else if (mMap.getMyLocation() != null) {
            mLastLocation = mMap.getMyLocation();
            mLastLocation_LtLn = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            CameraUpdate userLocation = CameraUpdateFactory.newLatLngZoom(mLastLocation_LtLn, DEFAULT_ZOOM);
            mMap.animateCamera(userLocation);
        }
        else Toast.makeText(context, "Unable to get location, ERROR 1.0", Toast.LENGTH_LONG).show();
    }


    /***
     * Gets the user's last known position.
     */
    private void getLastLocationForOnChange(){
        mLastLocation = locationMan.getLastKnownLocation(provider);
        if(mLastLocation != null) {
            mLastLocation_LtLn = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        }
        else if (mMap.getMyLocation() != null) {
            mLastLocation = mMap.getMyLocation();
            mLastLocation_LtLn = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        }
    }


    SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    double getDoubleSettings(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }


    @Override
    public void onDestroy(){
        unregisterReceiver(uiUpdated);
        if( (manager != null)&&(pendingIntent != null) ){
            manager.cancel(pendingIntent);
            alarmSet = false;
            settings = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(STATE_IS_SET, false);
        }
        super.onDestroy();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG,"**************************************************************");
        if (mGoogleApiClient.isConnected()) {
           mGoogleApiClient.disconnect();
        }
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        Log.i(TAG, "PAUSED IN STOP");
        if((mLastLocation_LtLn!=null)&&(targetLocation!=null)){
            editor.putBoolean("firstRun_NiceAlarms", false);
            editor.putString(STATE_USER_LOCATION, "" + mLastLocation_LtLn.latitude + "," + mLastLocation_LtLn.longitude);
            editor.putBoolean(STATE_IS_SET, alarmSet);
            editor.putBoolean(STATE_MARKER, marker);
            putDouble(editor, STATE_TARGET_LOCATION_LATITUDE, targetLocation.getLatitude());
            putDouble(editor, STATE_TARGET_LOCATION_LONGITUDE, targetLocation.getLongitude());
        }
        Log.i(TAG, "IS IT THERE?: " + settings.contains(STATE_TARGET_LOCATION_LATITUDE));
        Log.i(TAG, "**************************************************************");
        Log.i(TAG, "IS IT THERE?: " + settings.contains(STATE_TARGET_LOCATION_LONGITUDE));
        Log.i(TAG, "**************************************************************");
        editor.commit();
        Log.i(TAG, "IS IT THERE?: " + settings.contains(STATE_TARGET_LOCATION_LATITUDE));
        Log.i(TAG, "**************************************************************");
        Log.i(TAG, "IS IT THERE?: " + settings.contains(STATE_TARGET_LOCATION_LONGITUDE));
        Log.i(TAG, "**************************************************************");
    }



    @Override
    protected void onStop(){
        super.onStop();
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(settings.contains(STATE_TARGET_LOCATION_LATITUDE)&&
                settings.contains(STATE_TARGET_LOCATION_LONGITUDE)){
            Log.i(TAG, "**************************************************************");
            targetLocation = new Location("target");
            targetLocation.setLatitude(getDoubleSettings(settings, STATE_TARGET_LOCATION_LATITUDE, 0.0));
            targetLocation.setLongitude(getDoubleSettings(settings, STATE_TARGET_LOCATION_LONGITUDE, 0.0));
            Log.i(TAG, "**************************************************************");
            alarmSet = settings.getBoolean(STATE_IS_SET,false);
            marker = settings.getBoolean(STATE_MARKER,false);
            if(alarmSet && mMap!=null && marker){
                distanceView.setText(settings.getString(STATE_DISTANCE_VIEW, ""));
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(targetLocation.getLatitude(), targetLocation.getLongitude()))
                        .draggable(false));
            }
            else{
                distanceView.setText("Distance: 0m");
            }
            Log.i(TAG, "**************************************************************");
        }else{
            Log.i(TAG, "**************************************************************");
            distanceView.setText("Distance: 0m");
            Log.i(TAG, "ITS NOT THERE,ONRESTART");
            Log.i(TAG, "**************************************************************");
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(settings.contains(STATE_TARGET_LOCATION_LATITUDE)&&
                settings.contains(STATE_TARGET_LOCATION_LONGITUDE)){
            Log.i(TAG, "**************************************************************");
            targetLocation = new Location("target");
            targetLocation.setLatitude(getDoubleSettings(settings, STATE_TARGET_LOCATION_LATITUDE, 0.0));
            targetLocation.setLongitude(getDoubleSettings(settings, STATE_TARGET_LOCATION_LONGITUDE, 0.0));
            Log.i(TAG, "**************************************************************");
            setUpMap();
            alarmSet = settings.getBoolean(STATE_IS_SET,false);
            marker = settings.getBoolean(STATE_MARKER,false);
            if(alarmSet){
                distanceView.setText(settings.getString(STATE_DISTANCE_VIEW, ""));
                if(mMap != null && marker){
                    userAlarmLocation = new LatLng(targetLocation.getLatitude(), targetLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions()
                            .position(userAlarmLocation)
                            .draggable(false));
                }
            }
            else{
                distanceView.setText("Distance: 0m");
            }
            Log.i(TAG, "**************************************************************");
        }else{
            Log.i(TAG, "**************************************************************");
            distanceView.setText("Distance: 0m");
            Log.i(TAG, "ITS NOT THERE,ONRESTART");
            Log.i(TAG, "**************************************************************");
        }
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(settings.contains(STATE_TARGET_LOCATION_LATITUDE)&&
                settings.contains(STATE_TARGET_LOCATION_LONGITUDE)){
            Log.i(TAG, "**************************************************************");
            targetLocation = new Location("target");
            targetLocation.setLatitude(getDoubleSettings(settings, STATE_TARGET_LOCATION_LATITUDE, 0.0));
            targetLocation.setLongitude(getDoubleSettings(settings, STATE_TARGET_LOCATION_LONGITUDE, 0.0));
            Log.i(TAG, "**************************************************************");
            setUpMap();
            alarmSet = settings.getBoolean(STATE_IS_SET,false);
            marker = settings.getBoolean(STATE_MARKER,false);
            if(alarmSet){
                distanceView.setText(settings.getString(STATE_DISTANCE_VIEW, ""));
                if(mMap != null && marker){
                    userAlarmLocation = new LatLng(targetLocation.getLatitude(), targetLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions()
                            .position(userAlarmLocation)
                            .draggable(false));
                }
            }
            else{
                distanceView.setText("Distance: 0m");
            }
            Log.i(TAG, "**************************************************************");
        }else{
            Log.i(TAG, "**************************************************************");
            distanceView.setText("Distance: 0m");
            Log.i(TAG, "ITS NOT THERE,ONRESTART");
            Log.i(TAG, "**************************************************************");
        }
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

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            public void onMapLoaded() {
                if(!marker)
                    getLastLocation();
            }
        });

        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                getLastLocationForOnChange();
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
           // Log.i(TAG,"Building!");
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
     *
     *  Found on here
     * http://stackoverflow.com/questions/4656802/midpoint-between-two-latitude-and-longitude
     */
    public LatLng midPoint(double lat1,double lon1,double lat2,double lon2){

        double dLon = Math.toRadians(lon2 - lon1);
        double result[] = new double[2];

        //convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        lon1 = Math.toRadians(lon1);

        double Bx = Math.cos(lat2) * Math.cos(dLon);
        double By = Math.cos(lat2) * Math.sin(dLon);
        result[0] = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
        result[1] = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);

        //print out in degrees
        Log.i(TAG,Math.toDegrees( result[0]) + " " + Math.toDegrees(result[1]));
        return new LatLng(Math.toDegrees( result[0]),Math.toDegrees(result[1]));
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
                    .draggable(false));
            markerClicked = false;
            marker = true;
            CustomDialog dialog = new CustomDialog(this,"Are you sure?");
            dialog.show();
            userAlarmLocation = point;
            setTargetLocation();
        }


    }

    /***
     * A method that is run when the app starts
     * purely for debugging atm.
     */
    @Override
    protected void onStart() {
        super.onStart();
        //Log.i(TAG, "Connected: " + String.valueOf(mGoogleApiClient.isConnected()));
    }



    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
       // Log.i(TAG, String.valueOf(mGoogleApiClient.isConnected()));
    }
    /***
     * A method that runs when the API is connected to.
     * @param connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Log.i(TAG,"onConnected mLocation inside Main: "+mLastLocation);
        }
    }

    /***
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onLocationChanged(Location location) {
        if((location != null)&&(location != mLastLocation)){
            Log.i(TAG,"Location from Main: "+location.getLatitude()+","+location.getLongitude());
            mLastLocation = location;
            if(alarmSet == true) {
                mLastLocation_LtLn = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                MainActivity.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLastLocation_LtLn, DEFAULT_ZOOM));
            }
        }
    }
}
