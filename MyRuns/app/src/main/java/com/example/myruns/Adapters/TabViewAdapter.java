package com.example.myruns.Adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

public class TabViewAdapter extends FragmentPagerAdapter {

    private static final int TAB_START_ID = 0;
    private static final int TAB_HISTORY_ID = 1;
    private static final String START_STRING = "Start";
    private static final String HISTORY_STRING = "History";
    private ArrayList<Fragment> FragmentArray;

    /* constructor */
    public TabViewAdapter(FragmentManager FragManager, ArrayList<Fragment> fragments, int behaivor){
        super(FragManager, behaivor);
        this.FragmentArray = fragments;
    }

    @NonNull
    @Override
    /* returns fragment in this position */
    public Fragment getItem(int position) {
        return FragmentArray.get(position);
    }

    @Override
    /*returns quantity of fragments */
    public int getCount() {
        return FragmentArray.size();
    }
/*
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_START_ID:
                return START_STRING;
            case TAB_HISTORY_ID:
                return HISTORY_STRING;
            default:
                break;
        }
        return null;
    }

 */

}
