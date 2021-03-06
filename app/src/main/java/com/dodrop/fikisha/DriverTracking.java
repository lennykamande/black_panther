package com.dodrop.fikisha;

import android.*;
import android.Manifest;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.dodrop.fikisha.Common.Common;
import com.dodrop.fikisha.Helper.DirectionJSONParser;
import com.dodrop.fikisha.Model.FCMResponse;
import com.dodrop.fikisha.Model.Notification;
import com.dodrop.fikisha.Model.Sender;
import com.dodrop.fikisha.Model.Token;
import com.dodrop.fikisha.Remote.IFCMService;
import com.dodrop.fikisha.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.transform.sax.SAXResult;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverTracking extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    String customerId;
    double riderLat, riderLng;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;

    private static final int PLAY_SERVICE_RES_REQUEST = 7001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    private static int UPDATE_INTERVAL = 5000;
    private static int FATEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    private Circle riderMarker;
    private Marker driverMarker;
    private Polyline direction;

    IGoogleAPI mService;

    IFCMService mFCMService;

    GeoFire geoFire;

    Button btnStartTrip;

    Location pickupLocation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_tracking);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        buildLocationRequest();
        createLocationCallBack();
        if (ActivityCompat.checkSelfPermission(DriverTracking.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(DriverTracking.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());
        displayLocation();

        if(getIntent()!= null)
        {
            riderLat = getIntent().getDoubleExtra("lat", -1.0);
            riderLng = getIntent().getDoubleExtra("lng", -1.0);
            customerId = getIntent().getStringExtra("customerId");
        }

        mService = Common.getGoogleAPI();

        mFCMService = Common.getFCMService();

        setUpLocation();

        btnStartTrip = (Button)findViewById(R.id.btnStartTrip);
        btnStartTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btnStartTrip.getText().equals("START TRIP"))
                {
                    pickupLocation = Common.mLastLocation;
                    btnStartTrip.setText("END TRIP");
                }
                else if(btnStartTrip.getText().equals("END TRIP"))
                {
                    calculateCashFee(pickupLocation, Common.mLastLocation);
                }
            }
        });
    }

    private void calculateCashFee(final Location pickupLocation, Location mLastLocation) {


        String requestApi = null;
        try{
            requestApi = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+pickupLocation.getLatitude()+","+pickupLocation.getLongitude()+"&"+
                    "destination="+mLastLocation.getLatitude()+","+mLastLocation.getLongitude()+"&"+
                    "key="+getResources().getString(R.string.google_direction_api);

            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try{

                                JSONObject jsonObject = new JSONObject(response.body().toString());
                                JSONArray routes = jsonObject.getJSONArray("routes");

                                JSONObject object = routes.getJSONObject(0);

                                JSONArray legs = object.getJSONArray("legs");

                                JSONObject legsObject = legs.getJSONObject(0);

                                JSONObject distance = legsObject.getJSONObject("distance");
                                String distance_text = distance.getString("text");

                                Double distance_value = Double.parseDouble(distance_text.replaceAll("[^0-9\\\\.]+",""));

                                JSONObject timeObject = legsObject.getJSONObject("duration");
                                String time_text = distance.getString("text");

                                Double time_value = Double.parseDouble(time_text.replaceAll("\\D+",""));

                                //Calculator Function

                                //Create new Activity
                                Intent intent = new Intent(DriverTracking.this, TripDetailActivity.class);
                                intent.putExtra("start_address", legsObject.getLong("start_address"));
                                intent.putExtra("end_address", legsObject.getLong("end_address"));
                                intent.putExtra("time", String.valueOf(time_value));
                                intent.putExtra("distance", String.valueOf(distance_value));
                                intent.putExtra("total", Common.getPrice(distance_value,time_value));
                                //intent.putExtra("location_Start",String.format("%f,%f",pickupLocation.getLatitude(),pickupLocation.getLongitude()));
                                intent.putExtra("location_end",String.format("%f,%f",Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude()));

                                startActivity(intent);
                                finish();


                            }catch (Exception e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(DriverTracking.this,""+t.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        riderMarker = mMap.addCircle(new CircleOptions()
                        .center(new LatLng(riderLat,riderLng))
                        .radius(50)
                        .strokeColor(Color.BLUE)
                        .fillColor(0x220000FF)
                        .strokeWidth(5.0f));

        geoFire = new GeoFire(FirebaseDatabase.getInstance().getReference(Common.driver_tbl));
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(riderLat,riderLng), 0.05f);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //Send Push Notification here
                sendArrivedNotification(customerId);
                btnStartTrip.setEnabled(true);
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

        if (ActivityCompat.checkSelfPermission(DriverTracking.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(DriverTracking.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        buildLocationRequest();
        createLocationCallBack();
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());

    }

    private void sendArrivedNotification(String customerId) {
        Token token = new Token(customerId);
        Notification notification = new Notification("Motorbike Arrival", String.format("Your Motorbike Driver %s Has Arrived", Common.currentUser.getName()));
        Sender sender = new Sender(token.getToken(), notification);

        mFCMService.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if(response.body().success!=1)
                {
                    Toast.makeText(DriverTracking.this, "Please try to request once more", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {

            }
        });
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED   )
        {
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        Common.mLastLocation = location;

                        if(Common.mLastLocation != null)
                        {

                                final double latitude = Common.mLastLocation.getLatitude();
                                final double longitude = Common.mLastLocation.getLongitude();

                                if(driverMarker != null)
                                    driverMarker.remove();
                                driverMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude,longitude))
                                        .title("You")
                                        .icon(BitmapDescriptorFactory.defaultMarker()));

                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude), 17.0f));

                                if(direction != null)
                                    direction.remove();
                                getDirection();

                        }
                        else
                        {
                            Log.d("Error","Cannot get your Location" );
                        }
                    }
                });

    }

    private void getDirection(){
        LatLng currentPosition = new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude());

        String requestApi = null;
        try{
            requestApi = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+currentPosition.latitude+","+currentPosition.longitude+"&"+
                    "destination="+riderLat+","+riderLng+"&"+
                    "key="+getResources().getString(R.string.google_direction_api);
            Log.d("DODROP", requestApi);
            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try{

                                new ParserTask().execute(response.body().toString());

                            }catch (Exception e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(DriverTracking.this,""+t.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpLocation(){
            buildLocationRequest();
            createLocationCallBack();
            displayLocation();

    }

    private void createLocationCallBack() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for(Location location:locationResult.getLocations())
                {
                    Common.mLastLocation = location;
                }
                displayLocation();
            }
        };

    }


    private void buildLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private class ParserTask extends AsyncTask<String,Integer,List<List<HashMap<String,String>>>> {
        ProgressDialog mDialog = new ProgressDialog(DriverTracking.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            mDialog.setMessage("Please Waiting..");
            mDialog.show();
        }
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jObect;
            List<List<HashMap<String, String>>>  routes = null;

            try{
                jObect = new JSONObject(strings[0]);
                DirectionJSONParser parser = new DirectionJSONParser();
                routes = parser.parse(jObect);
            } catch (JSONException e){
                e.printStackTrace();
            }

            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            mDialog.dismiss();

            ArrayList points = null;
            PolylineOptions polylineOptions = null;

            for(int i=0;i<lists.size();i++)
            {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();

                List<HashMap<String,String>> path = lists.get(i);

                for(int j=0;j<path.size();j++)
                {
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat,lng);

                    points.add(position);
                }

                polylineOptions.addAll(points);
                polylineOptions.width(10);
                polylineOptions.color(Color.RED);
                polylineOptions.geodesic(true);
            }
            direction = mMap.addPolyline(polylineOptions);
        }
    }
}
