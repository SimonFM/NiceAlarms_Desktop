package com.nicefeels.nicealarms;

import android.app.Activity;
import android.app.FragmentManager;
import android.location.Location;
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


import java.util.Calendar;

public class MainActivity extends Activity implements
        GoogleMap.OnMapLongClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /**
     * Constants
     */
    private final LatLng DEFAULT_LOCATION = new LatLng(53.3096163, -6.3123088);
    private final String TAG = "NiceFeelsApp";
    private final int DEFAULT_ZOOM = 10;
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
    private Activity a = this;
    private Location mLastLocation;
    private float distanceBetween[];

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
    }

    /***
     *
     */
    public void addListenerOnButton() {
        locationButton = (Button) findViewById(R.id.locationButton);
        resetButton = (Button) findViewById(R.id.resetButton);
        a = this;
        distanceBetween = new float[4];
        locationButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
               // Log.i(TAG, "User Location: "+ mLastLocation.getLatitude()+" "+mLastLocation.getLongitude());

                Log.i(TAG,"Requested Location: "+ userAlarmLocation.latitude+" "+userAlarmLocation.longitude);
//                Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(),
//                        userAlarmLocation.latitude,userAlarmLocation.longitude,distanceBetween);

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
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
    /***
     * TODO
     * @param connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null){};
    }

    /***
     * TODO
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {}


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }


}
