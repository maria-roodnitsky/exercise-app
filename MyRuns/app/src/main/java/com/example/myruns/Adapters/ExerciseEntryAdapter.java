package com.example.myruns.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;


import com.example.myruns.Fragment.HistoryFragment;
import com.example.myruns.Model.ExerciseDataSource;
import com.example.myruns.Activity.MainActivity;

import com.example.myruns.Model.ExerciseEntryStructure;
import com.example.myruns.Model.ManualEntryStructure;
import com.example.myruns.R;

import org.json.JSONException;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class ExerciseEntryAdapter extends ArrayAdapter<ExerciseEntryStructure> {
    // private variables for context and item list
    private ArrayList<ExerciseEntryStructure> mItems;
    private Context context;
    private ExerciseDataSource mExerciseDataSource;

    /**
     * Constructor for a manualEntryAdapter that extends an array adapter.
     * @param context context
     * @param resource resource
     * @param objects   object list
     */
    public ExerciseEntryAdapter(@NonNull Context context, int resource, @NonNull ArrayList<ExerciseEntryStructure> objects) throws SQLException {
        super(context, resource, objects);
        this.mItems = objects;
        this.context = context;
        mExerciseDataSource = new ExerciseDataSource(getContext());
        mExerciseDataSource.open();
    }

    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        ExerciseEntryStructure entry = mItems.get(position);

        View listItem = convertView;

        convertView = LayoutInflater.from(context).inflate(R.layout.log_entry, parent, false);
        // Get and set the interior text views
        TextView title = convertView.findViewById(R.id.log_title);
        TextView distance = convertView.findViewById(R.id.log_distance);
        TextView duration = convertView.findViewById(R.id.log_duration);
        TextView date = convertView.findViewById(R.id.log_date);
        TextView time = convertView.findViewById(R.id.log_time);
        TextView id = convertView.findViewById(R.id.log_id);


        String converted_date = new SimpleDateFormat("MM/dd/yyyy").format(new Date(entry.getmDateTime()));
        String converted_time = new SimpleDateFormat("h:mm a").format(new Date(entry.getmDateTime()));

        String[] stringArray = context.getResources().getStringArray(R.array.input_activity_list);

        title.setText(stringArray[entry.getmActivityType()]);
        distance.setText(Double.toString(entry.getmDistance()) + " " + MainActivity.units);
        duration.setText(Double.toString(entry.getmDuration()) + " Minutes");
        date.setText(converted_date);
        time.setText(converted_time);
        id.setText(Long.toString(entry.getmID()));

        return convertView;
    }
    public ArrayList<ExerciseEntryStructure> getmItems() {
        return mItems;
    }

    @Override
    public void notifyDataSetChanged() {
        if (mExerciseDataSource != null) {
            ArrayList<ExerciseEntryStructure> list = null;
            try {
                list = mExerciseDataSource.fetchAllEntries();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            ExerciseEntryAdapter adapter = null;
            try {
                adapter = new ExerciseEntryAdapter(getContext(), 0, list);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            HistoryFragment.listView.setAdapter(adapter);
            HistoryFragment.list = new HistoryFragment.HistoryTabLoader(getContext()).loadInBackground();
        }
        super.notifyDataSetChanged();
    }
}
