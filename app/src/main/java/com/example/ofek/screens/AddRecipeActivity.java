package com.example.ofek.screens;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ofek.R;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddRecipeActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etIngredients, etInstructions, etPrepTime, etDifficulty;
    private Button btnSubmit;
    private ImageView ivRecipePreview;
    private TextView tvAddImageHint;
    private MaterialCardView cardSelectImage;

    private DatabaseReference recipesRef;
    private User currentUser;
    private Uri selectedImageUri;

    // Launcher for selecting an image from gallery
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivRecipePreview.setImageURI(selectedImageUri);
                    ivRecipePreview.setPadding(0, 0, 0, 0);
                    ivRecipePreview.setAlpha(1.0f);
                    tvAddImageHint.setVisibility(View.GONE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        currentUser = SharedPreferencesUtil.getUser(this);
        recipesRef = FirebaseDatabase.getInstance().getReference("recipes");

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.etRecipeTitle);
        etDescription = findViewById(R.id.etRecipeDescription);
        etIngredients = findViewById(R.id.etRecipeIngredients);
        etInstructions = findViewById(R.id.etRecipeInstructions);
        // חיבור השדות החדשים
        etPrepTime = findViewById(R.id.etPrepTime);
        etDifficulty = findViewById(R.id.etDifficulty);

        btnSubmit = findViewById(R.id.btnSubmitRecipe);
        ivRecipePreview = findViewById(R.id.ivRecipePreview);
        tvAddImageHint = findViewById(R.id.tvAddImageHint);
        cardSelectImage = findViewById(R.id.cardSelectImage);
    }

    private void setupClickListeners() {
        cardSelectImage.setOnClickListener(v -> openGallery());
        btnSubmit.setOnClickListener(v -> submitRecipe());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void submitRecipe() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String ingredients = etIngredients.getText().toString().trim();
        String instructions = etInstructions.getText().toString().trim();
        // קליטת הערכים החדשים
        String prepTime = etPrepTime.getText().toString().trim();
        String difficulty = etDifficulty.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String recipeId = recipesRef.push().getKey();
        String imageUrl = (selectedImageUri != null) ? selectedImageUri.toString() : "";

        // שימוש בבנאי החדש (ללא imageUrl וללא boolean בסוף)
        Recipe newRecipe = new Recipe(
                recipeId,
                title,
                description,
                ingredients,
                instructions,
                currentUser.getId(),
                "General", // Default category
                prepTime,  // שדה חדש
                difficulty // שדה חדש
        );

        // הגדרת התמונה בנפרד (כי הסרנו אותה מהבנאי בטעות בקובץ הקודם, או פשוט כסטנדרט)
        newRecipe.setImageUrl(imageUrl);

        if (recipeId != null) {
            recipesRef.child(recipeId).setValue(newRecipe)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Recipe submitted for approval!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to submit recipe", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}