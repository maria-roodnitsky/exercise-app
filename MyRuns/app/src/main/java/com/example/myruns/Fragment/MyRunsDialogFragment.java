package com.example.myruns.Fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.myruns.Activity.MainActivity;
import com.example.myruns.Activity.ManualActivity;
import com.example.myruns.Activity.ProfileActivity;
import com.example.myruns.R;

/**
 * MYRUNS2: A MyRunsDialogFragment extends DialogFragment and allows for different types of Dialog
 * fragments to be displayed on the screen.
 */
public class MyRunsDialogFragment extends DialogFragment {
    // Types of MyRunDialogFragments
    public static final String TYPE_PHOTO = "photo";
    public static final String TYPE_DURATION = "duration";
    public static final String TYPE_DISTANCE = "distance";
    public static final String TYPE_CALORIES = "calories";
    public static final String TYPE_HEARTBEAT = "heartbeat";
    public static final String TYPE_COMMENT = "comments";

    // Different IDs
    private static final String DIALOG_ID = "dialogid";
    public static final int TAKE_PHOTO = 0;
    public static final int CHOOSE_PHOTO = 1;



    public MyRunsDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param id DialogID
     * @return A new instance of fragment MyRunsDialogFragment.
     */
    public static MyRunsDialogFragment newInstance(String id) {
        MyRunsDialogFragment fragment = new MyRunsDialogFragment();
        Bundle args = new Bundle();
        args.putString(DIALOG_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates the different types of dialogs when it is called.
     * @param savedInstanceState
     * @return
     */
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Retrieve the the ID and create the alert dialog that will be used
        String dialogID = getArguments().getString(DIALOG_ID);
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);

        // EditText to hold the text that will be stored.
        final EditText manualInput = new EditText(getActivity());
        manualInput.setTextColor(Color.WHITE);
        manualInput.setGravity(Gravity.CENTER_HORIZONTAL);

        // "Switch" Statement for the different types of dialogs
        if (dialogID.equals(TYPE_PHOTO)) {
            // Builds alert dialog
            alert.setTitle(R.string.profile_photo);

            // OnClick Listeners for choosing from gallery and for taking photo
            DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((ProfileActivity) getActivity()).takeProfilePhoto(which);
                }
            };
            // Items to display
            String[] photo_options = new String[]{"Take photo from camera", "Choose photo from gallery"};
            alert.setItems(photo_options, dialogListener);
            return alert.create();
        }
        // Builds dialog for manual entries :)
        // duration, distance, calories, heartbeat, comments dialogs
        else if (dialogID.equals(TYPE_DURATION)) {
            return createDialog(alert, dialogID, getString(R.string.duration), manualInput);
        }
        else if (dialogID.equals(TYPE_DISTANCE)) {
            return createDialog(alert, dialogID, getString(R.string.distance), manualInput);
        }
        else if (dialogID.equals(TYPE_CALORIES)) {
            return createDialog(alert, dialogID, getString(R.string.calories), manualInput);
        }
        else if (dialogID.equals(TYPE_HEARTBEAT)) {
            return createDialog(alert, dialogID, getString(R.string.heartbeat), manualInput);
        }
        else if (dialogID.equals(TYPE_COMMENT)) {
            return createDialog(alert, dialogID, getString(R.string.comment), manualInput);
        }
        return null;
    }


    /**
     * Handles the creation of of the dialog fragment for the extra fields.
     * @param alert
     * @param dialogID
     * @param title
     * @param manualInput
     * @return
     */
    public Dialog createDialog(AlertDialog.Builder alert, final String dialogID, String title, final EditText manualInput) {
        // Sets title
        alert.setTitle(title);

        // Set input type for the different types of fields
        if(dialogID.equals(TYPE_COMMENT)) {
            manualInput.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        else {
            manualInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }

        // set the EditText as input
        alert.setView(manualInput);

        // If the OK button is clicked!
        alert.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialogID.equals(TYPE_DURATION)) {
                    ((ManualActivity)getActivity()).mItems.get(3).setData(manualInput.getText().toString() + " mins");
                    try {
                        ((ManualActivity) getActivity()).mExerciseEntry.setmDuration(Integer.parseInt(manualInput.getText().toString()));
                    } catch (Exception e) {
                        ((ManualActivity) getActivity()).mExerciseEntry.setmDuration(0);
                    }
                }
                else if(dialogID.equals(TYPE_DISTANCE)) {
                    // check for unit
                    ((ManualActivity)getActivity()).mItems.get(4).setData(manualInput.getText().toString() + " " + MainActivity.units);
                    try {
                        ((ManualActivity)getActivity()).mExerciseEntry.setmDistance(Integer.parseInt(manualInput.getText().toString()));
                    } catch (Exception e) {
                        ((ManualActivity)getActivity()).mExerciseEntry.setmDistance(0);
                    }
                }
                else if(dialogID.equals(TYPE_CALORIES)) {
                    ((ManualActivity)getActivity()).mItems.get(5).setData(manualInput.getText().toString() + " calories");
                    try {
                        ((ManualActivity)getActivity()).mExerciseEntry.setmCalorie(Integer.parseInt(manualInput.getText().toString()));
                    } catch (Exception e) {
                        ((ManualActivity)getActivity()).mExerciseEntry.setmCalorie(0);
                    }
                }
                else if(dialogID.equals(TYPE_HEARTBEAT)) {
                    ((ManualActivity)getActivity()).mItems.get(6).setData(manualInput.getText().toString() + " bpm");
                    try {
                        ((ManualActivity)getActivity()).mExerciseEntry.setmHeartRate(Integer.parseInt(manualInput.getText().toString()));
                    } catch (Exception e) {
                        ((ManualActivity)getActivity()).mExerciseEntry.setmHeartRate(0);
                    }
                }
                else if(dialogID.equals(TYPE_COMMENT)) {
                    ((ManualActivity)getActivity()).mItems.get(7).setData(manualInput.getText().toString());
                    ((ManualActivity)getActivity()).mExerciseEntry.setmComment(manualInput.getText().toString());
                }
            }
        });
        // Negative Button
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                manualInput.setText("");
            }
        });
        // return created fragment alert dialog.
        return alert.create();
    }
}