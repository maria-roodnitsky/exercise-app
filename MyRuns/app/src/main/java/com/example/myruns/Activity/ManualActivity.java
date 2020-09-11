package com.example.myruns.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.AsyncTaskLoader;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TimePicker;
import com.example.myruns.Adapters.ManualEntryAdapter;
import com.example.myruns.Fragment.HistoryFragment;
import com.example.myruns.Model.ExerciseDataSource;
import com.example.myruns.Model.ExerciseEntryStructure;
import com.example.myruns.Model.ManualEntryStructure;
import com.example.myruns.Fragment.MyRunsDialogFragment;
import com.example.myruns.R;
import com.example.myruns.Fragment.StartFragment;
import org.json.JSONException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** MYRUNS2: MainActivity allows scrolling between two pages, the start and history fragments
 */

public class ManualActivity extends AppCompatActivity {
    // Constants for switch case onclick listener
    private static final int DATE = 1;
    private static final int TIME = 2;
    private static final int DURATION = 3;
    private static final int DISTANCE = 4;
    private static final int CALORIES = 5;
    private static final int HEARTBEAT = 6;
    private static final int COMMENT = 7;
    private static final int SAVE = 8;

    // Declaring of the listview of the manual activity, it's adapter and arraylist
    private ListView mListView;
    public ArrayList<ManualEntryStructure> mItems;
    private ManualEntryAdapter mManualInputAdapter;
    public ExerciseEntryStructure mExerciseEntry;
    public long mEntryID;
    public ExerciseDataSource mExerciseDataSource;


    // Calendar for the Date and Time
    private Calendar cal;

    // DisplayEntry instead of Manual Entry
    private boolean forHistory = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);
        getSupportActionBar().setTitle(R.string.LOG_MANUAL);



        // Intent extras
        Bundle extras = getIntent().getExtras();


        // Things that can be done regardless of the type of activity
        // Opening the dataSource
        mExerciseDataSource = new ExerciseDataSource(this);
        try {
            mExerciseDataSource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (extras.getBoolean(HistoryFragment.FROM_HISTORY, false)) {
            forHistory = true;
        }
        else{
            forHistory = false;
        }
        Log.d("ONACTIVITY", forHistory + "");

        // set up the list view
        mListView = findViewById(R.id.listView);

        // If this is not the first instance, load the data
        if (savedInstanceState != null){
            mItems = savedInstanceState.getParcelableArrayList("datalist");

        } else {
            mItems = new ArrayList<ManualEntryStructure>();
        }
        // Custom adapter
        mManualInputAdapter = new ManualEntryAdapter(this, R.layout.manual_entry_structure, mItems);
        // Bind the adapter to the listView and initialize the fields
        mListView.setAdapter(mManualInputAdapter);

        if (forHistory) {
            getSupportActionBar().setTitle(R.string.EXERCISE);
            mEntryID = extras.getLong(HistoryFragment.ENTRY_ID);
            Log.d("ENTRYID", extras.getLong(HistoryFragment.ENTRY_ID) + "");
            mExerciseEntry = new DatabaseTaskLoad(this).loadInBackground();

            if (savedInstanceState == null) {
                initializeFields(mExerciseEntry);
            }
        }
        else {
            getSupportActionBar().setTitle(R.string.LOG_MANUAL);

            //Set up Calendar
            cal = Calendar.getInstance();
            // Store in the data structure to store into database later on and database
            mExerciseEntry = new ExerciseEntryStructure(0, extras.getInt(StartFragment.ACTIVITY_ID));

            if (savedInstanceState == null) {
                initializeFields();
            }

            // Create an onItemClickListener for the list view: Date, time, duration, distance...
            AdapterView.OnItemClickListener mItemListener = new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    switch (position) {
                        case DATE:
                            manualDate(mListView);
                            break;
                        case TIME:
                            manualTime(mListView);
                            break;
                        case DURATION:
                            manualDuration();
                            break;
                        case DISTANCE:
                            manualDistance();
                            break;
                        case CALORIES:
                            manualCalories();
                            break;
                        case HEARTBEAT:
                            manualHeartbeat();
                            break;
                        case COMMENT:
                            manualComments();
                            break;
                        case SAVE:
                            try {
                                manualSave();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                    }
                }
            };
            // bind the view to the onClickListeners
            mListView.setOnItemClickListener(mItemListener);
        }

    }

    /* Save the array of objects in saved instances */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("datalist", mItems);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // For display, we want a delete button, otherwise we want a save button
        if (forHistory) {
            getMenuInflater().inflate(R.menu.display_menu, menu);
        }
        else {
            getMenuInflater().inflate(R.menu.manual_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {


        int menuID = item.getItemId();

        if (menuID == R.id.save_btn) {
            Log.d("save", Double.toString(mExerciseEntry.getmDistance()));
            mExerciseEntry.setmInputType(2);
            if (MainActivity.units.equals("Miles")){
                double converted = mExerciseEntry.getmDistance() * 1.60934;
                mExerciseEntry.setmDistance(converted);
                Log.d("Added miles as kms", Double.toString(converted));
            }
            new DatabaseTask().execute();
            finish();
            Log.d("DATABASE", "inserted");
        }
        else if (menuID == R.id.delete_btn) {
            Log.d("DELETE", "ID is " + mExerciseEntry.getmID());
            new DatabaseTaskDelete().execute();
            finish();
            Log.d("DATABASE", "deleted");

        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Initializes fields before the user decides to c h a n g e it u p.
     */
    public void initializeFields() {
        mItems.add(new ManualEntryStructure("Activity", getIntent().getStringExtra(StartFragment.ACTIVITY_TYPE)));
        mItems.add(new ManualEntryStructure("Date", "today"));
        mItems.add(new ManualEntryStructure("Time", "now"));
        mItems.add(new ManualEntryStructure("Duration", "0 mins"));
        mItems.add(new ManualEntryStructure("Distance", "0" + " " + MainActivity.units));
        mItems.add(new ManualEntryStructure("Calories", "0 calories"));
        mItems.add(new ManualEntryStructure("Heartbeat", "0 bpm"));
        mItems.add(new ManualEntryStructure("Comment", ""));
    }

    public void initializeFields(ExerciseEntryStructure exerciseEntry) {
        mItems.add(new ManualEntryStructure("Activity", idToActivity(exerciseEntry.getmActivityType())));
        mItems.add(new ManualEntryStructure("Date", retrieveDate(exerciseEntry.getmDateTime())));
        mItems.add(new ManualEntryStructure("Time", retrieveTime(exerciseEntry.getmDateTime())));
        mItems.add(new ManualEntryStructure("Duration", retrieveDuration(exerciseEntry.getmDuration())));
        mItems.add(new ManualEntryStructure("Distance", retrieveDistance(exerciseEntry.getmDistance())));
        mItems.add(new ManualEntryStructure("Calories", retrieveCalories(exerciseEntry.getmCalorie())));
        mItems.add(new ManualEntryStructure("Heartbeat", retrieveHeartbeat(exerciseEntry.getmHeartRate())));
        mItems.add(new ManualEntryStructure("Comment", exerciseEntry.getmComment()));
    }

    public static String idToActivity(int id) {
        switch (id) {
            case 0:
                return "Cross-Country Skiing";
            case 1:
                return "Cycling";
            case 2:
                return "Downhill Skiing";
            case 3:
                return "Driving";
            case 4:
                return "Elliptical";
            case 5:
                return "Hiking";
            case 6:
                return "Mountain Biking";
            case 7:
                return "On Foot";
            case 8:
                return "Other";
            case 9:
                return "Running";
            case 10:
                return "Skating";
            case 11:
                return "Snowboarding";
            case 12:
                return "Standing";
            case 13:
                return "Swimming";
            case 14:
                return "Tilting";
            case 15:
                return "Unknown";
            case 16:
                return "Walking";
            case 17:
                return "Wheelchair";
            default:
                return "";
        }
    }

    /**
     * Formats date into correct date string
     * @param dateTime
     * @return
     */
    private String retrieveDate(long dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.US);
        return sdf.format(new Date(dateTime));
    }

    /**
     * Helper to format time into correct time string
     * @param dateTime
     * @return
     */
    private String retrieveTime(long dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm", Locale.US);
        return sdf.format(new Date(dateTime));
    }

    /**
     * Retrueves string duration
     * @param duration
     * @return
     */
    private String retrieveDuration(double duration) {
        return duration + " mins";
    }

    /**
     * Retrieves distance String
     * @param distance
     * @return
     */
    private String retrieveDistance(double distance) {
        return distance + " " + MainActivity.units;
    }

    /**
     * Retrieves calories string
     * @param calories
     * @return
     */
    private String retrieveCalories(int calories) {
        return calories + " calories";
    }

    /**
     * Retrieves heartbeat string
     * @param heartbeat
     * @return
     */
    private String retrieveHeartbeat(int heartbeat) {
        return heartbeat + " BPM";
    }



    /** Methods to help edit fields! **/

    /**
     * Manual Date editing
     * @param v
     */
    public void manualDate(View v) {

        // Creates a Date Picker Dialog, adapted and inspired by Campbell.

        DatePickerDialog.OnDateSetListener activityDatePicker = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month);
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.US);
                mItems.get(1).setData(sdf.format(cal.getTime()));
                mExerciseEntry.setmDateTime(cal.getTimeInMillis());
                mManualInputAdapter.notifyDataSetChanged();
            }
        };
        new DatePickerDialog(ManualActivity.this, activityDatePicker, cal
                .get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Manual Time editing
     * @param v
     */
    public void manualTime(View v) {

        // Creates a Time Picker Dialog, adapted from Campbell

        TimePickerDialog activityTimePicker = new TimePickerDialog(ManualActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                String min = "";
                if(minute < 10)
                    min = "0" + minute;
                else
                    min = "" + minute;
                mItems.get(2).setData(hourOfDay + ":" + min);
                mExerciseEntry.setmDateTime(cal.getTimeInMillis());
                mManualInputAdapter.notifyDataSetChanged();
            }
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
        activityTimePicker.setTitle("Time Started");
        activityTimePicker.show();
    }

    /**
     *  Changes the duration manually with a MyRunsDialogFragment
     */
    public void manualDuration() {
        MyRunsDialogFragment durationFragment = MyRunsDialogFragment.newInstance(MyRunsDialogFragment.TYPE_DURATION);
        durationFragment.show(getSupportFragmentManager(), getString(R.string.duration));
    }

    /**
     *  Changes the distance manually with a MyRunsDialogFragment
     */
    public void manualDistance() {
        MyRunsDialogFragment distanceFragment = MyRunsDialogFragment.newInstance(MyRunsDialogFragment.TYPE_DISTANCE);
        distanceFragment.show(getSupportFragmentManager(), getString(R.string.distance));
    }

    /**
     *  Changes the calories manually with a MyRunsDialogFragment
     */
    public void manualCalories() {
        MyRunsDialogFragment caloriesFragment = MyRunsDialogFragment.newInstance(MyRunsDialogFragment.TYPE_CALORIES);
        caloriesFragment.show(getSupportFragmentManager(), getString(R.string.calories));
    }

    /**
     *  Changes the heartbeat manually with a MyRunsDialogFragment. <3 I l o v e working with M a r i a
     */
    public void manualHeartbeat() {
        MyRunsDialogFragment heartbeatFragment = MyRunsDialogFragment.newInstance(MyRunsDialogFragment.TYPE_HEARTBEAT);
        heartbeatFragment.show(getSupportFragmentManager(), getString(R.string.heartbeat));
    }

    /**
     *  Changes the comments manually with a MyRunsDialogFragment
     */
    public void manualComments() {
        MyRunsDialogFragment commentsFragment = MyRunsDialogFragment.newInstance(MyRunsDialogFragment.TYPE_COMMENT);
        commentsFragment.show(getSupportFragmentManager(), getString(R.string.comment));
    }

    /**
     * Saves the the data into the database
     */
    public void manualSave() throws SQLException {
        ExerciseDataSource datasource = new ExerciseDataSource(this);
        datasource.open();
    }

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

    // To delete an entry!
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

    private class DatabaseTaskLoad extends AsyncTaskLoader<ExerciseEntryStructure>{

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