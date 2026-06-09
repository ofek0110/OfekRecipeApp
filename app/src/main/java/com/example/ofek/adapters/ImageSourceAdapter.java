package com.example.ofek.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ofek.R;
import com.example.ofek.models.ImageSourceOption;

import java.util.List;

/// אדפטר בשביל הדיאלוג של בחירת תמונה (מצלמה או גלריה)
public class ImageSourceAdapter extends ArrayAdapter<ImageSourceOption> {

    // ממשק כדי שאוכל לדעת מתי המשתמש לחץ על אופציה כלשהי
    public interface OnImageSourceSelectedListener {
        void onImageSourceSelected(ImageSourceOption option);
    }

    private final LayoutInflater inflater;
    private final List<ImageSourceOption> objects;
    private OnImageSourceSelectedListener listener;

    public ImageSourceAdapter(@NonNull Context context, @NonNull List<ImageSourceOption> objects,
                              @NonNull OnImageSourceSelectedListener listener) {
        super(context, R.layout.row_image_source, objects);
        this.inflater = LayoutInflater.from(context);
        this.objects = objects;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        // מחזיר כמה אופציות יש ברשימה
        return objects.size();
    }

    @Nullable
    @Override
    public ImageSourceOption getItem(int position) {
        // מביא את האופציה שנמצאת במיקום הספציפי הזה
        return objects.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // אם אין View קיים שאפשר למחזר, יוצר אחד חדש מה-XML
        if (convertView == null) {
            convertView = this.inflater.inflate(R.layout.row_image_source, parent, false);
        }

        // מקשר את הרכיבים מה-layout
        ImageView icon = convertView.findViewById(R.id.icon_dialog_item);
        TextView title = convertView.findViewById(R.id.text_dialog_item);
        TextView description = convertView.findViewById(R.id.text_dialog_item_description);

        // לוקח את האופציה הנוכחית
        ImageSourceOption item = getItem(position);

        if (item != null) {
            // מעדכן את הטקסטים והאייקון בהתאם למה שהגדרתי באובייקט
            title.setText(item.getTitle());
            description.setText(item.getDescription());
            icon.setImageResource(item.getIconResource());
        }

        // ברגע שלוחצים על שורה, אני שולח את הבחירה למי שמקשיב (למשל האקטיביטי שפתח את הדיאלוג)
        convertView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageSourceSelected(item);
            }
        });

        return convertView;
    }
}