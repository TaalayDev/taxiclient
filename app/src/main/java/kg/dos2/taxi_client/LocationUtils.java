package kg.dos2.taxi_client;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import static kg.dos2.taxi_client.MainActivity.LOG_TAG;

public class LocationUtils implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    interface MyLocationListener {
        void onLocationChanged(Location location);
    }

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;
    private LocationRequest mLocationRequest;

    private MyLocationListener myLocationListener;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private final boolean mRequestingLocationUpdates = false;

    // Location updates intervals in sec
    private final static int UPDATE_INTERVAL = 2000; // 10 sec
    private final static int FATEST_INTERVAL = 1000; // 5 sec
    private final static int DISPLACEMENT = 2; // 10 meters

    private Context context;
    private Activity activity;

    public void onResume() {
        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {

        }
    }

    public void onStart() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    public void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public LocationUtils(Context context, MyLocationListener myLocationListener) {
        this.context = context;
        this.activity = (Activity) context;
        this.myLocationListener = myLocationListener;
        if (checkPlayServices())
        {
            buildGoogleApiClient();
            createLocationRequest();
        }
    }

    /* *
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient()
    {
        try {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API).build();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    /**
     * Creating location request object
     * */
    protected void createLocationRequest()
    {
        try {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(UPDATE_INTERVAL);
            mLocationRequest.setFastestInterval(FATEST_INTERVAL);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices()
    {
        try {
            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
            if (resultCode != ConnectionResult.SUCCESS) {
                if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                    GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                            PLAY_SERVICES_RESOLUTION_REQUEST).show();
                } else {
                    Toast.makeText(context,
                            "This device is not supported.", Toast.LENGTH_LONG)
                            .show();
                    activity.finish();
                }
                return false;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
        return true;
    }

    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates()
    {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    mLocationRequest, this);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        myLocationListener.onLocationChanged(location);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

}
