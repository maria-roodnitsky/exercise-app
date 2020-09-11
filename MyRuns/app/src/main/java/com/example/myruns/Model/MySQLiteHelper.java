package com.example.myruns.Model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class MySQLiteHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "exercise.db";
    public static final int DATABASE_VERSION = 1;


    public static final String TABLE_NAME = "exercises";
    public static final String KEY_COLUMN_ID = "_id";
    public static final String KEY_INPUT_TYPE = "input";
    public static final String KEY_ACTIVITY_TYPE = "activity";
    public static final String KEY_DATE_TIME = "date_time";
    public static final String KEY_DURATION = "duration";
    public static final String KEY_DISTANCE = "distance";
    public static final String KEY_AVG_PACE = "average_pace";
    public static final String KEY_AVG_SPEED = "average_speed";
    public static final String KEY_CALORIES = "calories";
    public static final String KEY_CLIMB = "climb";
    public static final String KEY_HEART_RATE = "heart_rate";
    public static final String KEY_COMMENT = "comment";
    public static final String KEY_GPS = "gps";

    public MySQLiteHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME
            + "("
            + KEY_COLUMN_ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + KEY_INPUT_TYPE
            + " INTEGER NOT NULL, "
            + KEY_ACTIVITY_TYPE
            + " INTEGER NOT NULL, "
            + KEY_DATE_TIME
            + " DATETIME NOT NULL, "
            + KEY_DURATION
            + " INTEGER NOT NULL, "
            + KEY_DISTANCE
            + " FLOAT, "
            + KEY_AVG_PACE
            + " FLOAT, "
            + KEY_AVG_SPEED
            + " FLOAT, "
            + KEY_CALORIES
            + " INTEGER, "
            + KEY_CLIMB
            + " FLOAT, "
            + KEY_HEART_RATE
            + " INTEGER, "
            + KEY_COMMENT
            + " TEXT, "
            + KEY_GPS
            + " BLOB "
            + ");";

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will clear all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }










}
