package com.example.myruns.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import com.example.myruns.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.myruns.Model.ExerciseEntryStructure;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.Map;


/** MYRUNS4: TrackingService starts a service that updates location in the background and sends information
 * back to the UI during the activity being performed
 */
public class TrackingService extends Service {

    // Speed and altitude
    public static float mSpeed;
    public static double mAltitude;

    // Notification Information
    private NotifyServiceReceiver notifyServiceReceiver;
    final static String ACTION = "NotifyServiceAction";
    final static String STOP_SERVICE_BROADCAST_KEY = "StopServiceBroadcastKey";
    final static int RQS_STOP_SERVICE = 1;
    private static final String CHANNEL_ID = "MyRuns Notification Channel";

    // Broadcast Information
    public static final String LOCATION_UPDATE = "location";
    public static boolean isRunning = false;
    private TrackingServiceBinder trackingServiceBinder = new TrackingServiceBinder();

    // Activity Recognition
    private PendingIntent mPendingIntent;
    private ActivityRecognitionClient mActivityRecognitionClient;
    private static final long DETECTION_INTERVAL_IN_MILLISECONDS = 3000;

    // Location Manager
    private LocationManager locationManager;
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(LOCATION_TAG, "onLocationChanged");
            startLocationUpdates(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(LOCATION_TAG, "onStatusChanged");

        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(LOCATION_TAG, "onProviderEnabled");

        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(LOCATION_TAG, "onProviderDisabled");
        }
    };

    // TAGS
    private static final String LOCATION_TAG = "location";
    private static final String NOTIFY_TAG = "Notifications";
    private static final String TAG = "ServiceLifeCycle";
    String provider;

    // Exercise Data
    private ExerciseEntryStructure mExerciseEntry;


    /**
     * Defualt Constructor for TrackingService
     */
    public TrackingService() {
    }


    /// LIFECYCLE METHODS ///

    /**
     * Creates the trackingService :)
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TrackingService:onCreate");
        notifyServiceReceiver = new NotifyServiceReceiver();

        // Now our service is running
        isRunning = true;

        // Make our exercise
        initExerciseEntry();
    }


    /**
     * The command run on startup of our service. This registers the recieiver for notifications,
     * sets up the notification, starts up the locationManager, and begins locationFinding
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean forAutomatic = intent.getBooleanExtra(MapActivity.FOR_AUTOMATIC, false);


        // Log our thread.
        Log.d(NOTIFY_TAG, "NotifyService:onStartCommand");

        // Register our notifyService Receiver and set up notifications
        // We use an intentFilter to make sure we are looking for specific intents.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION);
        registerReceiver(notifyServiceReceiver, intentFilter);

        // Now that we have registered our broadcast receiver, need to send notification
        setUpNotification();

        // Sets up the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        if (forAutomatic) {
            mActivityRecognitionClient = new ActivityRecognitionClient(this);
            Intent mIntentService = new Intent(this, DetectedActivityIntentService.class);
            // FLAG_UPDATE_CURRENT indicates that if the described PendingIntent already exists,
            // then keep it but replace its extra data with what is in this new Intent.
            mPendingIntent = PendingIntent.getService(this,
                    1, mIntentService, PendingIntent.FLAG_UPDATE_CURRENT);
            requestActivityUpdatesHandler();
        }

        // Criteria for our location manager. This is because fine location may return a
        // null provider, which would mean we could not access our location with that provider.
        // So we set a criteria to make sure we always get a provider, even if it is not what we want.
        // Gps provider = more battery but accurate. Network is less battery but not great in country.
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        provider = locationManager.getBestProvider(criteria, true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Will always pass, but won't allow location to be found without checking.
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        // Get last known location to update with recent location
        Location location = locationManager.getLastKnownLocation(provider);
        LatLng latlng = locationToLatLng(location);


        // Help here from StackOverflow! Using the same idea of an intent service, just within a
        // service so that we get location updates quicker.
        Handler intentService = new Handler();
        intentService.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                Location location = locationManager.getLastKnownLocation(provider);
                startLocationUpdates(location);
                locationManager.requestLocationUpdates(provider, 2000, 0, locationListener);
            }
        }, 0);

        return START_STICKY;
    }

    /**
     * destroys the service, unregisters the notification broadcast receiver, stops the locationManager from getting updates
     */
    public void onDestroy() {
        super.onDestroy();
        removeActivityUpdatesHandler();
        unregisterReceiver(notifyServiceReceiver);
        stopSelf();
        locationManager.removeUpdates(locationListener);
        Log.d(TAG, "TrackingService:onDestroy()");
    }



    /// HELPER METHODS ///

    /**
     * Initializes the Exercise Entry
     */
    public void initExerciseEntry() {
        mExerciseEntry = new ExerciseEntryStructure(1,1); // Hardcoded for the time being
    }

    /**
     * Gets the ExerciseEntryStructure from the trackingService
     *
     * @return
     */
    public ExerciseEntryStructure getmExerciseEntry() {
        return mExerciseEntry;
    }

    /**
     * Sets up the entire notification that is run when MyRuns starts a GPS or Automatic Service
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setUpNotification() {

        Context context = getApplicationContext();
        String notificationTitle = "Workout in Progress";
        String notificationText = "If MyRuns can keep running, so can you!";
        // When you click on the notification, where do you want to go?
        Intent destinationIntent = new Intent(context, MapActivity.class);
        destinationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        destinationIntent.setAction(Intent.ACTION_MAIN);
        destinationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Now use a pendingIntent to wrap up the destinationIntent. Think of the notification as
        // its own miniApp (NotificationManager). It does not have the permissions to perform
        // actions like executing the destinationIntent. By wrapping in a pendingIntent, the miniApp
        // will be granted the same permissions as MyRuns.

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                destinationIntent, 0);

        // Now set up with pending Intent
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,
                "MyRuns Channel", NotificationManager.IMPORTANCE_HIGH);

        // Build the notification with compatibility
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle(notificationTitle).setContentText(notificationText)
                        .setSmallIcon(R.drawable.calories_icon).setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_MAX);

        Notification notification = notificationBuilder.build();

        // Setting up the flags. FLAG_AUTO_CANCEL says that the notification icon goes poof when
        // user taps the icon. Also display as ongoing event
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        // Now we get the NotificationManager from system services and add our notification
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(notificationChannel);
            notificationManager.notify(0, notification); // id 0 for our application notification
        }
    }


    /**
     * Used by the location listener to broadcast locations to the location update broadcast receiver
     * @param location
     */
    public void startLocationUpdates (Location location) {
        // Get locations and send it back to MapActivity for the broadcast reciever to pick it up
        // Check if valid location!
        if (location == null) {
            return;
        }

        synchronized (mExerciseEntry.getmLocationList()) {
            // Retrieve the latLng of the location, add it to our list of latLng.
            LatLng latLng = locationToLatLng(location);
            mExerciseEntry.setmAvgSpeed(location.getSpeed());
            mExerciseEntry.getmLocationList().add(latLng);
            mSpeed = location.getSpeed();
            mAltitude = location.getAltitude();



            // Send a broadcast
            Intent updateLocation = new Intent();

            updateLocation.setAction(MapActivity.UPDATE_ACTION);
            updateLocation.putExtra(LOCATION_UPDATE, 1);
            sendBroadcast(updateLocation);
            Log.d(TAG, "broadcast has been completed! Womp");
        }

    }

    /**
     * request updates and set up callbacks for success or failure
     */
    public void requestActivityUpdatesHandler() {
        Log.d(TAG, "requestActivityUpdatesHandler()");
        if(mActivityRecognitionClient != null){
            Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                    DETECTION_INTERVAL_IN_MILLISECONDS,
                    mPendingIntent);

            // Adds a listener that is called if the Task completes successfully.
            task.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Successfully requested activity updates");
                }
            });
            // Adds a listener that is called if the Task fails.
            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Requesting activity updates failed to start");
                }
            });
        }

    }

    /**
     * remove updates and set up callbacks for success or failure
     */
    public void removeActivityUpdatesHandler() {
        if (mActivityRecognitionClient != null) {
            Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(
                    mPendingIntent);
            // Adds a listener that is called if the Task completes successfully.
            task.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Removed activity updates successfully!");
                }
            });
            // Adds a listener that is called if the Task fails.
            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Failed to remove activity updates!");
                }
            });
        }
    }



    /**
     * Turns Location into a LatLng object for us to use.
     * @param location
     * @return
     */
    private LatLng locationToLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }


    /**
     *
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return this.trackingServiceBinder;
    }

    /**
     * Binder class used for the IBinder that is used to bind a class to this service
     */
    public class TrackingServiceBinder extends Binder {
        public TrackingService getService() {
            return TrackingService.this;
        }
    }



    /// BROADCAST RECEIVER INFORMATION///

    /**
     * This base class has the code that will recieve intents sent by sendBroadcast()
     * The receiver needs to be registered, and then specify an intent it listens for, that sis
     * the intent should specify the action that it listens for.
     */
    public class NotifyServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(NOTIFY_TAG, "NotifyService:onRecieve(): stop service " +  intent);
            NotificationManager notificationManager;
            // The NotificationManager class notifies the user that events happen. It can be an
            // icon on status bar, flashing lights, sounds, vibrations, etc. Here we get the system's
            // NotificationManager for informing background events and cancelAll on it.

            int stopService = intent.getIntExtra(STOP_SERVICE_BROADCAST_KEY, 0);
            if (stopService == RQS_STOP_SERVICE) {
                notificationManager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
                // check if null
                if (notificationManager != null) {
                    notificationManager.cancelAll();
                }
            }
            // Stop the service if it had been started. Recall that this is the same as calling
            // stopService(intent) for this service. stopSelf() will indeed call onDestroy()
            stopSelf();
        }
    }


    /// INTENT SERVICES ///

    /**
     * Intent Service to update Locations
     */
    public class LocationIntentService extends IntentService {
        public static final String LOCATION = "location";

        /**
         * Constructor for LocationIntentService
         */
        public LocationIntentService() {
            super("LocationIntentService");
        }

        @Override
        protected void onHandleIntent(@Nullable Intent intent) {
            if (intent != null) {
                String provider = intent.getExtras().getString(LOCATION);
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                locationManager.requestLocationUpdates(provider, 2000, 0, locationListener);
            }

        }
    }
}
