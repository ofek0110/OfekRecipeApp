package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ofek.R;

public class LandingActivity extends AppCompatActivity {
    Button Btn_landing_go_to_signin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landing);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Btn_landing_go_to_signin = findViewById(R.id.Btn_landing_go_to_sign_in);
        Btn_landing_go_to_signin.setOnClickListener(v -> {
            Intent intentlog = new Intent(LandingActivity.this , LogIn.class);
            startActivity(intentlog);
        });
    }
}