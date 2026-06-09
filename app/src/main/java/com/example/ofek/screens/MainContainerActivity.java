package com.example.ofek.screens;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.example.ofek.R;
import com.example.ofek.adapters.MainPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// אקטיביטי מכולה (Container) ראשי שמחזיק ומסנכרן את שלושת מסכי הליבה באמצעות תפריט תחתון וגלילה.
public class MainContainerActivity extends AppCompatActivity {

    // רכיבי ה-UI המרכזיים לניווט
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        // סידור שולי התצוגה (Insets) למניעת חפיפה עם סרגל המערכת העליון
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // אתחול רכיבי ה-UI וחיבור האדפטר (MainPagerAdapter) שאחראי על יצירת הפרגמנטים
        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // הגדרת מצב התחלתי: מסך הבית (מיקום 1 באדפטר) נבחר כברירת מחדל מיד עם פתיחת האפליקציה
        viewPager.setCurrentItem(1, false);
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        // מאזין לחיצה לתפריט התחתון: מעביר את ה-ViewPager למסך המתאים (0=שמורים, 1=בית, 2=פרופיל)
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

        // סנכרון הפוך (מאזין החלקה): במידה והמשתמש גורר את המסך ימינה/שמאלה, נעדכן את האייקון הנבחר בתפריט התחתון בהתאם
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                switch (position) {
                    case 0:
                        bottomNavigation.getMenu().findItem(R.id.nav_saved).setChecked(true);
                        break;
                    case 1:
                        bottomNavigation.getMenu().findItem(R.id.nav_home).setChecked(true);
                        break;
                    case 2:
                        bottomNavigation.getMenu().findItem(R.id.nav_profile).setChecked(true);
                        break;
                }
            }
        });
    }
}