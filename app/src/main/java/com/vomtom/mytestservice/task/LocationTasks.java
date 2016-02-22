package com.vomtom.mytestservice.task;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

import com.vomtom.mytestservice.contracts.LocationContract;
import com.vomtom.mytestservice.dbhelper.LocationDbHelper;
import com.vomtom.mytestservice.listeners.OnDataSetInsertedListener;
import com.vomtom.mytestservice.listeners.OnDataSetReceivedListener;
import com.vomtom.mytestservice.task.exception.NoServerIdFoundException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class LocationTasks {

	private static void goBlooey(Throwable t, Context context) {
		AlertDialog.Builder builder=new AlertDialog.Builder(context);

		builder
				.setTitle("Exception!")
				.setMessage(t.toString())
				.setPositiveButton("OK", null)
				.show();
	}

	public static void getAllLocations(final Context context, final String server_id, final OnDataSetReceivedListener listener) {

		new AsyncTask<Void, Integer, Cursor>() {

			private Exception exception = null;
			private Cursor cursor = null;

			@Override
			protected Cursor doInBackground(Void... params) {
				LocationDbHelper locationDbHelper = new LocationDbHelper(context);
				// Gets the data repository in write mode
				SQLiteDatabase db = locationDbHelper.getWritableDatabase();
				// Define a projection that specifies which columns from the database
				// you will actually use after this query.
				String[] projection = {
						LocationContract.LocationEntry._ID,
						LocationContract.LocationEntry.COLUMN_NAME_LAT,
						LocationContract.LocationEntry.COLUMN_NAME_LNG,
						LocationContract.LocationEntry.COLUMN_NAME_ACCURACY,
						LocationContract.LocationEntry.COLUMN_NAME_ADDRESS,
						LocationContract.LocationEntry.COLUMN_NAME_ALTITUDE,
						LocationContract.LocationEntry.COLUMN_NAME_BEARING,
						LocationContract.LocationEntry.COLUMN_NAME_SPEED,
						LocationContract.LocationEntry.COLUMN_NAME_TIME,
						LocationContract.LocationEntry.COLUMN_NAME_TRANSFERRED,
				};

				String sortOrder =
						LocationContract.LocationEntry._ID + " DESC";

				String[] where = {server_id};
				cursor = db.query(LocationContract.LocationEntry.TABLE_NAME, projection, LocationContract.LocationEntry.COLUMN_NAME_SERVERID + " = ?", where, null, null, sortOrder);
				return cursor;

			}


			@Override
			protected void onPostExecute(Cursor cursor) {
				if(exception != null) {
					goBlooey(exception, context);
				} else if (cursor != null && listener != null) {
					listener.onDataSetReceived(cursor);
				}
			}
		}.execute();


	}

	public static void getOrInsertServerId(final Context context, final OnDataSetReceivedListener listener) {


		new AsyncTask<Void, Integer, Cursor>() {

			private Exception exception = null;
			private Cursor cursor = null;

			@Override
			protected Cursor doInBackground(Void... params) {
				LocationDbHelper locationDbHelper = new LocationDbHelper(context);
				// Gets the data repository in write mode
				SQLiteDatabase db = locationDbHelper.getWritableDatabase();
				// Define a projection that specifies which columns from the database
				// you will actually use after this query.
				String[] projection = {
						LocationContract.ServeridEntry._ID,
						LocationContract.ServeridEntry.COLUMN_NAME_SERVERID
				};

				String sortOrder =
						LocationContract.ServeridEntry._ID + " DESC";


				cursor = db.query(LocationContract.ServeridEntry.TABLE_NAME, projection, null, null, null, null, sortOrder, "1");
				if (!cursor.moveToFirst()) {
					exception = new NoServerIdFoundException("No Server ID found. Insert one first.");
				}
				return cursor;

			}


			@Override
			protected void onPostExecute(Cursor cursor) {
				if(exception != null) {
					if(exception instanceof NoServerIdFoundException) {
						/**
						 * I would love to call that in the "doInBackground" method, but cannot, as another async task can only be started from the UI Thread. So, we have to do that in the onPostExecute thing.
						 *
						 * The way of checking if an exception was blowing up, is from http://stackoverflow.com/questions/1739515/asynctask-and-error-handling-on-android
						 * https://github.com/commonsguy/cw-lunchlist/tree/master/15-Internet/LunchList
						 */
						insertNewServerId(context, new OnDataSetInsertedListener() {
							@Override
							public void onDataSetInserted(long newRowId) {
								//a new Server ID was inserted. Whatever the Row-ID is, we don't care, we just want to have the last server id.
								getOrInsertServerId(context, listener);
							}
						});
					} else {
						goBlooey(exception, context);
					}
				} else if (cursor != null && listener != null) {
					listener.onDataSetReceived(cursor);
				}
			}
		}.execute();


	}


	public static void insertNewServerId(final Context context, final OnDataSetInsertedListener listener) {
		new AsyncTask<Void, Integer, Long>() {

			private Exception exception = null;
			private Long db_id = null;
			@Override
			protected Long doInBackground(Void... params) {
				URL    url              = null;
				String jsonStringReturn = null;
				try {
					url = new URL("http://www.newscombinator.com/location/search");

					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
					urlConnection.setRequestMethod("GET");

					urlConnection.setReadTimeout(2000);
					urlConnection.connect();
					// read the output from the server
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
					StringBuilder stringBuilder = new StringBuilder();

					String line = null;
					while ((line = bufferedReader.readLine()) != null) {
						stringBuilder.append(line + "\n");
					}
					jsonStringReturn = stringBuilder.toString();
					try {
						InputStream in = new BufferedInputStream(urlConnection.getInputStream());
					} finally {
						urlConnection.disconnect();
					}


					JSONObject jObject = new JSONObject(jsonStringReturn);
					LocationDbHelper locationDbHelper = new LocationDbHelper(context);
					// Gets the data repository in write mode
					SQLiteDatabase db = locationDbHelper.getWritableDatabase();

					// Create a new map of values, where column names are the keys
					ContentValues values = new ContentValues();
					values.put(LocationContract.ServeridEntry.COLUMN_NAME_SERVERID, jObject.getInt("server_id"));
					values.put(LocationContract.ServeridEntry.COLUMN_NAME_TIME, new Date().getTime());

					db_id = db.insert(LocationContract.ServeridEntry.TABLE_NAME, "null", values);

				} catch (MalformedURLException e) {
					exception = e;
				} catch (IOException e) {
					exception = e;
				} catch (JSONException e) {
					exception = e;
				}

				return db_id;


			}

			@Override
			protected void onPostExecute(Long newRowId) {
				if (newRowId != null && listener != null) {
					listener.onDataSetInserted(newRowId);
				}
			}

		}.execute();
	}

	public static void addLocationTask(final Context context, final Location location, final String serverid, final OnDataSetInsertedListener listener) {
		new AsyncTask<Void, Integer, Long>() {
			@Override
			protected Long doInBackground(Void... arg0) {
				LocationDbHelper locationDbHelper = new LocationDbHelper(context);
				// Gets the data repository in write mode
				SQLiteDatabase db = locationDbHelper.getWritableDatabase();

				// Create a new map of values, where column names are the keys
				ContentValues values = new ContentValues();
				values.put(LocationContract.LocationEntry.COLUMN_NAME_ACCURACY, location.getAccuracy());
				values.put(LocationContract.LocationEntry.COLUMN_NAME_ALTITUDE, location.getAltitude());
				values.put(LocationContract.LocationEntry.COLUMN_NAME_BEARING, location.getBearing());
				values.put(LocationContract.LocationEntry.COLUMN_NAME_LAT, location.getLatitude());
				values.put(LocationContract.LocationEntry.COLUMN_NAME_LNG, location.getLongitude());
				values.put(LocationContract.LocationEntry.COLUMN_NAME_SERVERID, serverid);
				values.put(LocationContract.LocationEntry.COLUMN_NAME_TRANSFERRED, 0);
				values.put(LocationContract.LocationEntry.COLUMN_NAME_TIME, location.getTime());

				return db.insert(LocationContract.LocationEntry.TABLE_NAME, "null", values);
			}


			@Override
			protected void onPostExecute(Long newRowId) {
				if (newRowId != null && listener != null) {
					listener.onDataSetInserted(newRowId);
				}
			}
		}.execute();
	}
}
