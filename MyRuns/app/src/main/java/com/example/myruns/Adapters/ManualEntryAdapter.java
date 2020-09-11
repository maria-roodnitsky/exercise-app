package com.example.myruns.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myruns.Model.ManualEntryStructure;
import com.example.myruns.R;

import java.util.ArrayList;

/** MYRUNS2: A ManualEntryAdapter converts the corresponding object into a workable view.
 */

public class ManualEntryAdapter extends ArrayAdapter<ManualEntryStructure> {
    // private variables for context and item list
    private ArrayList<ManualEntryStructure> mItems;
    private Context context;

    /**
     * Constructor for a manualEntryAdapter that extends an array adapter.
     * @param context context
     * @param resource resource
     * @param objects   object list
     */
    public ManualEntryAdapter(@NonNull Context context, int resource, @NonNull ArrayList<ManualEntryStructure> objects) {
        super(context, resource, objects);
        this.mItems = objects;
        this.context = context;
    }

    /**
     * getView overrides the normal getView to inflate our own view.
     * @param position position in the current list
     * @param convertView view that we want to inflate with
     * @param parent parent activity
     * @return
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        // Get the data item
        ManualEntryStructure entry = mItems.get(position);

        convertView = LayoutInflater.from(context).inflate(R.layout.manual_entry_structure, parent, false);

        // Get and set the interior text views
        TextView title = convertView.findViewById(R.id.titleView);
        TextView data = convertView.findViewById(R.id.dataView);
        title.setText(entry.getTitle());
        data.setText(entry.getData());

        return convertView;
    }

}

