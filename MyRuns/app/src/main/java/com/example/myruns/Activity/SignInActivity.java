package com.example.myruns.Activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.VideoView;

import com.example.myruns.R;

public class SignInActivity extends AppCompatActivity {

    private static final String LOGGED_IN = "logged";
    private static final int REQUEST_LOGGED = 0;
    // View Declarations
    private VideoView mVideoView;
    private Button mSignUpButton;
    private Button mSignInButton;
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mPreferenceEditor;

    // Request Codes
    private static final int REQUEST_REGISTRATION = 1;
    private int REQUEST_LOGIN = 2;


    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);

        mSharedPreferences = getSharedPreferences("ProfileData", MODE_PRIVATE);
        mPreferenceEditor = mSharedPreferences.edit();
        Log.d("WOMP", ""+mSharedPreferences.getBoolean(LOGGED_IN, false));

        // Change logged in value if coming from preferences.
        if(getIntent().getBooleanExtra(SettingsActivity.SettingsFragment.PREFERENCES, false) || getIntent().getBooleanExtra(ProfileActivity.FROM_PROFILE, false)) {
            mPreferenceEditor.putBoolean(LOGGED_IN, false);
            mPreferenceEditor.commit();
        }

        // Check if logged in already.
        if (mSharedPreferences.getBoolean(LOGGED_IN, false)) {
            finish();
            Intent mainIntent = new Intent(SignInActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivityForResult(mainIntent, REQUEST_LOGGED);
        }

        // Find Views
        mEmailEditText = findViewById(R.id.email_edittext);
        mPasswordEditText = findViewById(R.id.password_edittext);
        mSignInButton = findViewById(R.id.signin_button);
        mSignUpButton = findViewById(R.id.signup_button);


        mVideoView = (VideoView)findViewById(R.id.dartmouth_drone_video);

        // Start Background Video
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.dartmouthdrone);
        mVideoView.setVideoURI(uri);
        mVideoView.start();

        // Crop the video to fit the current orientation
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                mp.setVolume(0,0);
                mp.setLooping(true);
                float videoRatio = mp.getVideoWidth() / (float) mp.getVideoHeight();
                float screenRatio = mVideoView.getWidth() / (float) mVideoView.getHeight();
                float scaleX = videoRatio / screenRatio;
                if (scaleX >= 1f) {
                    mVideoView.setScaleX(scaleX);
                } else {
                    mVideoView.setScaleY(1f / scaleX);
                }
            }
        });

        // Set a listener on the sign up button; fire an intent to start the registration activity
        // if triggered
        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, ProfileActivity.class);
                startActivityForResult(intent, REQUEST_REGISTRATION);
            }
        });

        // Set a listener on the sign in button; fire an intent to start the main activity
        // if triggered and fields match a valid  user, begin main activity
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmailEditText.getText().toString();
                String password = mPasswordEditText.getText().toString();

                boolean emailBlank = errorCheck(mEmailEditText, email);
                boolean passwordBlank = errorCheck(mPasswordEditText, password);

                // If neither field is blank, check to make sure they both correspond to the saved user
                if (!emailBlank && !passwordBlank) {
                    if (!mSharedPreferences.getString(ProfileActivity.PROFILE_EMAIL, "").equals(email)) {
                        mEmailEditText.setError(getString(R.string.error_invalid_email_login));
                    } else if (!mSharedPreferences.getString(ProfileActivity.PROFILE_PASSWORD, "").equals(password)) {
                        mPasswordEditText.setError(getString(R.string.error_invalid_password_login));
                    } else {
                        mPreferenceEditor.putBoolean(LOGGED_IN, true);
                        mPreferenceEditor.commit();
                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                        startActivityForResult(intent, REQUEST_LOGIN);
                    }
                }

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == SettingsActivity.SettingsFragment.FROM_PREFERENCES) {

        }
    }

    // Error checking helper to identify blank fields
    public boolean errorCheck(EditText mField, String fieldText){
        View focusView = null;
        boolean cancel = false;

        mField.setError(null);

        if (TextUtils.isEmpty(fieldText)){
            mField.setError(getString(R.string.error_field_required));
            focusView = mField;
            cancel = true;
        }
        if (cancel){
            if (focusView instanceof EditText){
                focusView.requestFocus();
            }
        }
        return cancel;
    }
}
