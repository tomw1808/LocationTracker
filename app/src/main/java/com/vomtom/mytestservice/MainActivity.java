package com.vomtom.mytestservice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.vomtom.mytestservice.adapters.LocationAdapter;

import junit.framework.Test;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    TestService mService;
    boolean mBound = false;
    ListView mLatLng;
    LocationAdapter mLocationAdapter;
    LocationUpdateBroadcastReceiver mBroadcastReceiver;
    ServiceStartedBroadcastReceiver mServiceStartedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(startStopListener);

        FloatingActionButton fab1 = (FloatingActionButton) findViewById(R.id.fab1);
        fab1.setOnClickListener(updateLocationListener);

        bindToService();
        updateStartStop();

        // Get a receiver for broadcasts from ActivityDetectionIntentService.
        mBroadcastReceiver = new LocationUpdateBroadcastReceiver();
        mServiceStartedReceiver = new ServiceStartedBroadcastReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver that informs this activity of the DetectedActivity
        // object broadcast sent by the intent service.
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(Constants.BROADCAST_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(mServiceStartedReceiver,
                new IntentFilter(Constants.SERVICE_STARTED));
        bindToService();
    }

    @Override
    protected void onPause() {
        // Unregister the broadcast receiver that was registered during onResume().
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceStartedReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private void bindToService() {

        if(!mBound) {
            bindService(new Intent(this, TestService.class), mConnection, Context.BIND_AUTO_CREATE);
        }

    }



    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            TestService.LocalBinder binder = (TestService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            updateStartStop();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            updateStartStop();
        }
    };

    private View.OnClickListener updateLocationListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if(mBound) {
                mService.updateLastLocation();
            }
        }
    };

    private View.OnClickListener startStopListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mBound) {
                if (mService.isServiceStarted()) {
                    mService.stopService();
                } else {
                    startService(new Intent(v.getContext(), TestService.class));
                }
            }
            updateStartStop();
        }
    };

    private void updateStartStop() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if(mBound) {
            if (mService.isServiceStarted()) {
                fab.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                fab.setImageResource(android.R.drawable.ic_media_play);
            }
            mLatLng = (ListView) findViewById(R.id.detected_activities_listview);

            mLocationAdapter = new LocationAdapter(this,0,mService.getmLocations());
            mLatLng.setAdapter(mLocationAdapter);

        }

    }


    /**
     * Receiver for intents sent by DetectedActivitiesIntentService via a sendBroadcast().
     * Receives a list of one or more DetectedActivity objects associated with the current state of
     * the device.
     */
    public class LocationUpdateBroadcastReceiver extends BroadcastReceiver {
        protected static final String TAG = "location-update-response-receiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Location> locations =
                    intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);
            updateLocationList(locations);
        }
    }
    /**
     * Receiver for intents sent by DetectedActivitiesIntentService via a sendBroadcast().
     * Receives a list of one or more DetectedActivity objects associated with the current state of
     * the device.
     */
    public class ServiceStartedBroadcastReceiver extends BroadcastReceiver {
        protected static final String TAG = "service-started-response-receiver";

        @Override
        public void onReceive(Context context, Intent intent) {
           updateStartStop();
        }
    }

    private void updateLocationList(ArrayList<Location> locations) {
        mLocationAdapter.notifyDataSetChanged();
    }


}
