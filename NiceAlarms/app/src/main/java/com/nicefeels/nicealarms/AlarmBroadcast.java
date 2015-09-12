package com.nicefeels.nicealarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by simon on 12/09/2015.
 */
public class AlarmBroadcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent background = new Intent(context, AlarmService.class);
        context.startService(background);
    }

}