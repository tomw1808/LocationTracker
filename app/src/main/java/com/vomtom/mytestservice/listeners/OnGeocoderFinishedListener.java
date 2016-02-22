package com.vomtom.mytestservice.listeners;

import android.location.Address;

import java.util.List;

public interface OnGeocoderFinishedListener {
    void onFinished(List<Address> results);
}