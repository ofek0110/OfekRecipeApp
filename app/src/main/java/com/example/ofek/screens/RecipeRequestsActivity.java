package com.example.ofek.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.adapters.RecipeAdapter;
import com.example.ofek.models.Recipe;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RecipeRequestsActivity extends AppCompatActivity {

    private RecyclerView rvRequests;
    private RecipeAdapter adapter;
    private List<Recipe> requestList;
    private DatabaseReference recipesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_requests);

        rvRequests = findViewById(R.id.rvRecipeRequests);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        requestList = new ArrayList<>();
        adapter = new RecipeAdapter(new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                Intent intent = new Intent(RecipeRequestsActivity.this, RecipeReviewActivity.class);
                intent.putExtra("recipe", recipe);
                startActivity(intent);
            }

            @Override
            public void onLongRecipeClick(Recipe recipe) { }
        });

        rvRequests.setAdapter(adapter);
        loadRequests();
    }

    private void loadRequests() {
        recipesRef = FirebaseDatabase.getInstance().getReference("recipes");
        recipesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Recipe recipe = data.getValue(Recipe.class);
                    if (recipe != null && !recipe.isApproved()) {
                        requestList.add(recipe);
                    }
                }
                adapter.setRecipeList(requestList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RecipeRequestsActivity.this, "Error loading requests", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
