package com.vomtom.mytestservice.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.vomtom.mytestservice.contracts.LocationContract;
import com.vomtom.mytestservice.listeners.OnDataSetInsertedListener;
import com.vomtom.mytestservice.listeners.OnDataSetReceivedListener;
import com.vomtom.mytestservice.listeners.OnHasNewLocationListener;
import com.vomtom.mytestservice.notifications.LocationServiceRunningNotification;
import com.vomtom.mytestservice.task.LocationTasks;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class LocationService extends Service implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

	public static final int notification_id = 1;
	public static final int min_accuracy    = 70;

	protected GoogleApiClient mGoogleApiClient;
	private final IBinder             mBinder        = new LocalBinder();
	private       ArrayList<Location> mLocations     = new ArrayList<>();
	private       boolean             serviceStarted = false;
	private       double              distance       = 0;

	private int locationUpdatePriority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;

	private long    startTime      = 0;
	private boolean showTimer      = true;
	private int     updateInterval = 5000;
	private String  server_id      = null;
	OnHasNewLocationListener onHasNewLocationListener;

	public void setOnHasNewLocationListener(OnHasNewLocationListener onHasNewLocationListener) {
		this.onHasNewLocationListener = onHasNewLocationListener;
	}

	public ArrayList<Location> getmLocations() {
		return this.mLocations;
	}

	public void setLocationUpdatePriority(int locationUpdatePriority) {
		this.locationUpdatePriority = locationUpdatePriority;
	}

	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
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
		// If we get killed, after returning from here, restart
		LocationRequest mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(updateInterval);
		mLocationRequest.setFastestInterval(5000);
		mLocationRequest.setPriority(locationUpdatePriority);
		mLocationRequest.setSmallestDisplacement(250);
		LocationServices.FusedLocationApi.requestLocationUpdates(
				mGoogleApiClient, mLocationRequest, this);


		Toast.makeText(this, "Location API connection successful", Toast.LENGTH_SHORT).show();


	}

	@Override
	public void onConnectionSuspended(int i) {
		Toast.makeText(this, "Connection Suspended", Toast.LENGTH_LONG).show();

	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		Toast.makeText(this, "Connection Failed", Toast.LENGTH_LONG).show();
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Create an instance of GoogleAPIClient.
		if (mGoogleApiClient == null) {
			mGoogleApiClient = new GoogleApiClient.Builder(this)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.addApi(LocationServices.API)
					.build();
		}

		if (!mGoogleApiClient.isConnected()) {
			mGoogleApiClient.connect();
		}
		Toast.makeText(this, "Location Service starting", Toast.LENGTH_SHORT).show();
		serviceStarted = true;

		startForeground(LocationService.notification_id, new LocationServiceRunningNotification(this).getNotification());

		startTime = System.nanoTime();
		timerHandler.postDelayed(timerRunnable, 0);
		mLocations = new ArrayList<>();

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public void stopService() {
		serviceStarted = false;
		if (mGoogleApiClient != null) {
			LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
			if (mGoogleApiClient.isConnected()) {
				mGoogleApiClient.disconnect();
			}
		}


		stopForeground(true);
		stopSelf();
		//NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		//manager.cancel(LocationService.notification_id);
		Toast.makeText(this, "Location Service Stopped.", Toast.LENGTH_SHORT).show();
	}


	public boolean isServiceStarted() {
		return serviceStarted;
	}

	@Override
	public void onDestroy() {

		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		manager.cancel(LocationService.notification_id);
		Toast.makeText(this, "Location Service Done.", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onLocationChanged(final Location location) {
		if (location.hasAccuracy()
			&& location.getAccuracy() < LocationService.min_accuracy
			&& (mLocations.size() == 0 || (mLocations.size() > 0 && mLocations.get(0).distanceTo(location) > 250)) //minimum 100m to the past location
				) {
			mLocations.add(0, location);

			if (server_id == null) {
				LocationTasks.getOrInsertServerId(this, new OnDataSetReceivedListener() {
					@Override
					public void onDataSetReceived(Cursor cursor) {
						server_id = cursor.getString(cursor.getColumnIndexOrThrow(LocationContract.ServeridEntry.COLUMN_NAME_SERVERID));
						addLocationToSqlite(location);
					}
				});
			} else {
				addLocationToSqlite(location);
			}

			if (mLocations.size() > 0) {
				distance = 0;
				Location last_loc = mLocations.get(0);
				for (Location loc : mLocations) {
					if (loc != last_loc) {
						distance += last_loc.distanceTo(loc);
					}
					last_loc = loc;
				}
			}


			updateNotification();
			if (onHasNewLocationListener != null) {
				onHasNewLocationListener.locationAdded(location);
			}

		}

	}

	private void addLocationToSqlite(Location location) {
		LocationTasks.addLocationTask(this, location, server_id, new OnDataSetInsertedListener() {
			@Override
			public void onDataSetInserted(long newRowId) {
				//ignore
			}
		});
	}

	public double getDistance() {
		return distance;
	}

	public void unsetOnHasNewLocationListener() {
		this.onHasNewLocationListener = null;
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
	Handler  timerHandler  = new Handler();
	Runnable timerRunnable = new Runnable() {

		@Override
		public void run() {
			if (serviceStarted) {
				updateNotification();
				if (showTimer) {
					timerHandler.postDelayed(this, 50);
				}
			}
		}
	};


	private void updateNotification() {

		if (showTimer) {
			long nanos = System.nanoTime() - startTime;
			long miliseconds = TimeUnit.NANOSECONDS.toMillis(nanos);
			Date date = new Date(miliseconds); // if you really have long

			SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			String result = dateFormat.format(date.getTime());

			DecimalFormat df = new DecimalFormat("###0000");
			Notification notification = new LocationServiceRunningNotification(this).getNotification(df.format(distance) + "m in " + result);
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			mNotificationManager.notify(LocationService.notification_id, notification);
		} else {
			DecimalFormat df = new DecimalFormat("###0.00");
			Notification notification = new LocationServiceRunningNotification(this).getNotification(df.format(distance / 1000) + " km since " + android.text.format.DateFormat.getTimeFormat(this).format(new Date(startTime)));
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			mNotificationManager.notify(LocationService.notification_id, notification);
		}

	}


}
