package com.example.ofek.screens;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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
import com.example.ofek.utils.SharedPreferencesUtil;

import java.util.List;

public class UsersList extends AppCompatActivity {

    private static final String TAG = "UsersListActivity";
    private UserAdapter userAdapter;
    private TextView tvUserCount;
    private DatabaseService databaseService;
    private User currentUser;

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
        currentUser = SharedPreferencesUtil.getUser(this);

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
            public void onLongUserClick(User selectedUser) {
                Log.d(TAG, "User long clicked: " + selectedUser.getEmail());

                // בדיקה אם המשתמש הנוכחי הוא אדמין
                if (currentUser != null && currentUser.isAdmin()) {

                    // חסימה: מנהל לא יכול למחוק את עצמו דרך הרשימה
                    if (currentUser.getId().equals(selectedUser.getId())) {
                        Toast.makeText(UsersList.this, "You cannot delete your own admin account", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // הקפצת חלונית אישור
                    new AlertDialog.Builder(UsersList.this)
                            .setTitle("Delete User")
                            .setMessage("Are you sure you want to completely delete " + selectedUser.getFirstname() + " from the database?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteUserFromDatabase(selectedUser);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }
        });

        usersList.setAdapter(userAdapter);
    }

    private void deleteUserFromDatabase(User userToDelete) {
        databaseService.deleteUser(userToDelete.getId(), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(UsersList.this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                // מחיקה מהמתאם (Adapter) כדי שהרשימה תתעדכן מיד מבלי לרענן את המסך
                userAdapter.removeUser(userToDelete);
                // עדכון ספירת המשתמשים
                tvUserCount.setText("Total users: " + userAdapter.getItemCount());
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to delete user", e);
                Toast.makeText(UsersList.this, "Error deleting user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
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