package com.vomtom.mytestservice.listeners;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.View;

import com.google.android.gms.location.LocationRequest;
import com.vomtom.mytestservice.R;
import com.vomtom.mytestservice.services.LocationService;

/**
 * Created by Thomas on 08.02.2016.
 */
public class StartStopRunServiceListener implements View.OnClickListener {


    private LocationService mService;
    private Context context;
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mService = binder.getService();
            if(mService.isServiceStarted()) {
                mService.stopService();

            } else {

                /**
                 * set to Running.
                 */
                mService.setShowTimer(true);
                mService.setLocationUpdatePriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                mService.setUpdateInterval(1000);
                context.startService(new Intent(context, LocationService.class));
            }
            context.unbindService(mConnection);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    public void onClick(View v) {
        context = v.getContext();
        context.bindService(new Intent(context, LocationService.class), mConnection, Context.BIND_AUTO_CREATE);

    }
}
