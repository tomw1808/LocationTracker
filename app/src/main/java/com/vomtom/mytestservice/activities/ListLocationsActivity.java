package com.vomtom.mytestservice.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.vomtom.mytestservice.Constants;
import com.vomtom.mytestservice.R;
import com.vomtom.mytestservice.adapters.LocationAdapter;
import com.vomtom.mytestservice.adapters.LocationCursorAdapter;
import com.vomtom.mytestservice.contracts.LocationContract;
import com.vomtom.mytestservice.listeners.OnDataSetReceivedListener;
import com.vomtom.mytestservice.listeners.OnHasNewLocationListener;
import com.vomtom.mytestservice.services.LocationService;
import com.vomtom.mytestservice.task.LocationTasks;

/**
 * Created by Thomas on 08.02.2016.
 */
public class ListLocationsActivity extends AppCompatActivity {

    LocationCursorAdapter mLocationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listlocations);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        setupLocationsAdapter();
        super.onResume();
    }

    private void setupLocationsAdapter() {
        LocationTasks.getOrInsertServerId(this, new OnDataSetReceivedListener() {
            @Override
            public void onDataSetReceived(Cursor cursor) {
                String server_id = cursor.getString(cursor.getColumnIndexOrThrow(LocationContract.ServeridEntry.COLUMN_NAME_SERVERID));
                setupLocationsAdapter(server_id);
            }
        });

    }
    private void setupLocationsAdapter(String server_id) {
        LocationTasks.getAllLocations(this, server_id, new OnDataSetReceivedListener() {
            @Override
            public void onDataSetReceived(Cursor cursor) {
                mLocationAdapter = new LocationCursorAdapter(ListLocationsActivity.this, cursor, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                ListView listView = (ListView) findViewById(R.id.detected_activities_listview);
                listView.setAdapter(mLocationAdapter);
            }
        });
    }

}
