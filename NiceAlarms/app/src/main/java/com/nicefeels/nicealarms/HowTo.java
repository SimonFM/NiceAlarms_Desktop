package com.nicefeels.nicealarms;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class HowTo extends Activity {

    private Button okayButton;
    private ImageView imageButton;
    private final String TAG = "NiceFeelsApp";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_how_to);
        addOkayButton();
    }
    /***
     * Adds the Okay Button functionality to the how to activity.
     */
    public void addOkayButton() {
        //okayButton = (Button) findViewById(R.id.okayButton);
        imageButton = (ImageView) findViewById(R.id.imageView2);

//        okayButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View arg0) {
//                Intent myIntent = new Intent(HowTo.this, MainActivity.class);
//                Log.i(TAG, "About to launch MainActivity from HowTo");
//                startActivity(myIntent);
//                finish();
//            }
//        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent myIntent = new Intent(HowTo.this, MainActivity.class);
                Log.i(TAG, "About to launch MainActivity from HowTo");
                startActivity(myIntent);
                finish();
            }
        });
    }
}
