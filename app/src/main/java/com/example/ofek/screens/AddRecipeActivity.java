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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ofek.R;
import com.example.ofek.adapters.ImageSourceAdapter;
import com.example.ofek.models.ImageSourceOption;
import com.example.ofek.models.Recipe;
import com.example.ofek.models.User;
import com.example.ofek.services.DatabaseService;
import com.example.ofek.utils.ImageUtil;
import com.example.ofek.utils.SharedPreferencesUtil;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;

// מסך ליצירת מתכון חדש או עריכה ותיקון של מתכון קיים שנדחה על ידי האדמין
public class AddRecipeActivity extends AppCompatActivity {

    // הגדרת משתני ה-UI והמשתנים הגלובליים לניהול המצב (State) במסך
    private TextInputEditText EtTitle, EtDescription, EtIngredients, EtInstructions, EtPrepTime;
    private AutoCompleteTextView ActvDifficulty, ActvCategory;
    private Button BtnSubmit;
    private MaterialButton BtnViewRejectionReason;
    private ImageView IvRecipePreview;
    private TextView TvAddImageHint;
    private MaterialCardView CardSelectImage;

    private User currentUser;
    private int selectedHour = 0;
    private int selectedMinute = 0;
    private boolean isImageChanged = false; // דגל לבדיקה אם המשתמש החליף את התמונה בזמן העריכה

    // רכיבי מערכת לקבלת תמונות (מצלמה/גלריה) בצורה המודרנית של אנדרואיד
    private ActivityResultLauncher<Intent> selectImageLauncher;
    private ActivityResultLauncher<Intent> captureImageLauncher;

    private Recipe recipeToEdit = null; // ישמור את אובייקט המתכון במידה ונכנסנו למצב עריכה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        currentUser = SharedPreferencesUtil.getUser(this);
        ImageUtil.requestPermission(this);

        initializeViews();
        setupDropdowns();
        setupClickListeners();

        // בדיקה האם הגענו למסך לצורך עריכת מתכון קיים. אם כן, נשלוף אותו מה-DB ונמלא את הטופס
        if (getIntent().hasExtra("RECIPE_ID_TO_EDIT")) {
            String recipeIdToEdit = getIntent().getStringExtra("RECIPE_ID_TO_EDIT");
            assert recipeIdToEdit != null;
            DatabaseService.getInstance().getRecipe(recipeIdToEdit, new DatabaseService.DatabaseCallback<Recipe>() {
                @Override
                public void onCompleted(@Nullable Recipe recipe) {
                    recipeToEdit = recipe;
                    fillFormForEdit();
                }

                @Override
                public void onFailed(Exception e) {}
            });
        }

        // הגדרת קולט התשובה (Launcher) עבור בחירת תמונה מהגלריה
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        IvRecipePreview.setImageURI(selectedImage);
                        TvAddImageHint.setVisibility(View.GONE);
                        IvRecipePreview.setTag(null);
                        isImageChanged = true;
                    }
                });

        // הגדרת קולט התשובה (Launcher) עבור צילום תמונה מהמצלמה
        captureImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                        IvRecipePreview.setImageBitmap(bitmap);
                        TvAddImageHint.setVisibility(View.GONE);
                        IvRecipePreview.setTag(null);
                        isImageChanged = true;
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
        ActvCategory = findViewById(R.id.ActvCategory);

        BtnSubmit = findViewById(R.id.BtnSubmitRecipe);
        IvRecipePreview = findViewById(R.id.IvRecipePreview);
        TvAddImageHint = findViewById(R.id.TvAddImageHint);
        CardSelectImage = findViewById(R.id.CardSelectImage);
        BtnViewRejectionReason = findViewById(R.id.BtnViewRejectionReason);
    }

    // אכלוס רשימות הבחירה (Spinner/Dropdown) של רמות קושי וקטגוריות עם ערכי ברירת מחדל
    private void setupDropdowns() {
        String[] difficulties = new String[] {"Easy", "Medium", "Hard"};
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, difficulties);
        ActvDifficulty.setAdapter(difficultyAdapter);
        ActvDifficulty.setText(difficulties[1], false);

        if (ActvCategory != null) {
            String[] categories = new String[] {"Breakfast", "Lunch", "Vegan", "Desserts", "Dinner", "General"};
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
            ActvCategory.setAdapter(categoryAdapter);
            ActvCategory.setText(categories[5], false);
        }
    }

    private void setupClickListeners() {
        CardSelectImage.setOnClickListener(v -> showImageSourceDialog());
        BtnSubmit.setOnClickListener(v -> submitRecipe());
        EtPrepTime.setOnClickListener(v -> showTimePickerWheel());
    }

    // פתיחת דיאלוג לבחירת זמן הכנה (שעות ודקות) ועדכון שדה הטקסט בהתאם לבחירה
    private void showTimePickerWheel() {
        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                new android.app.TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(android.widget.TimePicker view, int hourOfDay, int minute) {
                        selectedHour = hourOfDay;
                        selectedMinute = minute;

                        String timeText = "";
                        if (selectedHour > 0) {
                            timeText += selectedHour + " שעות ";
                        }
                        if (selectedMinute > 0 || selectedHour == 0) {
                            timeText += selectedMinute + " דקות";
                        }

                        EtPrepTime.setText(timeText.trim());
                    }
                },
                selectedHour,
                selectedMinute,
                true
        );

        if (timePickerDialog.getWindow() != null) {
            timePickerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        timePickerDialog.setTitle("בחר זמן הכנה:");
        timePickerDialog.show();
    }

    private void captureImageFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        captureImageLauncher.launch(takePictureIntent);
    }

    // פתיחת תפריט תחתון (Bottom Sheet) שמאפשר למשתמש לבחור בין העלאה מהגלריה לצילום במצלמה
    private void showImageSourceDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_image_source, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        final ArrayList<ImageSourceOption> options = new ArrayList<>();
        options.add(new ImageSourceOption(getString(R.string.gallery_title), getString(R.string.gallery_description), R.drawable.gallery_thumbnail));
        options.add(new ImageSourceOption(getString(R.string.camera_title), getString(R.string.camera_description), R.drawable.photo_camera));

        ListView listView = bottomSheetView.findViewById(R.id.list_view_image_sources);
        ImageSourceAdapter adapter = new ImageSourceAdapter(this, options, option -> {
            bottomSheetDialog.dismiss();
            if (option.getTitle().equals(getString(R.string.gallery_title))) {
                selectImageFromGallery();
            } else if (option.getTitle().equals(getString(R.string.camera_title))) {
                captureImageFromCamera();
            }
        });
        listView.setAdapter(adapter);

        bottomSheetDialog.show();
    }

    // טעינת נתוני המתכון הקיים לתוך שדות הטופס והצגת כפתור לצפייה בהערות הדחייה של האדמין (במצב עריכה)
    private void fillFormForEdit() {
        EtTitle.setText(recipeToEdit.getTitle());
        EtDescription.setText(recipeToEdit.getDescription());
        EtIngredients.setText(recipeToEdit.getIngredients());
        EtInstructions.setText(recipeToEdit.getInstructions());
        EtPrepTime.setText(recipeToEdit.getPreparationTime());
        ActvDifficulty.setText(recipeToEdit.getDifficulty(), false);

        if (ActvCategory != null && recipeToEdit.getCategory() != null) {
            ActvCategory.setText(recipeToEdit.getCategory(), false);
        }

        if (recipeToEdit.getImageBase64() != null && !recipeToEdit.getImageBase64().isEmpty()) {
            IvRecipePreview.setImageBitmap(ImageUtil.convertFrom64base(recipeToEdit.getImageBase64()));
            TvAddImageHint.setVisibility(View.GONE);
        } else {
            TvAddImageHint.setVisibility(View.VISIBLE);
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

    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selectImageLauncher.launch(intent);
    }

    // איסוף הנתונים מהטופס, ביצוע וולידציה כולל בדיקה אם בוצע שינוי כלשהו במצב עריכה, ושמירה ב-Firebase
    private void submitRecipe() {
        String title = EtTitle.getText().toString().trim();
        String description = EtDescription.getText().toString().trim();
        String ingredients = EtIngredients.getText().toString().trim();
        String instructions = EtInstructions.getText().toString().trim();
        String prepTime = EtPrepTime.getText().toString().trim();
        String difficulty = ActvDifficulty.getText().toString().trim();

        String category = "General";
        if (ActvCategory != null) {
            category = ActvCategory.getText().toString().trim();
        }

        if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty() || instructions.isEmpty() || prepTime.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // אם אנחנו במצב עריכה, נבדוק אם המשתמש בכלל שינה משהו לפני שנותנים לו להגיש מחדש
        if (recipeToEdit != null) {
            String oldTitle = recipeToEdit.getTitle() != null ? recipeToEdit.getTitle().trim() : "";
            String oldDesc = recipeToEdit.getDescription() != null ? recipeToEdit.getDescription().trim() : "";
            String oldIng = recipeToEdit.getIngredients() != null ? recipeToEdit.getIngredients().trim() : "";
            String oldInst = recipeToEdit.getInstructions() != null ? recipeToEdit.getInstructions().trim() : "";
            String oldPrep = recipeToEdit.getPreparationTime() != null ? recipeToEdit.getPreparationTime().trim() : "";
            String oldDiff = recipeToEdit.getDifficulty() != null ? recipeToEdit.getDifficulty().trim() : "Medium";
            String oldCat = recipeToEdit.getCategory() != null ? recipeToEdit.getCategory().trim() : "General";

            if (title.equals(oldTitle) && description.equals(oldDesc) &&
                    ingredients.equals(oldIng) && instructions.equals(oldInst) &&
                    prepTime.equals(oldPrep) && difficulty.equals(oldDiff) &&
                    category.equals(oldCat) && !isImageChanged) {

                Toast.makeText(this, "No changes detected. Please update the recipe before resubmitting.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        //  אם התמונה לא הוחלפה בעריכה, נשמור על ה-Base64 הקיים כדי לחסוך עיבוד מיותר
        String imageBase64;
        if (recipeToEdit != null && !isImageChanged) {
            imageBase64 = recipeToEdit.getImageBase64();
        } else {
            imageBase64 = ImageUtil.convertTo64Base(IvRecipePreview);
        }

        String recipeId;
        if (recipeToEdit != null) {
            recipeId = recipeToEdit.getId();
        } else {
            recipeId = DatabaseService.getInstance().generateRecipeId();
        }

        // בנייה או עדכון של אובייקט המתכון (הסטטוס מאופס ל-false כדי שיעבור שוב אישור אדמין)
        Recipe newRecipe = new Recipe(
                recipeId,
                title,
                description,
                ingredients,
                instructions,
                imageBase64,
                currentUser.getId(),
                category,
                prepTime,
                difficulty,
                false,
                null,
                recipeToEdit != null ? recipeToEdit.getRating() : 0f,
                recipeToEdit != null ? recipeToEdit.getNumRatings() : 0,
                recipeToEdit != null ? recipeToEdit.getRaters() : new HashMap<>()
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