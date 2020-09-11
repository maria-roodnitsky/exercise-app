package com.example.myruns.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;

import com.example.myruns.Activity.MainActivity;
import com.example.myruns.Activity.ManualActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.myruns.Activity.MapActivity;
import com.example.myruns.Adapters.ExerciseEntryAdapter;
import com.example.myruns.Model.ExerciseDataSource;
import com.example.myruns.Model.ExerciseEntryStructure;
import com.example.myruns.R;

import org.json.JSONException;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;


/** MYRUNS4: Displays the different activities that have been stored into the database. When an entry
 * is clicked, we go to that respective history view in displayEntry or MapActivity.
 */
public class HistoryFragment extends Fragment {

    public static final int HISTORY_REQUEST = 0;
    public static final String ENTRY_ID = "entryID";
    public static final String FROM_HISTORY = "fromHistory";
    public static final String IDTAG = "id";
    public static ListView listView;
    public static ExerciseEntryAdapter mAdapter;
    public static ArrayList<ExerciseEntryStructure> list;
    public static ExerciseDataSource mExerciseDataSource;

    // Generates a new instance
    public static HistoryFragment newInstance(String param1, String param2) {
        HistoryFragment fragment = new HistoryFragment();
        return fragment;
    }
    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instantiate and open up our database
        mExerciseDataSource = new ExerciseDataSource(getContext());
        try {
            mExerciseDataSource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        list = new HistoryTabLoader(getContext()).loadInBackground();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_history, container, false);
        listView = (ListView) rootView.findViewById(R.id.entries_list);
        try {
            mAdapter = new ExerciseEntryAdapter(getContext(), 0, list);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        listView.setAdapter(mAdapter);

        AdapterView.OnItemClickListener mOnExerciseClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //list = mAdapter.getmItems();
                long itemID = list.get(position).getmID();
                int inputType = list.get(position).getmInputType();

                Log.d(IDTAG, inputType + "");
                Intent intent;
                if(inputType == 2) {
                    intent = new Intent(getContext(), ManualActivity.class);
                } else {
                    intent = new Intent(getContext(), MapActivity.class);
                }

                intent.putExtra(ENTRY_ID, itemID);
                intent.putExtra(FROM_HISTORY, true);
                startActivityForResult(intent, HISTORY_REQUEST);
            }
        };

        listView.setOnItemClickListener(mOnExerciseClickListener);

        return rootView;
    }

    public static class HistoryTabLoader extends AsyncTaskLoader<ArrayList<ExerciseEntryStructure>> {

        public HistoryTabLoader(@NonNull Context context) {
            super(context);
        }

        @Nullable
        @Override
        public ArrayList<ExerciseEntryStructure> loadInBackground() {
            try {
                return mExerciseDataSource.fetchAllEntries();
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

    }
}
