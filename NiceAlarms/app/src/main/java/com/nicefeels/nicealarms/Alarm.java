package com.nicefeels.nicealarms;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.location.LocationServices;
import android.os.Vibrator;
/**
 * Created by Jigsaw on 21/08/2015.
 */
public class Alarm extends BroadcastReceiver {
    public final static String TAG = "NiceFeelsApp";
    public final static int MINIMUM_DISTANCE = 1000;

    private float[] newDist = new float[4];
    @Override
    public void onReceive(Context context, Intent intent) {

        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        MainActivity.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(MainActivity.mGoogleApiClient);
        Location.distanceBetween(MainActivity.mLastLocation.getLatitude(),MainActivity.mLastLocation.getLongitude(),
                                 MainActivity.userAlarmLocation.latitude, MainActivity.userAlarmLocation.longitude, newDist);
        Log.i(TAG, "Distance: " + MainActivity.distanceBetween[0]);
        if (newDist[0] > MINIMUM_DISTANCE) {
            Log.i(TAG, "Distance: " + newDist[0]);
            Toast.makeText(context, "Distance: " + newDist[0], Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(context, "You're There!", Toast.LENGTH_LONG).show();
            MainActivity.mp.start();

            // Vibrate in this pattern
            // sleep, vibrate for 100ms, sleep for half a second, vibrate for 300ms
            long[] pattern = {0, 100, 500, 300};
            v.vibrate(pattern, -1); //-1 is important
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            manager.cancel(MainActivity.pendingIntent);
        }
    }

}
