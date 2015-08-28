package com.nicefeels.nicealarms;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;


public class CustomDialog extends Dialog implements android.view.View.OnClickListener {
    public Activity c;
    public Button yes, no;
    private String toSay;

    public CustomDialog(Activity a, String text) {
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
        setContentView(R.layout.custom);
        TextView textView = (TextView) findViewById(R.id.txt_dia);
        textView.setText(toSay);
        yes = (Button) findViewById(R.id.btn_yes);
        no = (Button) findViewById(R.id.btn_no);
        yes.setOnClickListener(this);
        no.setOnClickListener(this);
    }

    /***
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_yes:
                dismiss();
                //c.onBackPressed();
                break;
            case R.id.btn_no:
                MainActivity.marker = false;
                MainActivity.mMap.clear();
                dismiss();
                break;
            default:
                break;
        }
    }
}