package com.dodrop.fikisha;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.dodrop.fikisha.Common.Common;
import com.dodrop.fikisha.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Calendar;

public class TripDetailActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private TextView txtDate, txtFee, txtBaseFare, txtTime, txtDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        txtBaseFare = (TextView)findViewById(R.id.txtBaseFare);
        txtDate = (TextView)findViewById(R.id.txtDate);
        txtFee = (TextView)findViewById(R.id.txtFee);
        txtTime = (TextView)findViewById(R.id.txtTime);
        txtDistance = (TextView)findViewById(R.id.txtDistance);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        settingInformation();
    }

    private void settingInformation() {
        if(getIntent() != null)
        {

            Calendar calendar = Calendar.getInstance();
            String date = String.format("%s, %d/%d", convertToDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))
                    ,calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH));

            txtFee.setText(String.format("$ %.2f", getIntent().getDoubleExtra("total", 0.0)));
            txtBaseFare.setText(String.format("$ %.2f", Common.base_fare));
            txtTime.setText(String.format("%s min", getIntent().getStringExtra("time")));
            txtDistance.setText(String.format("%s Km", getIntent().getStringExtra("distance")));

            String[] location_end = getIntent().getStringExtra("location_end").split(",");
            LatLng dropOff = new LatLng(Double.parseDouble(location_end[0]), Double.parseDouble(location_end[1]));

            mMap.addMarker(new MarkerOptions()
                .position(dropOff)
                .title("Drop Off Here")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_marker)));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dropOff, 12.0f));
        }
    }

    private String  convertToDayOfWeek(int day) {
        switch (day)
        {
            case Calendar.SUNDAY:
                return "SUNDAY";
            case Calendar.MONDAY:
                return "MONDAY";
            case Calendar.TUESDAY:
                return "TUESDAY";
            case Calendar.WEDNESDAY:
                return "WEDNESDAY";
            case Calendar.THURSDAY:
                return "THURSDAY";
            case Calendar.FRIDAY:
                return "FRIDAY";
            case Calendar.SATURDAY:
                return "SATURDAY";

                default:
                    return "UNK";
        }
    }
}
