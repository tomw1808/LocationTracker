package com.vomtom.mytestservice;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.android.gms.location.LocationRequest;
import com.vomtom.mytestservice.activities.ListLocationsActivity;
import com.vomtom.mytestservice.listeners.OnGeocoderFinishedListener;
import com.vomtom.mytestservice.listeners.OnHasNewLocationListener;
import com.vomtom.mytestservice.services.LocationService;
import com.vomtom.mytestservice.task.GetCityName;

import java.text.DecimalFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnHasNewLocationListener {

    LocationService mService;
    boolean mBound = false;
    private boolean isFabOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(startLocationServiceClickListener);

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
            Intent intent = new Intent(MainActivity.this, ListLocationsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        bindToService();
        super.onResume();
    }

    @Override
    protected void onPause() {
        // Unregister the broadcast receiver that was registered during onResume().
        if (mBound) {
            mService.unsetOnHasNewLocationListener();
            try {
                unbindService(mConnection);
                mBound = false;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        super.onPause();
    }




    private void bindToService() {

        if (!mBound) {
            bindService(new Intent(this, LocationService.class), mConnection, Context.BIND_AUTO_CREATE);
        }

    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.setOnHasNewLocationListener(MainActivity.this);
            if (mService.getmLocations().size() > 0) {
                updateCityName(mService.getmLocations().get(0));
                updateStats(mService.getmLocations().get(0));
            }
            if(mService.isServiceStarted()) {
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                fab.setImageResource(android.R.drawable.ic_media_pause);
            }
        }


        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mService = null;
        }
    };


    private View.OnClickListener startLocationServiceClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            animateFAB(v);
        }
    };

    @Override
    public void locationAdded(Location location) {
        updateCityName(location);
        updateStats(location);
    }


    private void updateCityName(Location location) {
        new GetCityName().getCityName(this, location, new OnGeocoderFinishedListener() {
            @Override
            public void onFinished(List<Address> results) {
                if (results.size() > 0) {
                    StringBuilder builder = new StringBuilder();
                    int maxLines = results.get(0).getMaxAddressLineIndex();
                    for (int i = 0; i < maxLines; i++) {
                        String addressStr = results.get(0).getAddressLine(i);
                        builder.append(addressStr);
                        builder.append(" ");
                    }

                    String currentAddress = builder.toString(); //This is the complete address.
                    TextView mAddressLine = (TextView) findViewById(R.id.textview_address);
                    mAddressLine.setText(currentAddress);
                }
            }
        });
    }


    private void updateStats(final Location lastLocation) {
        if (mBound) {
            TextView mDistance = (TextView) findViewById(R.id.textview_distance);
            DecimalFormat df = new DecimalFormat("0.00");
            mDistance.setText(df.format(mService.getDistance() / 1000) + " km");


            TextView mLastPosition = (TextView) findViewById(R.id.textview_position);

            mLastPosition.setText("Lat: " + lastLocation.getLatitude() + " Lng:" + lastLocation.getLongitude());

            mLastPosition.setOnClickListener(null);
            mLastPosition.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri gmmIntentUri = Uri.parse("geo:" + lastLocation.getLatitude() + "," + lastLocation.getLongitude());
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    }
                }
            });

        }
    }

    public void animateFAB(View v) {

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        FloatingActionButton fab1 = (FloatingActionButton) findViewById(R.id.fab_run);
        FloatingActionButton fab2 = (FloatingActionButton) findViewById(R.id.fab_bike);
        Animation fab_open, fab_close;
        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        if (isFabOpen) {

            fab1.startAnimation(fab_close);
            fab2.startAnimation(fab_close);
            fab1.setClickable(false);
            fab2.setClickable(false);
            isFabOpen = false;

            if (v.getId() == R.id.fab_run) {
                mService.setShowTimer(true);
                mService.setLocationUpdatePriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                mService.setUpdateInterval(5000);
                startService(new Intent(this, LocationService.class));
                fab.setImageResource(android.R.drawable.ic_media_pause);
            }
            if (v.getId() == R.id.fab_bike) {
                mService.setShowTimer(false);
                mService.setLocationUpdatePriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                mService.setUpdateInterval(25000);
                startService(new Intent(this, LocationService.class));
                fab.setImageResource(android.R.drawable.ic_media_pause);
            }
        } else {
            if (mBound && mService.isServiceStarted()) {
                mService.stopService();
                fab.setImageResource(android.R.drawable.ic_media_play);
            } else {
                fab1.startAnimation(fab_open);
                fab2.startAnimation(fab_open);
                fab1.setClickable(true);
                fab2.setClickable(true);
                fab1.setOnClickListener(startLocationServiceClickListener);
                fab2.setOnClickListener(startLocationServiceClickListener);
                isFabOpen = true;
            }

        }


    }


}
