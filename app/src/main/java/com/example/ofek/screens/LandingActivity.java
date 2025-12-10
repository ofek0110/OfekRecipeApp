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
import com.example.ofek.utils.SharedPreferencesUtil;

public class LandingActivity extends AppCompatActivity {
    Button Btn_landing_go_to_signin , Btn_landing_go_to_create;
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
        if(SharedPreferencesUtil.isUserLoggedIn(this)) {
            Intent mainIntent = new Intent(LandingActivity.this, MainActivity.class);
            /// clear the back stack (clear history) and start the MainActivity
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(mainIntent);
            finish();
            return;
        }

        Btn_landing_go_to_signin = findViewById(R.id.Btn_landing_go_to_sign_in);
        Btn_landing_go_to_create = findViewById(R.id.btn_landing_go_to_create_account);


        Btn_landing_go_to_signin.setOnClickListener(v -> {
            Intent intentlog = new Intent(LandingActivity.this , LogIn.class);
            startActivity(intentlog);
        });

        Btn_landing_go_to_create.setOnClickListener(v -> {
            Intent intentlog = new Intent(LandingActivity.this , Register.class);
            startActivity(intentlog);
        });

    }


}