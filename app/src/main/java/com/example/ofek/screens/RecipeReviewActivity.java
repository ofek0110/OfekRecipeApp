package com.example.ofek.screens;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ofek.R;
import com.example.ofek.models.Recipe;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RecipeReviewActivity extends AppCompatActivity {

    private Recipe recipe;
    private TextView tvTitle, tvDescription, tvIngredients, tvInstructions;
    private TextInputEditText etAdminNotes;
    private Button btnApprove, btnReject;
    private DatabaseReference recipeRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_review);

        recipe = (Recipe) getIntent().getSerializableExtra("recipe");
        if (recipe == null) {
            finish();
            return;
        }

        recipeRef = FirebaseDatabase.getInstance().getReference("recipes").child(recipe.getId());

        initializeViews();
        displayRecipeData();

        btnApprove.setOnClickListener(v -> updateRecipeStatus(true));
        btnReject.setOnClickListener(v -> updateRecipeStatus(false));
    }

    private void initializeViews() {
        tvTitle = findViewById(R.id.tvReviewTitle);
        tvDescription = findViewById(R.id.tvReviewDescription);
        tvIngredients = findViewById(R.id.tvReviewIngredients);
        tvInstructions = findViewById(R.id.tvReviewInstructions);
        etAdminNotes = findViewById(R.id.etAdminNotes);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
    }

    private void displayRecipeData() {
        tvTitle.setText(recipe.getTitle());
        tvDescription.setText(recipe.getDescription());
        tvIngredients.setText(recipe.getIngredients());
        tvInstructions.setText(recipe.getInstructions());
        etAdminNotes.setText(recipe.getAdminNotes());
    }

    private void updateRecipeStatus(boolean approve) {
        String notes = etAdminNotes.getText().toString().trim();
        
        recipeRef.child("approved").setValue(approve);
        recipeRef.child("adminNotes").setValue(notes)
            .addOnSuccessListener(aVoid -> {
                String msg = approve ? "Recipe Approved!" : "Recipe Rejected";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                finish();
            });
    }
}
