package com.nicefeels.nicealarms;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
/**
 * Created by Jinxy on 19/09/2015.
 */
public class WakefulReceiver extends WakefulBroadcastReceiver {
    public final static String TAG = "NiceFeelsApp";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Starting service @ " + SystemClock.elapsedRealtime());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, new Intent(context, AlarmIntentService.class));
    }
}