package com.example.ofek.screens;

import android.content.Intent;
import android.graphics.Bitmap;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ofek.R;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.ImageUtil;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class AddRecipeActivity extends AppCompatActivity {

    private TextInputEditText EtTitle, EtDescription, EtIngredients, EtInstructions, EtPrepTime;
    private AutoCompleteTextView ActvDifficulty, ActvCategory; // הוספנו את רכיב הקטגוריה

    private Button BtnSubmit;
    private MaterialButton BtnViewRejectionReason;
    private ImageView IvRecipePreview;
    private TextView TvAddImageHint;
    private MaterialCardView CardSelectImage;
    private User currentUser;
    private Uri selectedImageUri;

    /// Activity result launcher for selecting image from gallery
    private ActivityResultLauncher<Intent> selectImageLauncher;

    // משתנה לעריכה
    private Recipe recipeToEdit = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        currentUser = SharedPreferencesUtil.getUser(this);

        /// request permission for the camera and storage
        ImageUtil.requestPermission(this);

        initializeViews();
        setupDropdowns(); // הפונקציה המעודכנת שמגדירה גם רמת קושי וגם קטגוריה
        setupClickListeners();

        // בדיקה אם נכנסנו למצב עריכה
        if (getIntent().hasExtra("RECIPE_TO_EDIT")) {
            recipeToEdit = (Recipe) getIntent().getSerializableExtra("RECIPE_TO_EDIT");
            fillFormForEdit();
        }

        /// register the activity result launcher for selecting image from gallery
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        IvRecipePreview.setImageURI(selectedImage);
                        /// set the tag for the image view to null
                        IvRecipePreview.setTag(null);
                    }
                });

    }

    private void initializeViews() {
        EtTitle = findViewById(R.id.EtRecipeTitle);
        EtDescription = findViewById(R.id.EtRecipeDescription);
        EtIngredients = findViewById(R.id.EtRecipeIngredients);
        EtInstructions = findViewById(R.id.EtRecipeInstructions);
        EtPrepTime = findViewById(R.id.EtPrepTime);
        ActvDifficulty = findViewById(R.id.ActvDifficulty);

        // חיבור הקטגוריה למסך (יש לוודא שהוספת את זה בקובץ ה-XML)
        ActvCategory = findViewById(R.id.ActvCategory);

        BtnSubmit = findViewById(R.id.BtnSubmitRecipe);
        IvRecipePreview = findViewById(R.id.IvRecipePreview);
        TvAddImageHint = findViewById(R.id.TvAddImageHint);
        CardSelectImage = findViewById(R.id.CardSelectImage);
        BtnViewRejectionReason = findViewById(R.id.BtnViewRejectionReason);
    }

    private void setupDropdowns() {
        // הגדרת רמת קושי
        String[] difficulties = new String[] {"Easy", "Medium", "Hard"};
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                difficulties
        );
        ActvDifficulty.setAdapter(difficultyAdapter);
        ActvDifficulty.setText(difficulties[1], false);

        // הגדרת קטגוריות
        if (ActvCategory != null) {
            String[] categories = new String[] {"Breakfast", "Lunch", "Vegan", "Desserts", "Dinner", "General"};
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    categories
            );
            ActvCategory.setAdapter(categoryAdapter);
            ActvCategory.setText(categories[5], false); // ברירת מחדל: General
        }
    }

    private void setupClickListeners() {
        CardSelectImage.setOnClickListener(v -> selectImageFromGallery());
        BtnSubmit.setOnClickListener(v -> submitRecipe());
    }

    private void fillFormForEdit() {
        EtTitle.setText(recipeToEdit.getTitle());
        EtDescription.setText(recipeToEdit.getDescription());
        EtIngredients.setText(recipeToEdit.getIngredients());
        EtInstructions.setText(recipeToEdit.getInstructions());
        EtPrepTime.setText(recipeToEdit.getPreparationTime());
        ActvDifficulty.setText(recipeToEdit.getDifficulty(), false);

        // עדכון הקטגוריה במסך העריכה
        if (ActvCategory != null && recipeToEdit.getCategory() != null) {
            ActvCategory.setText(recipeToEdit.getCategory(), false);
        }

        BtnSubmit.setText("Fix & Resubmit");

        if (recipeToEdit.getAdminNotes() != null && !recipeToEdit.getAdminNotes().isEmpty()) {
            BtnViewRejectionReason.setVisibility(View.VISIBLE);
            BtnViewRejectionReason.setOnClickListener(v -> {
                new AlertDialog.Builder(AddRecipeActivity.this)
                        .setTitle("Admin Feedback")
                        .setMessage(recipeToEdit.getAdminNotes())
                        .setPositiveButton("Got it", null)
                        .show();
            });
        }
    }

    /// select image from gallery
    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selectImageLauncher.launch(intent);
    }

    private void submitRecipe() {
        String title = EtTitle.getText().toString().trim();
        String description = EtDescription.getText().toString().trim();
        String ingredients = EtIngredients.getText().toString().trim();
        String instructions = EtInstructions.getText().toString().trim();
        String prepTime = EtPrepTime.getText().toString().trim();
        String difficulty = ActvDifficulty.getText().toString().trim();

        // שליפת הקטגוריה שנבחרה במקום לקבע אותה
        String category = "General";
        if (ActvCategory != null) {
            category = ActvCategory.getText().toString().trim();
        }

        if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String recipeId;
        if (recipeToEdit != null) {
            recipeId = recipeToEdit.getId();
        } else {
            recipeId = DatabaseService.getInstance().generateRecipeId();
        }

        String imageBase64 = ImageUtil.convertTo64Base(IvRecipePreview);

        Recipe newRecipe = new Recipe(
                recipeId,
                title,
                description,
                ingredients,
                instructions,
                imageBase64,
                currentUser.getId(),
                category, // הקטגוריה שנבחרה נכנסת לכאן
                prepTime,
                difficulty,
                false,
                null
        );
        newRecipe.setApproved(false);
        newRecipe.setAdminNotes("");

        if (recipeId != null) {
            DatabaseService.getInstance().createNewRecipe(newRecipe, new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(@Nullable Void v) {
                    Toast.makeText(AddRecipeActivity.this, "Recipe submitted successfully!", Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(AddRecipeActivity.this, "Failed to submit recipe", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}