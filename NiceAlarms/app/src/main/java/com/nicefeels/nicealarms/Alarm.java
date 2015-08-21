package com.nicefeels.nicealarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Jigsaw on 21/08/2015.
 */
public class Alarm extends BroadcastReceiver {
    public final static String TAG = "NiceFeelsApp";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "HEY in Alarm!");

//        Location.distanceBetween(MainActivity.mLastLocation.getLatitude(), MainActivity.mLastLocation.getLongitude(),
//                MainActivity.userAlarmLocation.latitude, MainActivity.userAlarmLocation.longitude, distanceBetween);
        Log.i(TAG, "Distance: " + MainActivity.distanceBetween[0]);
        if (MainActivity.distanceBetween[0] > 1000) {
            Log.i(TAG, "Distance: " + MainActivity.distanceBetween[0]);
            Toast.makeText(context, "Distance: " + MainActivity.distanceBetween[0], Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(context, "You're There!", Toast.LENGTH_LONG).show();
            MainActivity.mp.start();
            Log.i(TAG, "Wake Up!" + MainActivity.distanceBetween[0]);
        }
    }

}
