package com.albaz.appuser.activities;


import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.albaz.appuser.fragments.OnGoingTrips;
import com.albaz.appuser.fragments.PastTrips;
import com.albaz.appuser.R;

public class HistoryActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private TabLayout tabLayout;
    private String tabTitles[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // SetUp ActionBar

        // Initialize Element
        viewPager = findViewById(R.id.viewpager);
        tabLayout = findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        String strTag = getIntent().getExtras().getString("tag");
        tabTitles = new String[]{"Past Trips", "Upcoming Trips"};

        // SetAdapter In ViewPager
        viewPager.setAdapter(new SampleFragmentPagerAdapter(tabTitles, getSupportFragmentManager(),this));

        // Checked StrTag
        if (strTag != null) {
            if (strTag.equalsIgnoreCase("past")) {
                viewPager.setCurrentItem(0); // Set Position Index 0
            } else {
                viewPager.setCurrentItem(1); // Set Position Index 1
            }
        }
    }

    // Selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }


    // Class Fragment
    public class SampleFragmentPagerAdapter extends FragmentPagerAdapter {
        final int PAGE_COUNT = 2;
        private String tabTitles[];
        private Context context;

        // Invoke Constrictor With Title Tabs
        public SampleFragmentPagerAdapter(String tabTitles[], FragmentManager fm, Context context) {
            super(fm);
            this.context = context;
            this.tabTitles = tabTitles;
        }

        // Count Tabs
        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        // Get Invoke Class When Select Position Item Fragment
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new PastTrips();
                case 1:
                    return new OnGoingTrips();
                default:
                    return new PastTrips();
            }
        }

        // Set Title Tabs
        @Override
        public CharSequence getPageTitle(int position) {
            // Generate title based on item position
            return tabTitles[position];
        }
    }

}
