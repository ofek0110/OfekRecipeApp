package com.example.ofek.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ofek.R;
import com.example.ofek.models.User;
import com.example.ofek.screens.UserProfile;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

// אדפטר לניהול והצגת רשימת משתמשים (טוב למסכי אדמין)
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    // הגדרה להאזנה ללחיצות על משתמש ברשימה
    public interface OnUserClickListener {
        void onUserClick(User user);
        void onLongUserClick(User user);
    }

    private final List<User> userList;
    private final OnUserClickListener onUserClickListener;

    public UserAdapter(@Nullable final OnUserClickListener onUserClickListener) {
        userList = new ArrayList<>();
        this.onUserClickListener = onUserClickListener;
    }

    @NonNull
    @Override
    public UserAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        if (user == null) return;

        // שיבוץ פרטי המשתמש ב-UI
        holder.tvName.setText(user.getFirstname() + " " + user.getLastname());
        holder.tvEmail.setText(user.getEmail());
        holder.tvPhone.setText(user.getPhone());

        // יצירת ראשי תיבות לתמונת הפרופיל הדיפולטיבית (למשל: Ofek Cohen -> OC)
        String initials = "";
        if (user.getFirstname() != null && !user.getFirstname().isEmpty()) {
            initials += user.getFirstname().charAt(0);
        }
        if (user.getLastname() != null && !user.getLastname().isEmpty()) {
            initials += user.getLastname().charAt(0);
        }
        holder.tvInitials.setText(initials.toUpperCase());

        // אם המשתמש אדמין - מציגים תג "Admin", אחרת מסטירים
        if (user.isAdmin()) {
            holder.chipRole.setVisibility(View.VISIBLE);
            holder.chipRole.setText("Admin");
        } else {
            holder.chipRole.setVisibility(View.GONE);
        }

        // לחיצה רגילה: מעבירה ישירות למסך הפרופיל של אותו משתמש עם ה-UID שלו
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), UserProfile.class);
            intent.putExtra("USER_UID", user.getId());
            v.getContext().startActivity(intent);

            if (onUserClickListener != null) {
                onUserClickListener.onUserClick(user);
            }
        });

        // לחיצה ארוכה: מפעילה את הליסנר (למשל לפתיחת תפריט מחיקה/עריכה)
        holder.itemView.setOnLongClickListener(v -> {
            if (onUserClickListener != null) {
                onUserClickListener.onLongUserClick(user);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // עדכון כל הרשימה מחדש
    public void setUserList(List<User> users) {
        userList.clear();
        userList.addAll(users);
        notifyDataSetChanged();
    }

    // הוספת משתמש בודד לרשימה ועדכון האנימציה בסוף
    public void addUser(User user) {
        userList.add(user);
        notifyItemInserted(userList.size() - 1);
    }

    // עדכון פרטי משתמש קיים ברשימה
    public void updateUser(User user) {
        int index = userList.indexOf(user);
        if (index == -1) return;
        userList.set(index, user);
        notifyItemChanged(index);
    }

    // מחיקת משתמש מהרשימה
    public void removeUser(User user) {
        int index = userList.indexOf(user);
        if (index == -1) return;
        userList.remove(index);
        notifyItemRemoved(index);
    }

    // מחזיק ה-Views של שורת המשתמש
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvPhone, tvInitials;
        Chip chipRole;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_user_name);
            tvEmail = itemView.findViewById(R.id.tv_item_user_email);
            tvPhone = itemView.findViewById(R.id.tv_item_user_phone);
            tvInitials = itemView.findViewById(R.id.tv_user_initials);
            chipRole = itemView.findViewById(R.id.chip_user_role);
        }
    }
}