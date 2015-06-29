package com.nicefeels.nicealarms;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;

import java.util.Map;


public class CustomDialog_1 extends Dialog implements android.view.View.OnClickListener {
    public Activity c;
    public Button ok;
    private String toSay;

    public CustomDialog_1(Activity a, String text) {
        super(a);
        this.c = a;
        this.toSay = text;
    }

    /***
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_1);
        TextView textView = (TextView) findViewById(R.id.txt_dia);
        textView.setText(toSay);
        ok = (Button) findViewById(R.id.btn_yes);
        ok.setOnClickListener(this);
    }

    /***
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ok:
                dismiss();
                break;
            default:
                break;
        }
    }
}