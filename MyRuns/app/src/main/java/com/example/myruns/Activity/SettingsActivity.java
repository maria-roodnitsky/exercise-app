package com.example.myruns.Activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.myruns.R;

public class SettingsActivity extends AppCompatActivity {


    private static final int FROM_SETTINGS = 3;
    public static final String CURRENT_TAB = "currentTab";

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.putExtra(CURRENT_TAB, getIntent().getIntExtra(MainActivity.TAB_POSITION, 0));
            startActivityForResult(intent, FROM_SETTINGS);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_layout, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            //getSupportActionBar().setTitle(Html.fromHtml("<font color='#00303B'>Settings</font>"));
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        intent.putExtra(CURRENT_TAB, getIntent().getIntExtra(MainActivity.TAB_POSITION, 0));
        startActivityForResult(intent, FROM_SETTINGS);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        public static final int FROM_PREFERENCES = 10;
        public static final String PREFERENCES = "preferences";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference signOut = (Preference) findPreference("Sign Out");
            signOut.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getActivity().finish();
                    Intent intent = new Intent(getActivity(), SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra(PREFERENCES, true);
                    startActivityForResult(intent, FROM_PREFERENCES);
                    return true;
                }
            });
        }
    }
}