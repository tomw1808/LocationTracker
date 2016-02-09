package com.vomtom.mytestservice.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.vomtom.mytestservice.MainActivity;
import com.vomtom.mytestservice.services.LocationService;

/**
 * Created by Thomas on 08.02.2016.
 */
public class LocationServiceRunningNotification {

    private Service context;

    public LocationServiceRunningNotification(Service context) {
        this.context = context;
    }

    public Notification getNotification() {


        Intent intent1 = new Intent(context, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent1, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setAutoCancel(false);
        builder.setTicker("Distance Tracker");
        builder.setContentTitle("Counting Distance");
        builder.setSmallIcon(android.R.drawable.ic_dialog_map);
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);
        return builder.build();
    }

    public Notification getNotification(String subtext) {


        Intent intent1 = new Intent(context, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent1, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setAutoCancel(false);
        builder.setTicker("Distance Tracker");
        builder.setContentTitle("Counting Distance");
        builder.setSmallIcon(android.R.drawable.ic_dialog_map);
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);
        builder.setContentText(subtext);
        return builder.build();
    }


}
