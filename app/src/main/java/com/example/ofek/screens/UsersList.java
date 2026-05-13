package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.adapters.UserAdapter;
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;

import java.util.List;

public class UsersList extends AppCompatActivity {

    private static final String TAG = "UsersListActivity";
    private UserAdapter userAdapter;
    private TextView tvUserCount;
    private DatabaseService databaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_users_list);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        databaseService = DatabaseService.getInstance();
        RecyclerView usersList = findViewById(R.id.rv_users_list);
        tvUserCount = findViewById(R.id.tv_user_count);
        
        usersList.setLayoutManager(new LinearLayoutManager(this));
        
        userAdapter = new UserAdapter(new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                Log.d(TAG, "User clicked: " + user.getFirstname());
                // ניווט ל-UserProfile Activity (Java)
                Intent intent = new Intent(UsersList.this, UserProfile.class);
                intent.putExtra("USER_UID", user.getId());
                startActivity(intent);
            }

            @Override
            public void onLongUserClick(User user) {
                Log.d(TAG, "User long clicked: " + user.getEmail());
            }
        });
        
        usersList.setAdapter(userAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // הגדרת ה-Callback בצורה מפורשת עבור Java
        databaseService.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                if (users != null) {
                    userAdapter.setUserList(users);
                    tvUserCount.setText("Total users: " + users.size());
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to get users list", e);
            }
        });
    }
}