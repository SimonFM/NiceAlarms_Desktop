package com.nicefeels.nicealarms;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

/**
 * Created by Jinxy on 26/08/2015.
 */
public class RingAlarm extends Service {
    public void onCreate() {
        Toast.makeText(getApplicationContext(), "hi there", Toast.LENGTH_LONG).show();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }



}
