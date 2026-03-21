package com.example.ofek.screens;

import android.app.ProgressDialog;
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
import com.example.ofek.models.FoodTokPost;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class AddRecipeActivity extends AppCompatActivity {

    private TextInputEditText EtTitle, EtDescription, EtIngredients, EtInstructions, EtPrepTime;
    private AutoCompleteTextView ActvDifficulty, ActvCategory;

    private Button BtnSubmit;
    private MaterialButton BtnViewRejectionReason, BtnAddFoodTokMedia;
    private ImageView IvRecipePreview;
    private TextView TvAddImageHint, TvFoodTokStatus;
    private MaterialCardView CardSelectImage;

    private DatabaseReference recipesRef;
    private User currentUser;
    private Uri selectedImageUri;
    private Uri selectedFoodTokUri;
    private String foodTokMediaType = "";

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

    private final ActivityResultLauncher<Intent> foodTokPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedFoodTokUri = result.getData().getData();
                    String type = getContentResolver().getType(selectedFoodTokUri);
                    if (type != null && type.startsWith("video")) {
                        foodTokMediaType = "video";
                        TvFoodTokStatus.setText("Video selected for FoodTok");
                    } else {
                        foodTokMediaType = "image";
                        TvFoodTokStatus.setText("Image selected for FoodTok");
                    }
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
        setupDropdowns();
        setupClickListeners();

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
        ActvCategory = findViewById(R.id.ActvCategory);

        BtnSubmit = findViewById(R.id.BtnSubmitRecipe);
        IvRecipePreview = findViewById(R.id.IvRecipePreview);
        TvAddImageHint = findViewById(R.id.TvAddImageHint);
        CardSelectImage = findViewById(R.id.CardSelectImage);
        BtnViewRejectionReason = findViewById(R.id.BtnViewRejectionReason);
        BtnAddFoodTokMedia = findViewById(R.id.BtnAddFoodTokMedia);
        TvFoodTokStatus = findViewById(R.id.TvFoodTokStatus);
    }

    private void setupDropdowns() {
        String[] difficulties = new String[] {"Easy", "Medium", "Hard"};
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, difficulties);
        ActvDifficulty.setAdapter(difficultyAdapter);

        String[] categories = new String[] {"Breakfast", "Lunch", "Vegan", "Desserts", "Dinner", "General"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        ActvCategory.setAdapter(categoryAdapter);
    }

    private void setupClickListeners() {
        CardSelectImage.setOnClickListener(v -> openGallery(false));
        BtnAddFoodTokMedia.setOnClickListener(v -> openGallery(true));
        BtnSubmit.setOnClickListener(v -> submitRecipe());
    }

    private void openGallery(boolean isFoodTok) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        if (isFoodTok) {
            intent.setType("video/* image/*");
            String[] mimeTypes = {"image/*", "video/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            foodTokPickerLauncher.launch(intent);
        } else {
            intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        }
    }

    private void submitRecipe() {
        String title = EtTitle.getText().toString().trim();
        String description = EtDescription.getText().toString().trim();
        String ingredients = EtIngredients.getText().toString().trim();
        String instructions = EtInstructions.getText().toString().trim();
        String prepTime = EtPrepTime.getText().toString().trim();
        String difficulty = ActvDifficulty.getText().toString().trim();
        String category = ActvCategory.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Saving Recipe...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String recipeId = (recipeToEdit != null) ? recipeToEdit.getId() : recipesRef.push().getKey();
        
        Recipe newRecipe = new Recipe(recipeId, title, description, ingredients, instructions, currentUser.getId(), category, prepTime, difficulty);
        
        if (selectedFoodTokUri != null) {
            uploadFoodTokMedia(selectedFoodTokUri, newRecipe, progressDialog);
        } else {
            saveRecipeToDatabase(newRecipe, progressDialog);
        }
    }

    private void uploadFoodTokMedia(Uri uri, Recipe recipe, ProgressDialog progressDialog) {
        String fileName = UUID.randomUUID().toString();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("foodtok_media/" + fileName);

        storageRef.putFile(uri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setMessage("Uploading FoodTok Media: " + (int)progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    recipe.setFoodTokUrl(downloadUri.toString());
                    recipe.setMediaType(foodTokMediaType);
                    
                    // Create FoodTok post
                    DatabaseReference foodTokRef = FirebaseDatabase.getInstance().getReference("foodtok_posts").push();
                    FoodTokPost post = new FoodTokPost(downloadUri.toString(), foodTokMediaType, recipe.getTitle(), currentUser.getFirstname());
                    foodTokRef.setValue(post);

                    saveRecipeToDatabase(recipe, progressDialog);
                }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveRecipeToDatabase(Recipe recipe, ProgressDialog progressDialog) {
        if (selectedImageUri != null) {
            recipe.setImageUrl(selectedImageUri.toString());
        } else if (recipeToEdit != null) {
            recipe.setImageUrl(recipeToEdit.getImageUrl());
        }

        recipesRef.child(recipe.getId()).setValue(recipe)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Recipe saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Database error", Toast.LENGTH_SHORT).show();
                });
    }

    private void fillFormForEdit() {
        EtTitle.setText(recipeToEdit.getTitle());
        EtDescription.setText(recipeToEdit.getDescription());
        EtIngredients.setText(recipeToEdit.getIngredients());
        EtInstructions.setText(recipeToEdit.getInstructions());
        EtPrepTime.setText(recipeToEdit.getPreparationTime());
        ActvDifficulty.setText(recipeToEdit.getDifficulty(), false);
        ActvCategory.setText(recipeToEdit.getCategory(), false);
        BtnSubmit.setText("Fix & Resubmit");
    }
}
