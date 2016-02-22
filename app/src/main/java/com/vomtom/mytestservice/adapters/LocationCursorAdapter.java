package com.vomtom.mytestservice.adapters;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.vomtom.mytestservice.R;
import com.vomtom.mytestservice.contracts.LocationContract;
import com.vomtom.mytestservice.services.LocationService;

import java.util.List;

/**
 * Created by Thomas on 06.02.2016.
 */
public class LocationCursorAdapter extends CursorAdapter {

    public LocationCursorAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(
                R.layout.detected_activity, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        // Find the UI widgets.
        TextView lat = (TextView) view.findViewById(R.id.latitude);
        TextView lng = (TextView) view.findViewById(R.id.longitude);

        // Populate widgets with values.
        lat.setText("Lat: " + cursor.getString(cursor.getColumnIndexOrThrow(LocationContract.LocationEntry.COLUMN_NAME_LAT)));
        lng.setText("Lng: " + cursor.getString(cursor.getColumnIndexOrThrow(LocationContract.LocationEntry.COLUMN_NAME_LNG)));
    }
}
