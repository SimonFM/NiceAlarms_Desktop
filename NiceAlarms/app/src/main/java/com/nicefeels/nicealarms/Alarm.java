package com.nicefeels.nicealarms;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

import android.os.Vibrator;
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
    private Location mLastLocation;
    LocationManager locationMan;


    private float[] newDist = new float[4];
    @Override
    public void onReceive(Context context, Intent intent) {
        LocationManager locationManager = (LocationManager)context.getSystemService(context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        mLastLocation = locationManager.getLastKnownLocation(provider);
        v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        Location.distanceBetween(mLastLocation.getLatitude(),mLastLocation.getLongitude(),
                                 MainActivity.userAlarmLocation.latitude, MainActivity.userAlarmLocation.longitude, newDist);
        Log.i(TAG, "Distance: " + MainActivity.distanceBetween[0]);
        if (newDist[0] > MINIMUM_DISTANCE) {
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
            MainActivity.mMap.animateCamera(cameraUpdate);
            Log.i(TAG, "Distance: " + newDist[0]);
            Toast.makeText(context, "Distance: " + newDist[0], Toast.LENGTH_SHORT).show();
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
