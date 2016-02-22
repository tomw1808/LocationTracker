package com.vomtom.mytestservice.listeners;

import android.database.Cursor;
import android.location.Location;

/**
 * Created by Thomas on 12.02.2016.
 */
public interface OnDataSetReceivedListener {
    void onDataSetReceived(Cursor cursor);
}
