package com.nicefeels.nicealarms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.gcm.Task;

public class Splash extends Activity {

    private final int SPLASH_DISPLAY_LENGTH = 3000;            //set your time here......
    private final String TAG = "NiceFeelsApp";
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        context = this;
        new Handler().postDelayed(myTask, SPLASH_DISPLAY_LENGTH);
             /* Create an Intent that will start the Menu-Activity. */






    }


    @Override
    protected void onPause(){
        super.onPause();
        SharedPreferences settings = getSharedPreferences("Nice Alarms", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.putBoolean("firstRun_NiceAlarms", false);
        editor.commit();
    }

    private Runnable myTask = new Runnable() {
        @Override
        public void run() {
            prefs = context.getSharedPreferences("Nice Alarms",Context.MODE_PRIVATE);
            editor = prefs.edit();

//            editor.remove("Nice Alarms");
//            editor.remove("firstRun");
//            editor.commit();

//            boolean firstRun = prefs.getBoolean("firstRun", false);
            boolean firstRun = prefs.contains("firstRun_NiceAlarms");

            Intent intent = new Intent(Splash.this, HowTo.class);
            Intent mainIntent = new Intent(Splash.this, MainActivity.class);
            Log.i(TAG,"Boolean: "+firstRun);
            Log.i(TAG,"Boolean key nice alarms: "+prefs.contains("Nice Alarms"));
            Log.i(TAG,"Boolean firstRun: "+prefs.contains("firstRun_NiceAlarms"));
            if (firstRun) {
                editor.clear();
                editor.putBoolean("firstRun_NiceAlarms", false);
                editor.commit();
                Log.i(TAG, "About to launch MainActivity from Splash");
                Splash.this.startActivity(mainIntent);
                Splash.this.finish();
            } else {
                editor.clear();
                editor.putBoolean("firstRun_NiceAlarms", false);
                editor.commit();
                Log.i(TAG, "Starting How To");
                startActivity(intent);
                finish();
            }
        }
    };
}
