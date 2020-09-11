package com.example.myruns.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.content.AsyncTaskLoader;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.example.myruns.Fragment.HistoryFragment;
import com.example.myruns.Fragment.StartFragment;
import com.example.myruns.Model.ExerciseDataSource;
import com.example.myruns.Model.ExerciseEntryStructure;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.example.myruns.R;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import org.json.JSONException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, ServiceConnection {

    // Broadcasting Service and Binding Service Information
    public static final String UPDATE_ACTION = "Update"; // Action for ActivityUpdate Receiver
    public static final String FOR_AUTOMATIC = "forAuto";      // Whether we want activity recognition updates
    private static final String FROMTAG = "gpsauto";
    private IntentFilter mUpdateFilter; // Intent filter for the broadcast receiver
    private ActivityUpdateReceiver mActivityUpdateReceiver; // The Broadcast Receiver for updates
    private boolean mIsBound; // Is our activity bound to the broadcast receiver
    private Intent mServiceIntent; // Intent to start the trackingService
    private TrackingService trackingService; // trackingService instance from running TrackingService
    private ServiceConnection mConnection = this; // connect MapActivity to Service to use BindService

    // Map Information
    private GoogleMap mMap;                 // map
    private LatLng mStartLocation;          // StartLocation
    public Marker mStartMarker;             // Start Marker for start location
    private LatLng mLiveLocation;           // Current Location
    private LatLng mPrevLocation;
    public Marker mLiveMarker;              // Marker for current location
    public PolylineOptions rectOptions;     // PolylineOptions for our drawing on map
    public Polyline polyline;               // The polyline that will be on the map
    private boolean mIsFirst;               // Boolean to catch the first location
    public double mStartingAltitude;

    // Database Information
    private ExerciseEntryStructure mExerciseEntry;  // Structure to hold current exercise entry
    private long mEntryID;
    private Date mStartTime;

    // Map TextViews
    private TextView mLabel;                // What activity are we doing right now
    public TextView mCurrSpeed;      // With what speed?
    public TextView mAverageSpeed;
    public TextView mDistance;
    public TextView mCalories;
    public TextView mElevation;
    private Button mSaveDeleteButton;

    // Where we are coming from
    private boolean fromHistory;
    private boolean startAutomatic;
    private boolean startGPS;


    // TAGS
    private static final String LOCATION_TAG = "location";
    private static final String BIND_TAG = "Binding";

    // Database Storage!
    public ExerciseDataSource mExerciseDataSource;
    HashMap<Integer, Integer> activityCount;

    /// LIFECYCLE METHODS ///

    /**
     * Creates the activity. Starts our service, shows the map and fire up the BroadcastReceiver
     * @param savedInstanceState
     */
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);


        // Get the Intent Bundle so we can decide what activity we are going to...
        Bundle extras;
        if (savedInstanceState == null) {
            extras = getIntent().getExtras();
        } else {
            extras = savedInstanceState;
        }

        findOutActivities(extras);

        // Data Labels and Database Setup
        mCurrSpeed = findViewById(R.id.curr_speed);
        mAverageSpeed = findViewById(R.id.avg_speed);
        mDistance = findViewById(R.id.distance);
        mCalories = findViewById(R.id.calories);
        mElevation = findViewById(R.id.elevation);
        mLabel = findViewById(R.id.activity_type);

        mExerciseDataSource = new ExerciseDataSource(this);
        try {
            mExerciseDataSource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }


        if (!fromHistory) {

            // Start the tracking service via an intent and then bind the service.
            mServiceIntent = new Intent(MapActivity.this, TrackingService.class);
            mServiceIntent.putExtra(FOR_AUTOMATIC, startAutomatic);
            startService(mServiceIntent);
            mIsBound = false;
            mIsFirst = true;
            automaticBind();

            // Instantiate our Broadcast Receiver
            // DO NOT REGISTER HERE BECAUSE ONCREATE IS NOT ALWAYS CALLED! I LEARNED THAT THROUGH AN
            // HOUR OF S T A C K O V E R F L O W. Rather, register and unregister with onResume and onPause


            mActivityUpdateReceiver = new ActivityUpdateReceiver();
            mUpdateFilter = new IntentFilter();
            mUpdateFilter.addAction(UPDATE_ACTION);


            // If GPS, we know the label!
            if (startGPS) {
                mLabel.setText(extras.getString(StartFragment.ACTIVITY_TYPE));

            }

            // Only need activity recognition if automatic
            if (startAutomatic) {
                activityCount = new HashMap<>();
            }
        } else {
            mEntryID = extras.getLong(HistoryFragment.ENTRY_ID);
            mExerciseEntry = new MapActivity.DatabaseTaskLoad(this).loadInBackground();
            mSaveDeleteButton = findViewById(R.id.stopGPSButton);
            mSaveDeleteButton.setBackground(getDrawable(R.drawable.trash_icon_dynamic));
        }

    }


    /**
     * Resume runs each time the activity comes up, so we bind service here and register our receiver
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Register the receiver with the intent filter. Also bind your service once again
        if(!fromHistory) {
            doBindService();
            registerReceiver(mActivityUpdateReceiver, mUpdateFilter);
        }
    }

    /**
     * Pause runs each time activity is left, so we unbind service and unregister our receiver here
     */
    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the receiver with the intent filter. Also unbind your service once again
        if (!fromHistory) {
            doUnbindService();
            unregisterReceiver(mActivityUpdateReceiver);
        }
    }

    /**
     * Manipulates the map once available.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Grab the map
        mMap = googleMap;

        // If map is not null, then let us get a hybrid map going!
        if (mMap != null) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }

        // Do in onMapReady because we need the map!
        if(fromHistory) {
            showHistory();
        }
    }

    /**
     * Stops and unbinds the service. It also passes the correct intent with the action to destroy the ntoification.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop l i t e r a l l y everything. Services, broadcast receivers, unregsiter, ...
        stopAllServices();
        finish();
    }

    /**
     * When back is pressed, we want to stop all of the services and finish everything so that we start
     * a new service and broadcast receiver the next time we start the activity.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopAllServices();
        finish();
    }



    /// BINDING METHODS AND SERVICE CONNECTIONS ///

    /**
     * If our trackingService is running, bind our MapActivity to
     */
    private void automaticBind() {
        Log.d(BIND_TAG, "automaticBind()");
        // If our trackingService is running, then we want to bind our activity to the service
        if (TrackingService.isRunning) {
            doBindService();
        }
    }

    /**
     *  If our activity is not bound, we bind the activity to the service
     */
    private void doBindService() {
        Log.d(BIND_TAG, "doBindService()");

        // We pass in mConnection as this class to tell the service that it is this activity that
        // is trying to bind. Some flag info. If BIND_AUTO_CREATE is used, then it will bind and
        // start the service; if 0 is used, method will return true and will not start a service
        // until startService.

        if (!mIsBound) {
            bindService(this.mServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
            mIsBound = true;
        }
    }

    /**
     * If we are already bound, we detach our connection and unbind from the service
     */
    private void doUnbindService() {
        Log.d(BIND_TAG, "doUnbindService()");
        // Check if we are already bound
        if (mIsBound) {
            // Unbind and detach our connection
            unbindService(this.mConnection);
            mIsBound = false;
        }
    }

    /**
     * Connects the service before we can bind the activity to the service.
     * We cannot bind to the service and expect to get any information without connecting to service
     * @param name      Name of the Component
     * @param service   The service returned in the form of an IBinder when the Broadcast receives it
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(BIND_TAG, "onServiceConnected()");
        // Connect the trackingService here
        trackingService = ((TrackingService.TrackingServiceBinder) service).getService();

        // Now grab the entire exerciseEntry from the trackingService
        mExerciseEntry = trackingService.getmExerciseEntry();
    }

    /**
     * Disconnects the service and sets our service instance to null
     * @param name
     */
    @Override
    public void onServiceDisconnected(ComponentName name) {
        // Connection has been established, giving us the service object we can use to interact
        // with the service.
        Log.d(BIND_TAG, "onServiceDisconnected()");
        trackingService = null;
    }

    /**
     * Public class to create a Broadcast Receiver that decides what to do when we receive an update
     */
    public class ActivityUpdateReceiver extends BroadcastReceiver {
        // For location updates as well as activity updates

        /**
         * What we want to do when it receives an intent. Either a location update or an activity
         * update.
         * @param context   context to use
         * @param intent    intent coming from the trackingService or DetectedActivityIntentService
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (trackingService != null) {
                Bundle extras = intent.getExtras();
                if (extras.getInt(TrackingService.LOCATION_UPDATE) == 1){
                    Log.d("maria is on same page", "1 1 1");
                    locationUpdate(intent);
                }
                // If it is an activity update.
                else if (extras.getInt(TrackingService.LOCATION_UPDATE) == 2){
                    Log.d("maria is on same page", "2 2 2 ");
                    int type = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    handleUserActivity(type, confidence);
                }
            }
        }
    }


    /// HELPER METHODS ///

    /**
     * Helper method to find out what activity we are coming from and what activity we should be starting
     * @param extras
     */
    private void findOutActivities(Bundle extras) {
        if (extras != null) {
            if (extras.getBoolean(HistoryFragment.FROM_HISTORY)) {
                fromHistory = true;
                startGPS = false;
                startAutomatic = false;
            } else if (extras.getBoolean(StartFragment.FROM_AUTOMATIC)) {
                fromHistory = false;
                startGPS = false;
                startAutomatic = true;
            }
            else {
                fromHistory = false;
                startGPS = true;
                startAutomatic = false;
            }
        }
    }

    /**
     * Helper method to display the history version of the MapActivity if fromHistory is true!
     */
    private void showHistory() {
        // First draw trace on map
        ArrayList<LatLng> locations = mExerciseEntry.getmLocationList();

        // Markers
        mStartMarker = mMap.addMarker(new MarkerOptions().position(locations.get(0))
                .icon(getMarkerIcon("#5bbbd2")));
        mLiveMarker = mMap.addMarker(new MarkerOptions().position(locations.get(locations.size()-1))
                .icon(getMarkerIcon("#00303b")));

        // Draw the trace
        rectOptions = new PolylineOptions();
        rectOptions.color(Color.parseColor(String.valueOf("#00A5CB")));
        rectOptions.addAll(locations);
        polyline = mMap.addPolyline(rectOptions);

        // Zoom in
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locations.get(0), 17));


        // Now we update the views!
        DecimalFormat df = new DecimalFormat("###.##");
        if (MainActivity.units.equals("Miles")){
            // m/s to ft/s
            mAverageSpeed.setText(df.format(3.281 * mExerciseEntry.getmAvgSpeed()) + " ft/s");
            // km to mile
            mDistance.setText(df.format( mExerciseEntry.getmDistance()) + " m");
            // m to ft
            mElevation.setText(df.format(3.281 * mExerciseEntry.getmClimb()) +" ft");
            // m/s to ft/s
            mCurrSpeed.setText(df.format(3.281 * mExerciseEntry.getmAvgPace()) + " ft/s");
        } else {
            mAverageSpeed.setText(df.format(mExerciseEntry.getmAvgSpeed()) + " m/s");
            mDistance.setText(df.format(mExerciseEntry.getmDistance()) + " km");
            mCurrSpeed.setText(df.format(mExerciseEntry.getmAvgPace()) + " m/s");
            mElevation.setText(df.format(mExerciseEntry.getmClimb()) + " m");
        }
        mCalories.setText(df.format(mExerciseEntry.getmCalorie()));
        mLabel.setText(ManualActivity.idToActivity(mExerciseEntry.getmActivityType()));
        // Hope for the best?
    }

    /**
     * Map marker icon with custom color - courtesy of M a r i a :)
     * @param color     Color we want to change our marker
     * @return
     */
    public BitmapDescriptor getMarkerIcon(String color) {
        float[] hsv = new float[3];
        Color.colorToHSV(Color.parseColor(color), hsv);
        return BitmapDescriptorFactory.defaultMarker(hsv[0]);
    }


    /**
     * Saves the entry to the database and stops the services associated
     * @param v
     */
    public void onSaveClicked(View v) {
        stopAllServices();

        if(!fromHistory) {

            if (startAutomatic) {

                int max_count = 0;
                int unconverted_type = 1;
                int converted_type = 1;

                for (int key : activityCount.keySet()) {
                    if (activityCount.get(key) > max_count) {
                        unconverted_type = key;
                    }
                }

                switch (unconverted_type) {
                    case DetectedActivity.IN_VEHICLE: {
                        converted_type = 3;
                        break;
                    }
                    case DetectedActivity.ON_BICYCLE: {
                        converted_type = 1;
                        break;
                    }
                    case DetectedActivity.ON_FOOT: {
                        converted_type = 7;
                        break;
                    }
                    case DetectedActivity.RUNNING: {
                        converted_type = 9;
                        break;
                    }
                    case DetectedActivity.STILL: {
                        converted_type = 12;
                        break;
                    }
                    case DetectedActivity.TILTING: {
                        converted_type = 14;
                        break;
                    }
                    case DetectedActivity.WALKING: {
                        converted_type = 16;
                        break;
                    }
                    case DetectedActivity.UNKNOWN: {
                        converted_type = 15;
                        break;
                    }
                }

                mExerciseEntry.setmActivityType(converted_type);
                mExerciseEntry.setmInputType(0);
            } else {
                mExerciseEntry.setmActivityType(getIntent().getExtras().getInt(StartFragment.ACTIVITY_ID));
                mExerciseEntry.setmInputType(1);
            }
            new MapActivity.DatabaseTask().execute();
        } else {
            new DatabaseTaskDelete().execute();
        }
        finish();
    }

    /**
     * Helper method to stop l i t e r a l l y everything. So first check if our tracking service
     * is not null. If so, we write an intent to stop the notification, unbind the service, stop
     * the service.
     */
    private void stopAllServices() {
        if (!fromHistory) {
            if (trackingService != null) {
                // Broadcast intent to stop the notification!
                Intent stopIntent = new Intent();
                stopIntent.setAction(TrackingService.ACTION);
                stopIntent.putExtra(TrackingService.STOP_SERVICE_BROADCAST_KEY,
                        TrackingService.RQS_STOP_SERVICE);
                sendBroadcast(stopIntent);

                // Now unbind and stop the service
                doUnbindService();
                stopService(mServiceIntent);
                trackingService = null;
                Log.d(BIND_TAG, "stopAllServices()");
            }
        }
    }


    /**
     * A helper method to update the location.
     * @param intent
     */
    public void locationUpdate(Intent intent) {

        // We want to use the synchronized list, since work is being done by an IntentService
        // Which is basically just a service run on a worker thread.
        synchronized (mExerciseEntry.getmLocationList()) {
            // Check if it is the first point
            if (mIsFirst && mExerciseEntry.getmLocationList().size() == 1) {
                mIsFirst = false;
                mStartLocation = mExerciseEntry.getmLocationList().get(0);
                mLiveLocation = mExerciseEntry.getmLocationList()
                        .get(mExerciseEntry.getmLocationList().size() - 1);

                // Draw the first marker and the live marker
                mStartMarker = mMap.addMarker(new MarkerOptions().position(mStartLocation)
                    .icon(getMarkerIcon("#5bbbd2")));


                mLiveMarker = mMap.addMarker(new MarkerOptions().position(mLiveLocation)
                        .icon(getMarkerIcon("#00303b")));
                // Zoom in if not zoomed in, desired level 17
                Log.d(LOCATION_TAG, "First Time: " + mStartLocation + " " + mLiveLocation);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mStartLocation, 17));

                //grab the starting time
                mStartTime = new Date();
                mStartingAltitude = TrackingService.mAltitude;
            }
            else if (mExerciseEntry.getmLocationList().size() > 1) {

                // Now we update the live marker if our size is larger
                // But first lets check if we are missing the start marker
                if (mStartMarker == null) {
                    mStartLocation = mExerciseEntry.getmLocationList().get(0);
                    mStartMarker = mMap.addMarker(new MarkerOptions().position(mStartLocation)
                            .icon(getMarkerIcon("#5bbbd2")));
                }

                // Now we update the live marker
                mLiveLocation = mExerciseEntry.getmLocationList()
                        .get(mExerciseEntry.getmLocationList().size() - 1);

                mPrevLocation = mExerciseEntry.getmLocationList()
                        .get(mExerciseEntry.getmLocationList().size() - 2);
                if (mLiveMarker != null) {
                    mLiveMarker.remove();
                }
                Log.d(LOCATION_TAG, "First Time: " + mStartLocation + " " + mLiveLocation);
                mLiveMarker = mMap.addMarker(new MarkerOptions().position(mLiveLocation)
                        .icon(getMarkerIcon("#00303b")));

                // Now we update the polyline on the m a p!
                rectOptions = new PolylineOptions();
                rectOptions.color(Color.parseColor(String.valueOf("#00A5CB")));
                rectOptions.addAll(mExerciseEntry.getmLocationList());
                polyline = mMap.addPolyline(rectOptions);

                // Grab current time
                Date mDuration = new Date();

                // Calculate time in seconds since start
                long diff = Math.abs(mDuration.getTime() - mStartTime.getTime()) / 1000;

                //calculate distance between last two points
                float[] distance = new float[3];
                distance[0] = 0;
                Log.d("womp", mLiveLocation.toString());
                Location.distanceBetween(mPrevLocation.latitude, mPrevLocation.longitude, mLiveLocation.latitude, mLiveLocation.longitude, distance);
                Log.d("distance calc", Double.toString(distance[0]));

                //grab old distance, add the new amount, set the distance, store as km
                double newDistance = mExerciseEntry.getmDistance() + (distance[0] / 1000);
                mExerciseEntry.setmDistance(newDistance);

                Log.d("distance", Double.toString(mExerciseEntry.getmDistance()));
                DecimalFormat df = new DecimalFormat("###.##");

                //grab in km/s, convert to m/s
                Double avg_speed = (mExerciseEntry.getmDistance() * 1000)/diff;
                //meters per second
                mExerciseEntry.setmAvgPace(avg_speed);
                df.format(avg_speed);

                Double elevation_change = TrackingService.mAltitude - mStartingAltitude;
                //in meters
                mExerciseEntry.setmClimb(elevation_change);

                //grab distance in km, calculate calories
                mExerciseEntry.setmCalorie((int)(mExerciseEntry.getmDistance() * 60));

                // TODO: Once we are certain that distance works,
                if (MainActivity.units.equals("Miles")){
                    // m/s to ft/s
                    mAverageSpeed.setText(df.format(3.281 * mExerciseEntry.getmDistance()/diff) + " ft/s");
                    // km to mile
                    mDistance.setText(df.format((1./1.60934) * mExerciseEntry.getmDistance()) + " m");
                    // m to ft
                    mElevation.setText(df.format(3.281 * elevation_change) +" ft");
                    // m/s to ft/s
                    mCurrSpeed.setText(df.format(3.281 * TrackingService.mSpeed) + " ft/s");
                } else {
                    mAverageSpeed.setText(df.format(mExerciseEntry.getmDistance()/diff) + " m/s");
                    mDistance.setText(df.format(mExerciseEntry.getmDistance()) + " km");
                    mCurrSpeed.setText(df.format(TrackingService.mSpeed) + " m/s");
                    mElevation.setText(df.format(elevation_change) + " m");
                }
                mCalories.setText(df.format(mExerciseEntry.getmCalorie()));

                Log.d("speed", Double.toString(TrackingService.mSpeed));
                //meters per second
                mExerciseEntry.setmAvgSpeed(TrackingService.mSpeed);

                long minutes = TimeUnit.MILLISECONDS
                        .toMinutes(diff * 1000);
                mExerciseEntry.setmDuration((int)minutes);

            }
        }
    }

    /**
     * Changes up the activity type on the map display based on the intent service.
     * @param type          type of activity that the intentService returns
     * @param confidence    confidence of the activity type
     */
    private void handleUserActivity(int type, int confidence) {
        // Switch statement to change numerical type into string.
        String label = "Unknown";
        switch (type) {
            case DetectedActivity.IN_VEHICLE: {
                label = "Driving";
                break;
            }
            case DetectedActivity.ON_BICYCLE: {
                label = "Cycling";
                break;
            }
            case DetectedActivity.ON_FOOT: {
                label = "On Foot";
                break;
            }
            case DetectedActivity.RUNNING: {
                label = "Running";
                break;
            }
            case DetectedActivity.STILL: {
                label = "Standing";
                break;
            }
            case DetectedActivity.TILTING: {
                label = "Tilting";
                break;
            }
            case DetectedActivity.WALKING: {
                label = "Walking";
                break;
            }
            case DetectedActivity.UNKNOWN: {
                break;
            }
        }

        // Change the label of the activity!

        mLabel.setText(label);

        Log.d("womp womp", "broadcast:onReceive(): Activity is " + label
                + " and confidence level is: " + confidence);

        if (activityCount.containsKey(type)){
            activityCount.put(type, activityCount.get(type) + 1);
        } else {
            activityCount.put(type, 1);
        }
    }


    /// DATABASE STORAGE AND DELETION TASKS AND LOADERS

    /**
     * DatabaseTask is an AsyncTask that saves to our Database!
     */
    private class DatabaseTask extends AsyncTask<ExerciseEntryStructure, Void, Void> {

        @Override
        protected Void doInBackground(ExerciseEntryStructure... exerciseEntryStructures) {

            try {
                mExerciseEntry.setmID(mExerciseDataSource.insertExerciseEntry(mExerciseEntry));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // UPDATE HISTORY WHEN IT IS CREATED.
            HistoryFragment.mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * DatabaseTaskDelete is an AsyncTask that deletes from our database given an ID
     */
    private class DatabaseTaskDelete extends AsyncTask<ExerciseEntryStructure, Void, Void> {

        @Override
        protected Void doInBackground(ExerciseEntryStructure... exerciseEntryStructures) {
            Log.d("DELETE", "ID is" + mExerciseEntry.getmID());
            mExerciseDataSource.deleteExerciseEntry(mExerciseEntry);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // UPDATE HISTORY WHEN IT IS CREATED.
            HistoryFragment.mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * DatabaseTaskLoad is an AsyncTaskLoader that loads up an entry from the database for History
     * view
     */
    private class DatabaseTaskLoad extends AsyncTaskLoader<ExerciseEntryStructure> {

        public DatabaseTaskLoad(@NonNull Context context) {
            super(context);
        }

        @Nullable
        @Override
        public ExerciseEntryStructure loadInBackground() {
            try {
                return mExerciseDataSource.fetchEntry(mEntryID);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}