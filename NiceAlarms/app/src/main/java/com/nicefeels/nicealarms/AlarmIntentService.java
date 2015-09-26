package com.nicefeels.nicealarms;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Jinxy on 13/09/2015.
 */
public class AlarmIntentService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener{
    private final String TAG = "NiceFeelsApp";
    private final int DEFAULT_ZOOM = 14;
    private final int MINIMUM_DISTANCE = 1000;
    static final String STATE_IS_SET = "isSet";
    private Vibrator v;
    // Vibrate in this pattern
    // sleep, vibrate for 100ms, sleep for half a second, vibrate for 300ms
    private long[] pattern = { 50, 100, 50, 100 };
    private AlarmManager manager;
    private Location mLastLocation,targetLocation;
    private LocationManager locationMan;
    private String provider;
    private Criteria criteria;
    private float newDist;

    private final Context context = this;

    private Handler mHandler;
    private Uri soundUri;
    private NotificationManager notificationManager;
    private TextView distanceView;
    private Intent mWakefulIntent;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public AlarmIntentService() {
        super("AlarmIntentService");
    }
    /***
     *
     */
    public AlarmIntentService(String name) {
        super(name);
    }

    /***
     *
     */
    @Override
    public void onCreate(){
        super.onCreate();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        this.locationMan = (LocationManager)context.getSystemService(context.LOCATION_SERVICE);
        this.criteria = new Criteria();
        this.distanceView = new TextView(getApplicationContext());
        this.mHandler = new Handler(getApplicationContext().getMainLooper());
        this.buildGoogleApiClient();
        // Gets data from the incoming Intent
        provider = locationMan.getBestProvider(criteria, true);
        v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    public void onDestroy(){
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(STATE_IS_SET, false);
        editor.commit();
        MainActivity.alarmSet = false;
        Log.i(TAG, "OH NO I HAVE BEEN KILLED");
        super.onDestroy();
    }



    /***
     *
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        mWakefulIntent = intent;
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        alarmMethod(context);
    }

    /***
     *
     */
    private void alarmMethod(Context context){
        if(MainActivity.targetLocation == null){
                Log.i(TAG, "User has not picked a location");
        }
        else if(mLastLocation != null){
            newDist = mLastLocation.distanceTo(MainActivity.targetLocation);
            // if the distance is less than MINIMUM ring the alarm,
            // otherwise display the distance
            if (newDist > MINIMUM_DISTANCE) {
                getDistanceAndDisplayNotification();
            } else {
                displayDestinationReachedNotification();
            }
        }
        else{
            Log.i(TAG,"This shouldn't be happening.");
        }
    }


    public void getDistanceAndDisplayNotification(){
        Log.i(TAG, "Distance: " + newDist + "m...");
        Intent i = new Intent("LOCATION_UPDATED");
        i.putExtra("LOCATION_DATA", "Distance: " + newDist + "m");
        sendBroadcast(i);
        this.notificationManager.notify(0, displayNotification(null, "Not there yet", "Distance to stop: " + newDist + "m",false).build());
    }

    public void displayDestinationReachedNotification(){
        this.mHandler.post(new ToastRunnable("You're There!"));
        this.manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.manager.cancel(MainActivity.pendingIntent);
        Notification notThere = displayNotification(soundUri, "Destination Reached", "You're there!",true).build();
        MainActivity.alarmSet = false;

        this.notificationManager.notify(0,notThere);
        this.v.vibrate(pattern, -1);
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        WakefulReceiver.completeWakefulIntent(mWakefulIntent);

        stopSelf();
    }

    private NotificationCompat.Builder displayNotification(Uri soundUri,String title, String messgae, boolean atLocation){

        NotificationCompat.Builder mBuilder =  new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.clock_light)
                .setContentTitle(title)
                .setContentText(messgae)
                .setSound(soundUri);

        if(!atLocation){
            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent intent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
            mBuilder.setContentIntent(intent);
        }

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
       return mBuilder;
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        if (mLastLocation != null) {
            Log.i(TAG,"onConnected mLocation inside Service: "+mLastLocation.getLatitude()+","+mLastLocation.getLongitude());
        }else{
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

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

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG,"Sup, location has changed");
        if((location != null)&&(location != mLastLocation)
                &&(MainActivity.mMap != null)){
                Log.i(TAG,"Location from Service: "+location.getLatitude()+","+location.getLongitude());
                mLastLocation = location;
                LatLng mLastLocation_LtLn = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                MainActivity.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLastLocation_LtLn, DEFAULT_ZOOM));
                alarmMethod(context);
        }
    }

    /**
     * Solution to use Toast inside a service:
     *  http://stackoverflow.com/questions/12730675/show-toast-at-current-activity-from-service
     *
     *  ALSO, interesting:
     *  http://cooking.stackexchange.com/questions/61628/is-it-safe-to-boil-water-in-a-microwave
     */
    private class ToastRunnable implements Runnable {
        String mText;

        public ToastRunnable(String text) {
            mText = text;
        }

        @Override
        public void run(){
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
        }
    }
}
