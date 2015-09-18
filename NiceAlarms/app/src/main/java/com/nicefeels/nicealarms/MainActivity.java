package com.nicefeels.nicealarms;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;

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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity implements
        GoogleMap.OnMapLongClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    /**
     * Constants
     */
    public final String TAG = "NiceFeelsApp";
    public final int MINIMUM_DISTANCE = 1000;
    private final int DEFAULT_ZOOM = 14;
    private final int SECONDS = 1;
    private final long MIN_TIME = 100 * SECONDS;
    /**
     * Instance variables
     */
    //Managers
    private AlarmManager manager;
    private LocationManager locationMan;
    private Criteria criteria;

    //Google Things
    public static GoogleMap mMap;
    public static boolean marker;
    private GoogleApiClient mGoogleApiClient;

    //Locations
    public static Location mLastLocation,targetLocation;
    public static LatLng userAlarmLocation,mLastLocation_LtLn;
    private static float distanceBetween;

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
        checkLocationIsEnabled();
        distanceView = new TextView(this);
        distanceView = (TextView) findViewById(R.id.distanceView);
        distanceView.setText("Distance: 0m");

        criteria = new Criteria();
        provider = locationMan.getBestProvider(criteria, true);
        mp = MediaPlayer.create(this, R.raw.chime);

        cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 10);
        setUpMap();
        addListenerOnImage();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                    // TODO Auto-generated method stub
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(myIntent);
                    //get gps
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

        pendingIntent = PendingIntent.getService(getActivity(), 0, new Intent(getActivity(), AlarmService.class), 0);

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
        pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(getActivity(), AlarmBroadcast.class), 0);
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
//                setTargetLocation();
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
        Toast.makeText(this, "Alarm Cancelled", Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a message saying the alarm was started, also starts the alarm
     */
    public void start() {
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), MIN_TIME, pendingIntent);
        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
        mLastLocation_LtLn = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());


    }

    @Override
    protected void onPause(){
        super.onPause();
        SharedPreferences settings = getSharedPreferences("Nice Alarms",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.putBoolean("firstRun_NiceAlarms", false);
        editor.commit();
    }

    public static void animateMap(CameraUpdate cm){
       mMap.animateCamera(cm);
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
        }
        setTargetLocation();

    }

    /***
     * A method that is run when the app starts
     * purely for debugging atm.
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Connected: " + String.valueOf(mGoogleApiClient.isConnected()));
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
