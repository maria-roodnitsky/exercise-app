package com.example.myruns.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;

import com.example.myruns.Fragment.HistoryFragment;
import com.example.myruns.R;
import com.example.myruns.Fragment.StartFragment;
import com.example.myruns.Adapters.TabViewAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

/** MYRUNS2: MainActivity allows scrolling between two pages, the start and history fragments
 */

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_SETTINGS = 10;
    public static final int REQUEST_PROFILE = 20;
    public static final String KEY_FROM_MAIN = "fromMainActivity";
    public static final String TAB_POSITION = "tabPosition";

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private Toolbar mToolBar;
    private ArrayList<Fragment> mFragmentList;
    private TabViewAdapter mViewPageAdapter;
    private int currentTab = 0;
    private int[] tabIcons = {R.drawable.start_icon, R.drawable.history_icon};

    //grab the unit
    public SharedPreferences mPreference;
    public static String units;


    // Inflates the settings menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }



    // Fires intent based off of selected option in settings menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_layout:
                // Send to settings
                Intent intent_settings = new Intent(MainActivity.this, SettingsActivity.class);
                intent_settings.putExtra(TAB_POSITION, currentTab);
                startActivityForResult(intent_settings, REQUEST_SETTINGS);
                break;
            case R.id.edit_profile:
                // Send to profile (noting that the user ended up at profile from beyond the login page)
                Intent intent_profile = new Intent(MainActivity.this, ProfileActivity.class);
                intent_profile.putExtra(KEY_FROM_MAIN, true);
                startActivityForResult(intent_profile, REQUEST_PROFILE);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check location permissions
        checkPermissions();

        // check the unit needed for display
        mPreference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        units = mPreference.getString("Unit Preference", "kilometers");

        // Toolbar
        mToolBar =  findViewById(R.id.toolbar);
        setSupportActionBar(mToolBar);
        try {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        } catch (Exception e){
            e.printStackTrace();
        }

        // Tabs
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);



        // Creates list of different fragments
        mFragmentList = new ArrayList<Fragment>();
        mFragmentList.add(new StartFragment());
        mFragmentList.add(new HistoryFragment());


        // Use adapter to turn fragments into tabs in the viewPager.
        mViewPageAdapter = new TabViewAdapter(getSupportFragmentManager(),
                mFragmentList, mViewPageAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        // Give the page adapter to the page
        mViewPager.setAdapter(mViewPageAdapter);

        // Set the tab layout up
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        if(getIntent() != null) {
            mTabLayout.setScrollPosition(getIntent().getIntExtra(SettingsActivity.CURRENT_TAB, 0), 0f, true);
            mViewPager.setCurrentItem(getIntent().getIntExtra(SettingsActivity.CURRENT_TAB, 0));
        }

        // Icons
        try {
            mTabLayout.getTabAt(0).setIcon(tabIcons[0]);
            mTabLayout.getTabAt(1).setIcon(tabIcons[1]);
        } catch (Exception e){
            e.printStackTrace();
        }
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Show an explanation to the user asychronously
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Location Services are necessary to use the map!").setTitle("Important permission required");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 0);
                        }
                    });
                    builder.show();
                }
            }
        }
    }

    /// HELPER METHODS ///

    /**
     * Permission to use the location of the people.
     */
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        // Allows us to use the location of our user for all sorts of m i s c h i e f :)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
    }
}