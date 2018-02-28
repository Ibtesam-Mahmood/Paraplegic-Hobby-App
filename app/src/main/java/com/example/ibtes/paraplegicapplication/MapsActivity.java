package com.example.ibtes.paraplegicapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.yelp.fusion.client.connection.YelpFusionApi;
import com.yelp.fusion.client.connection.YelpFusionApiFactory;
import com.yelp.fusion.client.models.Coordinates;
import com.yelp.fusion.client.models.SearchResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private FusedLocationProviderClient mFusedLocationClient;

    private YelpFusionApi yelpFusionApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        String yelpApiKey = getString(R.string.yelp_fusion_api_key);

        try {
            yelpFusionApi =  new YelpFusionApiFactory().createAPI(yelpApiKey);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        getLocation();

    }

    //Locates the current location for the user and moves the map to the location
    private void getLocation(){

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Checks if the permission is granted
        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
            getLocation();
            return;

        }
        else{ //If the permission is granted

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if(location != null){
                                LatLng latLng =  new LatLng(location.getLatitude(), location.getLongitude());
                                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(
                                        latLng,
                                        12f
                                );
                                MarkerOptions marker =  new MarkerOptions()
                                        .position(latLng);
                                mMap.moveCamera(update);
                                mMap.addMarker(marker);
                                yelpMarker(latLng);
                            }
                        }
                    });
        }

    }

    public void yelpMarker(LatLng latLng){

        Map<String, String> params =  new HashMap<>();

        params.put("term", "food");
        params.put("latitude", latLng.latitude + "");
        params.put("longitude", latLng.longitude + "");

        Call<SearchResponse> call = yelpFusionApi.getBusinessSearch(params);

        Callback<SearchResponse> callback = new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                SearchResponse searchResponse = response.body();
                if(searchResponse != null){
                    Coordinates coordinates =  searchResponse.getBusinesses().get(0).getCoordinates();
                    String companyName = searchResponse.getBusinesses().get(0).getName();

                    LatLng markerLocation = new LatLng(coordinates.getLatitude(), coordinates.getLongitude());

                    MarkerOptions marker =  new MarkerOptions()
                            .position(markerLocation)
                            .title(companyName);
                    mMap.addMarker(marker);
                }


            }
            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                // HTTP error happened, do something to handle it.
            }
        };

        call.enqueue(callback);


    }

    //Prints a msg as a toast
    public static void printToast(String m, Context context){

        Toast.makeText(context, m, Toast.LENGTH_SHORT).show();

    }
}
