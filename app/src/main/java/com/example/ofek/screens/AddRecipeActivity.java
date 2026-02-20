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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ofek.R;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddRecipeActivity extends AppCompatActivity {

    private TextInputEditText EtTitle, EtDescription, EtIngredients, EtInstructions, EtPrepTime;
    private AutoCompleteTextView ActvDifficulty;

    private Button BtnSubmit;
    private MaterialButton BtnViewRejectionReason; // הכפתור החדש
    private ImageView IvRecipePreview;
    private TextView TvAddImageHint;
    private MaterialCardView CardSelectImage;

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
                    IvRecipePreview.setImageURI(selectedImageUri);
                    IvRecipePreview.setPadding(0, 0, 0, 0);
                    IvRecipePreview.setAlpha(1.0f);
                    TvAddImageHint.setVisibility(View.GONE);
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
        EtTitle = findViewById(R.id.EtRecipeTitle);
        EtDescription = findViewById(R.id.EtRecipeDescription);
        EtIngredients = findViewById(R.id.EtRecipeIngredients);
        EtInstructions = findViewById(R.id.EtRecipeInstructions);
        EtPrepTime = findViewById(R.id.EtPrepTime);
        ActvDifficulty = findViewById(R.id.ActvDifficulty);
        BtnSubmit = findViewById(R.id.BtnSubmitRecipe);
        IvRecipePreview = findViewById(R.id.IvRecipePreview);
        TvAddImageHint = findViewById(R.id.TvAddImageHint);
        CardSelectImage = findViewById(R.id.CardSelectImage);

        // חיבור הכפתור החדש
        BtnViewRejectionReason = findViewById(R.id.BtnViewRejectionReason);
    }

    private void setupDifficultyDropdown() {
        String[] difficulties = new String[] {"Easy", "Medium", "Hard"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                difficulties
        );
        ActvDifficulty.setAdapter(adapter);
        ActvDifficulty.setText(difficulties[1], false);
    }

    private void setupClickListeners() {
        CardSelectImage.setOnClickListener(v -> openGallery());
        BtnSubmit.setOnClickListener(v -> submitRecipe());
    }

    private void fillFormForEdit() {
        EtTitle.setText(recipeToEdit.getTitle());
        EtDescription.setText(recipeToEdit.getDescription());
        EtIngredients.setText(recipeToEdit.getIngredients());
        EtInstructions.setText(recipeToEdit.getInstructions());
        EtPrepTime.setText(recipeToEdit.getPreparationTime());
        ActvDifficulty.setText(recipeToEdit.getDifficulty(), false);
        BtnSubmit.setText("Fix & Resubmit");

        // אם יש הערת מנהל - מציגים את הכפתור שמקפיץ את הדיאלוג
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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void submitRecipe() {
        String title = EtTitle.getText().toString().trim();
        String description = EtDescription.getText().toString().trim();
        String ingredients = EtIngredients.getText().toString().trim();
        String instructions = EtInstructions.getText().toString().trim();
        String prepTime = EtPrepTime.getText().toString().trim();
        String difficulty = ActvDifficulty.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String recipeId;
        if (recipeToEdit != null) {
            recipeId = recipeToEdit.getId();
        } else {
            recipeId = recipesRef.push().getKey();
        }

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

        // איפוס הסטטוסים לבדיקה מחדש אצל המנהל
        newRecipe.setApproved(false);
        newRecipe.setAdminNotes("");

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