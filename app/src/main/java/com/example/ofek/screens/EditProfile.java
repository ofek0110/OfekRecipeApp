package com.example.ofek.screens;

import android.content.Intent;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ofek.R;
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.example.ofek.utils.Validator;

public class EditProfile extends AppCompatActivity {
    EditText Password, Email,Fname,Lname,Phone;
    Button Submit;
    User user;
    String id;
    DatabaseService databaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        Fname = findViewById(R.id.etFname);
        Lname = findViewById(R.id.etLname);
        Phone = findViewById(R.id.etPhone);
        Email = findViewById(R.id.etEmail);
        Password = findViewById(R.id.etPassword);

        user = SharedPreferencesUtil.getUser(this);
        if (user == null) {
            Toast.makeText(this, "User not loaded", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        databaseService = DatabaseService.getInstance();
        Submit = findViewById(R.id.btnSave);

        Fname.setText(user.getFirstname());
        Lname.setText(user.getLastname());
        Phone.setText(user.getPhone());
        Password.setText(user.getPassword());
        Email.setText(user.getEmail());
        id = user.getId();

        Submit.setOnClickListener(v -> {
            if(!checkInputUpdate(Fname.getText().toString(),Lname.getText().toString(),Phone.getText().toString(),Email.getText().toString(),Password.getText().toString())) {
                return;
            }
            databaseService.getUser(id, new DatabaseService.DatabaseCallback<User>() {
                @Override
                public void onCompleted(User user) {
                    user.setFirstname(Fname.getText().toString().trim()+"");
                    user.setLastname(Lname.getText().toString().trim()+"");
                    user.setPhone(Phone.getText().toString().trim()+"");
                    user.setEmail(Email.getText().toString().trim()+"");
                    user.setPassword(Password.getText().toString().trim()+"");

                    databaseService.updateUser(user, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {

                        }

                        @Override
                        public void onFailed(Exception e) {

                        }
                    });
                    SharedPreferencesUtil.saveUser(EditProfile.this, user);
                    Intent from_update_to_main = new Intent(EditProfile.this, MainActivity.class);
                    startActivity(from_update_to_main);
                }

                @Override
                public void onFailed(Exception e) {

                }
            });
        });
    }
    private boolean checkInputUpdate(String fname,String lname, String phone,String email,String password){
        if (!Validator.isNameValid(fname)) {
            Fname.setError("Invalid first name");
            /// set focus to fname field
            Fname.requestFocus();
            return false;
        }
        if (!Validator.isLastNameValid(lname)) {
            Lname.setError("Invalid last name");
            /// set focus to lname field
            Lname.requestFocus();
            return false;
        }
        if (!Validator.isPhoneValid(phone)) {
            Phone.setError("Invalid phone number");
            /// set focus to number field
            Phone.requestFocus();
            return false;
        }
        if (!Validator.isEmailValid(email)) {
            Email.setError("Invalid email address");
            /// set focus to email field
            Email.requestFocus();
            return false;
        }

        if (!Validator.isPasswordValid(password)) {
            Password.setError("Password must be at least 6 characters long");
            /// set focus to password field
            Password.requestFocus();
            return false;
        }
        return true;
    }
}

