package com.example.myruns.Model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.myruns.Activity.MainActivity;
import com.example.myruns.Activity.ManualActivity;
import com.google.android.gms.maps.model.LatLng;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Array;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class ExerciseDataSource {
    // database helper
    MySQLiteHelper databaseHelper;
    SQLiteDatabase database;

    public static final String TABLE_NAME = "exercises";
    public static final String KEY_COLUMN_ID = "_id";

    public ExerciseDataSource(Context context) {
        databaseHelper = new MySQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = databaseHelper.getWritableDatabase();
    }

    public synchronized long insertExerciseEntry(ExerciseEntryStructure exerciseEntry) throws JSONException {
        ContentValues contentValues = new ContentValues();

        contentValues.put(MySQLiteHelper.KEY_INPUT_TYPE, exerciseEntry.getmInputType());        // 1
        contentValues.put(MySQLiteHelper.KEY_ACTIVITY_TYPE, exerciseEntry.getmActivityType());  // 2
        contentValues.put(MySQLiteHelper.KEY_DATE_TIME, exerciseEntry.getmDateTime());          // 3
        contentValues.put(MySQLiteHelper.KEY_DURATION, exerciseEntry.getmDuration());           // 4
        contentValues.put(MySQLiteHelper.KEY_DISTANCE, exerciseEntry.getmDistance());           // 5
        contentValues.put(MySQLiteHelper.KEY_AVG_PACE, exerciseEntry.getmAvgPace());            // 6
        contentValues.put(MySQLiteHelper.KEY_AVG_SPEED, exerciseEntry.getmAvgSpeed());          // 7
        contentValues.put(MySQLiteHelper.KEY_CALORIES, exerciseEntry.getmCalorie());            // 8
        contentValues.put(MySQLiteHelper.KEY_CLIMB, exerciseEntry.getmClimb());                 // 9
        contentValues.put(MySQLiteHelper.KEY_HEART_RATE, exerciseEntry.getmHeartRate());        // 10
        contentValues.put(MySQLiteHelper.KEY_COMMENT, exerciseEntry.getmComment());             // 11
        contentValues.put(MySQLiteHelper.KEY_GPS, LocationListToJSON(exerciseEntry.getmLocationList())); //12
        long insertId = database.insert(MySQLiteHelper.TABLE_NAME, null, contentValues);

        return insertId;
    }

    public String LocationListToJSON(ArrayList<LatLng> LocationList) throws JSONException {
        JSONObject json = new JSONObject();

        JSONArray array = new JSONArray();
        for (LatLng location: LocationList){
            JSONObject latlngasjson = new JSONObject();
            latlngasjson.put("lat", location.latitude);
            latlngasjson.put("lng", location.longitude);
            array.put(latlngasjson);
        }

        json.put("latlnglist", array);
        return json.toString();
    }


    public synchronized void deleteExerciseEntry(ExerciseEntryStructure ExerciseEntry){
        long id = ExerciseEntry.getmID();
        Log.d("delete", "Entry deleted with ID: " + id);
        database.delete(MySQLiteHelper.TABLE_NAME, MySQLiteHelper.KEY_COLUMN_ID
                + " = " + id, null);
    }

    public synchronized void deleteAllEntries(){
        Log.d("delete", "Literally e v e r y t h i n g was just deleted.");
        database.delete(MySQLiteHelper.TABLE_NAME, null, null);
    }

    public synchronized ExerciseEntryStructure fetchEntry(long entryID) throws JSONException {
        database = databaseHelper.getReadableDatabase();

        Cursor cursor = database.query(MySQLiteHelper.TABLE_NAME,
                null,
                KEY_COLUMN_ID + " = " + entryID,
                null,
                null,
                null,
                null);
        cursor.moveToFirst();
        ExerciseEntryStructure exerciseEntry = cursorToExerciseEntry(cursor);
        cursor.close();
        return exerciseEntry;
    }

    public synchronized ArrayList<ExerciseEntryStructure> fetchAllEntries() throws JSONException {
        database = databaseHelper.getReadableDatabase();

        ArrayList<ExerciseEntryStructure> exerciseEntries = new ArrayList<ExerciseEntryStructure>();
        Cursor cursor = database.query(MySQLiteHelper.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null);

        cursor.moveToFirst();

        // Fetch all now
        while (!cursor.isAfterLast()) {
            ExerciseEntryStructure exerciseEntry = cursorToExerciseEntry(cursor);
            exerciseEntries.add(exerciseEntry);
            cursor.moveToNext();
        }
        // Return our list
        return exerciseEntries;
    }

    public synchronized ExerciseEntryStructure cursorToExerciseEntry(Cursor cursor) throws JSONException {
        ExerciseEntryStructure exerciseEntry = new ExerciseEntryStructure(cursor.getInt(1), cursor.getInt(2));
        exerciseEntry.setmDateTime(cursor.getLong(3));
        exerciseEntry.setmDuration(cursor.getInt(4));
        // converts to miles if needed and round to nearest thousandth, seeing as
        // db stores all distance as km
        DecimalFormat twoDForm = new DecimalFormat("#.###");
        if (MainActivity.units.equals("Miles")){
            exerciseEntry.setmDistance(Double.parseDouble(twoDForm.format(cursor.getDouble(5) * 0.621)));
        } else {
            exerciseEntry.setmDistance(Double.parseDouble(twoDForm.format(cursor.getDouble(5))));
        }
        exerciseEntry.setmAvgPace(cursor.getDouble(6));
        exerciseEntry.setmAvgSpeed(cursor.getDouble(7));
        exerciseEntry.setmCalorie(cursor.getInt(8));
        exerciseEntry.setmClimb(cursor.getDouble(9));
        exerciseEntry.setmHeartRate(cursor.getInt(10));
        exerciseEntry.setmComment(cursor.getString(11));
        exerciseEntry.setmID(cursor.getLong(0));
        exerciseEntry.setmLocationList(StringtoArrayList(cursor.getString(12)));
        return exerciseEntry;
    }


    public ArrayList<LatLng> StringtoArrayList(String JSONString) throws JSONException {

        ArrayList<LatLng> LocationList = new ArrayList<LatLng>();

            if (JSONString == null) return LocationList;
            JSONObject obj = new JSONObject(JSONString);

            JSONArray array = obj.getJSONArray("latlnglist");


            for (int i = 0; i < array.length(); i++) {
                double lat = array.getJSONObject(i).getDouble("lat");
                double lng = array.getJSONObject(i).getDouble("lng");
                LatLng location = new LatLng(lat, lng);
                LocationList.add(location);
                Log.d("mrp mrp oh my god", Double.toString(lat) + " " + Double.toString(lng));
            }
        return LocationList;
    }

}
