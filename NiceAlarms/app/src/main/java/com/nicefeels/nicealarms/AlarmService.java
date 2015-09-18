package com.nicefeels.nicealarms;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Jinxy on 12/09/2015.
 */
public class AlarmService extends Service {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public final static String TAG = "NiceFeelsApp";
    private Vibrator v;
    // Vibrate in this pattern
    // sleep, vibrate for 100ms, sleep for half a second, vibrate for 300ms
    private long[] pattern = {0, 100, 1000, 300, 500, 1500};
    public final static int MINIMUM_DISTANCE = 1000;
    private AlarmManager manager;
    private Location mLastLocation,targetLocation;
    private LocationManager locationMan;
    private String provider;
    private Criteria criteria;
    private float newDist;// = new float[4];

    private final Context context = this;
    private boolean isRunning;
    private Thread backgroundThread;
    private Handler mHandler;
    private Context contextAct;
    private  Uri soundUri;
    NotificationManager notificationManager;
    private TextView distanceView;


    public AlarmService(){}

    public AlarmService(Context cnxt){
        this.contextAct = cnxt;
    }

    @Override
    public void onCreate() {
        Log.i(TAG,"Hey Service onCreate()");
        int i = 0;
        this.isRunning = false;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        this.locationMan = (LocationManager)context.getSystemService(context.LOCATION_SERVICE);
        this.criteria = new Criteria();
        this.backgroundThread = new Thread(myTask);
        this.distanceView = new TextView(getApplicationContext());
    }
    private Runnable myTask = new Runnable() {
        public void run() {
            Log.i(TAG, "Testing Service was created");
            //Toast.makeText(context, "Testing Service was created", Toast.LENGTH_LONG).show();
            // Gets data from the incoming Intent
            provider = locationMan.getBestProvider(criteria, true);
            mLastLocation = MainActivity.mLastLocation;
            v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            if(mLastLocation != null){
                //Log.i(TAG,"Inside Alarm: "+mLastLocation.getLatitude()+","+mLastLocation.getLongitude());
                alarmMethod(context);
            }
            else if(MainActivity.mMap != null){
                mHandler.post(new FindLocation());
                alarmMethod(context);
            } else {
                //Toast.makeText(context, "Unable to get location, ERROR 10", Toast.LENGTH_LONG).show();
                mHandler.post(new ToastRunnable("Unable to get location, ERROR 10"));
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"Destroying Service");
        this.isRunning = false;
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Hey onStartCommand");

        if(!this.isRunning) {
            Log.i(TAG,"Hey onStartCommand, inside if");
            this.mHandler = new Handler();
            this.isRunning = true;
            this.backgroundThread.start();
        }
        else{
            this.alarmMethod(context);
        }
        return START_STICKY;
    }


    /***
     * This method handles the checking of the distance and displaying of messages.
     * @param context
     */
    private void alarmMethod(Context context){
        if (this.isRunning){
            mLastLocation = MainActivity.mLastLocation;
            Log.i(TAG, "Hey The alarm method was started");
            if(MainActivity.targetLocation == null){
                Log.i(TAG, "User has not picked a location");
            }
            else{
                newDist = mLastLocation.distanceTo(MainActivity.targetLocation);
                // if the distance is less than MINIMUM ring the alarm,
                // otherwise display the distance
                if (newDist > MINIMUM_DISTANCE) {
                    LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                    this.mHandler.post(new AnimateCamera(cameraUpdate));
                    Log.i(TAG, "Distance: " + newDist + "m...");//+ MainActivity.distanceBetween[0]);
                   // this.mHandler.post(new ToastRunnable("Distance: " + newDist + "m"));
                    this.mHandler.post(new updateLocation("Distance: " + newDist + "m"));
                    this.notificationManager.notify(0,displayNotification(null,"Not there yet","Distance to stop: "+ newDist + "m").build());
                } else {
                    this.mHandler.post(new ToastRunnable("You're There!"));
                    this.manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    this.manager.cancel(MainActivity.pendingIntent);

                    this.notificationManager.notify(0, displayNotification(soundUri, "Destination Reached", "You're there!").build());
                    this.v.vibrate(pattern, -1); //-1 is important
                    stopSelf();
                }
            }
        }
        else{
            Log.i(TAG, "ERROR 11");
        }
    }

    private NotificationCompat.Builder displayNotification(Uri soundUri,String title, String messgae){
        return new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.alarm)
                .setContentTitle(title)
                .setContentText(messgae)
                .setSound(soundUri); //This sets the sound to play
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
    private class AnimateCamera implements Runnable{

        CameraUpdate mUpdate;

        public AnimateCamera(CameraUpdate up) {
            mUpdate = up;
        }

        @Override
        public void run() {
            Context context = getApplicationContext();
            MainActivity.animateMap(mUpdate);
        }
    }
    private class FindLocation implements Runnable{
        public FindLocation() {}

        @Override
        public void run() {
            mLastLocation = MainActivity.mMap.getMyLocation();
        }
    }

    private class updateLocation implements Runnable{
        private String distance;
        public updateLocation(String d) {distance = d;}

        @Override
        public void run() {
            MainActivity.distanceView.setText(distance);
        }
    }


}
