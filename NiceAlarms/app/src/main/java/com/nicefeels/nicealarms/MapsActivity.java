package com.nicefeels.nicealarms;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.*;

import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapsActivity extends Activity implements OnMapClickListener,OnMapLongClickListener, OnMarkerDragListener {

    /**
     * Constants
     */
    private final LatLng DEFAULT_LOCATION = new LatLng(53.3096163, -6.3123088);
    private final int DEFAULT_ZOOM = 10;
    /**
     * Local variables *
     */
    public static GoogleMap mMap;
    private boolean markerClicked;
    public static boolean marker;
    private final Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        FragmentManager myFragmentManager = getFragmentManager();
        MapFragment myMapFragment
                = (MapFragment) myFragmentManager.findFragmentById(R.id.mapView);
        mMap = myMapFragment.getMap();
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);


        mMap.setMyLocationEnabled(true);

        createMapView();

        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerDragListener(this);
        markerClicked = false;
        marker = false;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM) );
    }

    /**
     * Creates the view for the map
     */
    private void createMapView() {
        try {
            if (null == mMap) {
                mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapView)).getMap();
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                if (null == mMap) {
                    Toast.makeText(getApplicationContext(),
                            "Error creating map", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (NullPointerException exception) {
            Log.e("mapApp", exception.toString());
        }
    }

    /**
     * Handles A long press on the screen
     * @param point
     */
    @Override
    public void onMapLongClick(LatLng point) {
        if (!marker) {
            mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .draggable(true));
            markerClicked = false;
            marker = true;

            CustomDialog dialog = new CustomDialog(this);
            dialog.show();
        }

    }
    /***
     * Handles a single press on the screen.
     * @param point
     */
    @Override
    public void onMapClick(LatLng point) {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(point));
        markerClicked = false;
    }

    @Override
    public void onMarkerDrag(Marker marker) {
//        tvLocInfo.setText("Marker " + marker.getId() + " Drag@" + marker.getPosition());
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
//        tvLocInfo.setText("Marker " + marker.getId() + " DragEnd");
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
//        tvLocInfo.setText("Marker " + marker.getId() + " DragStart");

    }
}
