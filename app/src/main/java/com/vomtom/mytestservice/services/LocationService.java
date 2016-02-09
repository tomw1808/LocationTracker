package com.vomtom.mytestservice.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.os.*;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.vomtom.mytestservice.Constants;
import com.vomtom.mytestservice.listeners.OnGeocoderFinishedListener;
import com.vomtom.mytestservice.notifications.LocationServiceRunningNotification;
import com.vomtom.mytestservice.task.GetCityName;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LocationService extends Service implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    public static final int notification_id = 1;
    public static final int min_accuracy = 70;

    protected GoogleApiClient mGoogleApiClient;
    private final IBinder mBinder = new LocalBinder();
    private ArrayList<Location> mLocations = new ArrayList<Location>();
    private boolean serviceStarted = false;
    private double distance = 0;

    private int locationUpdatePriority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;

    private long startTime = 0;
    private boolean showTimer = true;
    private int updateInterval = 1500;



    public ArrayList<Location> getmLocations() {
        return this.mLocations;
    }

    public void setLocationUpdatePriority(int locationUpdatePriority) {
        this.locationUpdatePriority = locationUpdatePriority;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setShowTimer(boolean showTimer) {
        this.showTimer = showTimer;
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No Permission to Access Location, Aborting", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }


        Toast.makeText(this, "Location API connection successful", Toast.LENGTH_LONG).show();


    }

    public void updateLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            mLocations.add(mLastLocation);
        }
        Intent localIntent = new Intent(Constants.BROADCAST_ACTION);

        // Broadcast the list of detected activities.
        localIntent.putExtra(Constants.ACTIVITY_EXTRA, mLocations);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }


    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection Suspended", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_LONG).show();
    }


    @Override
    public void onCreate() {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mGoogleApiClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        serviceStarted = true;
        Intent localIntent = new Intent(Constants.SERVICE_STARTED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        // If we get killed, after returning from here, restart
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(15000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(locationUpdatePriority);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "Listening to Location not possible, permission not set. Aborting.", Toast.LENGTH_SHORT).show();
            return 0;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

        startForeground(LocationService.notification_id, new LocationServiceRunningNotification(this).getNotification());

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean isConnected() {
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }

    public void stopService() {
        serviceStarted = false;
        Intent localIntent = new Intent(Constants.SERVICE_STARTED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);



        stopSelf();
    }

    public boolean isServiceStarted() {
        return serviceStarted;
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(LocationService.notification_id);
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location.hasAccuracy() && location.getAccuracy() < LocationService.min_accuracy) {
            mLocations.add(0, location);

            if (mLocations.size() > 0) {
                distance = 0;
                Location last_loc = mLocations.get(0);
                for (Location loc : mLocations) {
                    distance += last_loc.distanceTo(loc);
                }
            }


            updateNotification();
            Intent localIntent = new Intent(Constants.BROADCAST_ACTION);
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        }

    }

    public double getDistance() {
        return distance;
    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationService.this;
        }
    }

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            updateNotification();
            if(showTimer) {
                timerHandler.postDelayed(this, 50);
            }
        }
    };


    private void updateNotification() {


        if(showTimer) {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            DecimalFormat df = new DecimalFormat("###0000");
            Notification notification = new LocationServiceRunningNotification(this).getNotification(df.format(distance)+ " in "+String.format("%d:%02d", minutes, seconds));
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotificationManager.notify(LocationService.notification_id, notification);
        } else {
            DecimalFormat df = new DecimalFormat("###0.00");
            Notification notification = new LocationServiceRunningNotification(this).getNotification(df.format(distance / 1000) + " km since "+ android.text.format.DateFormat.getTimeFormat(this).format(new Date(startTime)));
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotificationManager.notify(LocationService.notification_id, notification);
        }

    }


}
