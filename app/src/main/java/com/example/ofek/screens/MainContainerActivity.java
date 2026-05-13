package com.example.ofek.screens;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.ofek.R;
import com.example.ofek.adapters.MainPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainContainerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);
        
        // Default to Home (index 1 in the adapter)
        viewPager.setCurrentItem(1, false);
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_saved) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.nav_profile) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                bottomNavigation.getMenu().getItem(position).setChecked(true);
            }
        });
    }
}