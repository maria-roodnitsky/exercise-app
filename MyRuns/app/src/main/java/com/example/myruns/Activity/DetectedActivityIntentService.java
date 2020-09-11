package com.example.myruns.Activity;

import android.app.IntentService;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

/** MYRUNS4: Detects different changes in the activity and sends a broadcaset back to the MapActivity.
 */
public class DetectedActivityIntentService extends IntentService {
    protected static final String TAG = DetectedActivityIntentService.class.getSimpleName();

    /**
     * Constructor for the DetectedActivityIntentService
     */
    public DetectedActivityIntentService() {
        super(TAG);
        Log.d("maria",TAG + "DetectedActivityIntentService()");
    }

    /**
     * Creates the IntentService
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Maria","onCreate()");

    }

    /**
     * What the intent should be handling. In this case it is grap
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG,TAG + "onHandleIntent()");
        Log.d("maria", "we got to handle intent");
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.

        List<DetectedActivity> detectedActivities = result.getProbableActivities();
        DetectedActivity mostLikelyActivity = detectedActivities.get(0);

        for (DetectedActivity activity : detectedActivities) {
            //Log.d(TAG, "Detected activity: " + activity.getType() + ", " + activity.getConfidence());
            if (activity.getConfidence() > mostLikelyActivity.getConfidence()){
                mostLikelyActivity = activity;
            }
        }
        broadcastActivity(mostLikelyActivity);
    }

    /**
     * Broadcast the activity updates to the map!
     * @param activity
     */
    private void broadcastActivity(DetectedActivity activity) {
        Log.d("maria","we got to broadcast activity");

        Intent intent = new Intent(MapActivity.UPDATE_ACTION);
        intent.putExtra(TrackingService.LOCATION_UPDATE, 2);
        intent.putExtra("type", activity.getType());
        intent.putExtra("confidence", activity.getConfidence());
        sendBroadcast(intent);

        Log.d("maria", Integer.toString(activity.getConfidence()));
    }
}
