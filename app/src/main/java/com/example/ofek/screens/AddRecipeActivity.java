package com.example.ofek.screens;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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

    private TextInputEditText etTitle, etDescription, etIngredients, etInstructions, etPrepTime;
    private AutoCompleteTextView actvDifficulty;

    private Button btnSubmit;
    private ImageView ivRecipePreview;
    private TextView tvAddImageHint;
    private MaterialCardView cardSelectImage;

    private DatabaseReference recipesRef;
    private User currentUser;
    private Uri selectedImageUri;

    // משתנה לעריכה
    private Recipe recipeToEdit = null;

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
        setupDifficultyDropdown();
        setupClickListeners();

        // בדיקה אם נכנסנו למצב עריכה
        if (getIntent().hasExtra("RECIPE_TO_EDIT")) {
            recipeToEdit = (Recipe) getIntent().getSerializableExtra("RECIPE_TO_EDIT");
            fillFormForEdit();
        }
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.etRecipeTitle);
        etDescription = findViewById(R.id.etRecipeDescription);
        etIngredients = findViewById(R.id.etRecipeIngredients);
        etInstructions = findViewById(R.id.etRecipeInstructions);
        etPrepTime = findViewById(R.id.etPrepTime);
        actvDifficulty = findViewById(R.id.actvDifficulty);
        btnSubmit = findViewById(R.id.btnSubmitRecipe);
        ivRecipePreview = findViewById(R.id.ivRecipePreview);
        tvAddImageHint = findViewById(R.id.tvAddImageHint);
        cardSelectImage = findViewById(R.id.cardSelectImage);
    }

    private void setupDifficultyDropdown() {
        String[] difficulties = new String[] {"Easy", "Medium", "Hard"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                difficulties
        );
        actvDifficulty.setAdapter(adapter);
        actvDifficulty.setText(difficulties[1], false);
    }

    private void setupClickListeners() {
        cardSelectImage.setOnClickListener(v -> openGallery());
        btnSubmit.setOnClickListener(v -> submitRecipe());
    }

    private void fillFormForEdit() {
        etTitle.setText(recipeToEdit.getTitle());
        etDescription.setText(recipeToEdit.getDescription());
        etIngredients.setText(recipeToEdit.getIngredients());
        etInstructions.setText(recipeToEdit.getInstructions());
        etPrepTime.setText(recipeToEdit.getPreparationTime());
        actvDifficulty.setText(recipeToEdit.getDifficulty(), false);
        btnSubmit.setText("Fix & Resubmit");

        // הערה: טעינת התמונה הקיימת דורשת ספרייה חיצונית.
        // כרגע אם המשתמש לא יבחר תמונה חדשה, נשמור על ה-URL הישן.
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
        String prepTime = etPrepTime.getText().toString().trim();
        String difficulty = actvDifficulty.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String recipeId;
        if (recipeToEdit != null) {
            recipeId = recipeToEdit.getId(); // שימוש ב-ID הקיים
        } else {
            recipeId = recipesRef.push().getKey(); // יצירת חדש
        }

        // שמירת התמונה: חדשה אם נבחרה, או הישנה אם אנחנו בעריכה
        String imageUrl = "";
        if (selectedImageUri != null) {
            imageUrl = selectedImageUri.toString();
        } else if (recipeToEdit != null) {
            imageUrl = recipeToEdit.getImageUrl();
        }

        Recipe newRecipe = new Recipe(
                recipeId,
                title,
                description,
                ingredients,
                instructions,
                currentUser.getId(),
                "General",
                prepTime,
                difficulty
        );
        newRecipe.setImageUrl(imageUrl);

        // איפוס הסטטוסים לבדיקה מחדש
        newRecipe.setApproved(false);
        newRecipe.setAdminNotes(""); // מחיקת ההערה כדי שהמנהל יראה את זה שוב

        if (recipeId != null) {
            recipesRef.child(recipeId).setValue(newRecipe)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Recipe submitted successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to submit recipe", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}