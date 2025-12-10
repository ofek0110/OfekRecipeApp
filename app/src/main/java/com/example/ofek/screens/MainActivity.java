package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ofek.R;
import com.example.ofek.models.User;
import com.example.ofek.utils.SharedPreferencesUtil;


public class MainActivity extends AppCompatActivity {
    Button btnLogout;
    User user;
    TextView Name;
    String FName,LName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        user = SharedPreferencesUtil.getUser(MainActivity.this);
        Name = findViewById(R.id.TvName);
        FName = user.getFirstname();
        Name.setText(FName);

        // מציאת הכפתור מהתצוגה
         btnLogout = findViewById(R.id.LogOutBtn);

// הקשבה ללחיצה על הכפתור
        btnLogout.setOnClickListener(v -> {



            // מחיקת פרטי המשתמש מהטלפון (SharedPreferences)
            SharedPreferencesUtil.signOutUser(this);

            // מעבר חזרה למסך ההתחברות
            startActivity(new Intent(MainActivity.this, LandingActivity.class));

            // סגירת מסך הבית כך שלא יחזור אליו בלחיצה אחורה
            finish();

        });





    }


}
