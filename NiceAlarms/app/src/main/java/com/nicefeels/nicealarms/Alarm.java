package com.nicefeels.nicealarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Jigsaw on 21/08/2015.
 */
public class Alarm extends BroadcastReceiver {
    public final static String TAG = "NiceFeelsApp";
    private float[] newDist = new float[4];
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "HEY in Alarm!");
        MainActivity.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(MainActivity.mGoogleApiClient);
        Location.distanceBetween(MainActivity.mLastLocation.getLatitude(),MainActivity.mLastLocation.getLongitude(),
                                 MainActivity.userAlarmLocation.latitude, MainActivity.userAlarmLocation.longitude, newDist);
        Log.i(TAG, "Distance: " + MainActivity.distanceBetween[0]);
        if (newDist[0] > 1000) {
            Log.i(TAG, "Distance: " + newDist[0]);
            Toast.makeText(context, "Distance: " + newDist[0], Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(context, "You're There!", Toast.LENGTH_LONG).show();
            MainActivity.mp.start();
            Log.i(TAG, "Wake Up!" + newDist[0]);
        }
    }

}
