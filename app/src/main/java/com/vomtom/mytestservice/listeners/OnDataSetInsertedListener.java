package com.vomtom.mytestservice.listeners;

import android.database.Cursor;

/**
 * Created by Thomas on 12.02.2016.
 */
public interface OnDataSetInsertedListener {
    void onDataSetInserted(long newRowId);
}
