package com.nicefeels.nicealarms;

import android.app.AlarmManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

import android.os.Vibrator;

import java.security.Provider;

/**
 * Created by Jigsaw on 21/08/2015.
 */
public class Alarm extends BroadcastReceiver {
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
    @Override
    public void onReceive(Context context, Intent intent) {
        locationMan = (LocationManager)context.getSystemService(context.LOCATION_SERVICE);
        criteria = new Criteria();
        provider = locationMan.getBestProvider(criteria, true);
        mLastLocation = locationMan.getLastKnownLocation(provider);
        v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if(mLastLocation != null){
            Log.i(TAG,"Inside Alarm: "+mLastLocation.getLatitude()+","+mLastLocation.getLongitude());
            alarmMethod(context);
        }
        else if(MainActivity.mMap.getMyLocation() != null){
            mLastLocation = MainActivity.mMap.getMyLocation();
            alarmMethod(context);
        }
        else{
            Toast.makeText(context, "Unable to get location", Toast.LENGTH_LONG).show();
        }

    }

    private void alarmMethod(Context context){

        if(MainActivity.targetLocation == null){
            Log.i(TAG, "User hasnt picked a location");
        }
        else{
            Log.i(TAG,"Inside Alarm Method mLocation: "+mLastLocation.getLatitude()+","+mLastLocation.getLongitude());
            Log.i(TAG,"Inside Alarm Method targetLocation: "+MainActivity.targetLocation.getLatitude()+","+MainActivity.targetLocation.getLongitude());
            newDist = mLastLocation.distanceTo(MainActivity.targetLocation);
            Log.i(TAG, "Distance: " + newDist );//+ MainActivity.distanceBetween[0]);

            // if the distance is less than MINIMUM ring the alarm,
            // otherwise display the distance
            if (newDist > MINIMUM_DISTANCE) {
                LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                MainActivity.mMap.animateCamera(cameraUpdate);
                Log.i(TAG, "Distance: " + newDist);
                Toast.makeText(context, "Distance: " + newDist, Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(context, "You're There!", Toast.LENGTH_LONG).show();
                MainActivity.mp.start();
                v.vibrate(pattern, -1); //-1 is important
                manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                manager.cancel(MainActivity.pendingIntent);
            }
        }

    }
}
