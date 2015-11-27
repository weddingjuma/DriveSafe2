package com.drivesafe2;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by cozofdeath on 10/25/2015.
 */
public class RootService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "com.drivesafe2";

    String mCurrentUpdateTime, mLastUpdateTime;
    Location mCurrentLocation, mLastLocation;
    LocationRequest mLocationRequest;
    LayoutInflater layoutInflater;
    WindowManager windowManager;
    View mView;
    private GoogleApiClient mGoogleApiClient;

    static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Root Service Created...");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    // Let it continue running until it is stopped.
        Toast.makeText(this, "Root Service Started", Toast.LENGTH_LONG).show();
        Log.i(TAG, "Root Service Started...");

        checkPlayServices();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

/*
        Intent LockActivityIntent = new Intent(this, LockActivity.class);
        LockActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(LockActivityIntent);
*/

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Root Service Terminated", Toast.LENGTH_LONG).show();
        Log.i(TAG, "Root Service Terminated...");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location Changed! Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude());

        if(location == null)
        {
            return;
        }

        else if(location.getSpeed() != 0)
        {
            // convert it to km/h
            // int speed=(int) ((location.getSpeed()*3600)/1000);

            // convert to mph
            int speed = (int)(location.getSpeed()*2.2369);

            if(speed > 25)
            {
                if(mView == null)
                    showLockScreen();
            }
            else
            {
                if (mView != null)
                    disableLockScreen();
            }

            mLastLocation = mCurrentLocation;
            mLastUpdateTime = mCurrentUpdateTime;
            return;
        }

        else if (mLastLocation == null){
            mLastLocation = location;
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            return;
        }

        else
        {
            mCurrentLocation = location;
            mCurrentUpdateTime = DateFormat.getTimeInstance().format(new Date());

            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss aa");
            
            float mDistanceTo = mLastLocation.distanceTo(mCurrentLocation);
            float mSpeed = mDistanceTo/(Integer.parseInt(mCurrentUpdateTime) - Integer.parseInt(mLastUpdateTime));

            Log.i(TAG, "Calculated Speed is " + mSpeed);

            if(mSpeed > 25)
            {
                if(mView == null)
                    showLockScreen();
            }
            else
            {
                if (mView != null)
                    disableLockScreen();
            }

            mLastLocation = mCurrentLocation;
            mLastUpdateTime = mCurrentUpdateTime;
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.i(TAG, "Location services connected.");
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(location != null)
        {
            Log.d("Location: ", location.toString());
        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        mCurrentLocation = location;
        mLastLocation = mCurrentLocation;
        mCurrentUpdateTime = DateFormat.getTimeInstance().format(new Date());
        mLastUpdateTime = mCurrentUpdateTime;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    public void showLockScreen()
    {
        layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams mLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY              //Window type: system overlay windows, which need to be displayed on top of everything else.
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED  //Window flag: special flag to let windows be shown when the screen is locked.
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD  //Window flag: when set the window will cause the keyguard to be dismissed, only if it is not a secure lock keyguard.
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON    //Window flag: when set as a window is being added or made visible, once the window has been shown then the system will poke the power manager's user activity
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN        //Window flag: hide all screen decorations (such as the status bar) while this window is displayed.
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN  //Window flag: place the window within the entire screen, ignoring decorations around the border (such as the status bar).
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN    //Window flag: allow window contents to extend in to the screen's overscan area, if there is one.
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE     //Window flag: this window won't ever get key input focus, so the user can not send key or other button events to it.
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE     //Window flag: this window can never receive touch events.
                        | WindowManager.LayoutParams.FLAG_SCALED            //Window flag: a special mode where the layout parameters are used to perform scaling of the surface when it is composited to the screen.
                        | WindowManager.LayoutParams.FLAG_DIM_BEHIND        //Window flag: everything behind this window will be dimmed.
                        | PixelFormat.RGBA_8888
        );

        mView = layoutInflater.inflate(R.layout.lock_layout, null);
        windowManager.addView(mView, mLayoutParams);
    }

    public void disableLockScreen()
    {
        mView.setVisibility(View.GONE);
        if(mView != null)
        {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mView);
            mView = null;
        }
    }

    private boolean checkPlayServices() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
                GooglePlayServicesUtil.getErrorDialog(status, (Activity) getApplicationContext(),
                        REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
            }
            else {
                Toast.makeText(this, "This device is not supported.",
                        Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }

}
