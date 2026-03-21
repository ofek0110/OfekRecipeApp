package com.example.ofek.screens;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.ofek.R;
import com.example.ofek.adapters.FoodTokAdapter;
import com.example.ofek.models.FoodTokPost;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FoodTokActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private FoodTokAdapter adapter;
    private List<FoodTokPost> postList;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_tok);

        viewPager = findViewById(R.id.viewPagerFoodTok);
        postList = new ArrayList<>();
        
        adapter = new FoodTokAdapter(postList, this::showFullRecipeBottomSheet);
        viewPager.setAdapter(adapter);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        databaseReference = FirebaseDatabase.getInstance().getReference("foodtok_posts");
        loadFoodTokPosts();
    }

    private void loadFoodTokPosts() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    FoodTokPost post = data.getValue(FoodTokPost.class);
                    if (post != null) {
                        postList.add(post);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FoodTokActivity.this, "Failed to load feed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFullRecipeBottomSheet(FoodTokPost post) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.layout_recipe_bottom_sheet);
        
        // You can find views in bottomSheetDialog and set dummy data
        // For now, it just shows the layout
        
        bottomSheetDialog.show();
    }
}
