package com.example.ofek.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ofek.R;
import com.example.ofek.models.FoodTokPost;

import java.util.List;

public class FoodTokAdapter extends RecyclerView.Adapter<FoodTokAdapter.ViewHolder> {

    private final List<FoodTokPost> postList;
    private final OnRecipeClickListener onRecipeClickListener;

    public interface OnRecipeClickListener {
        void onShowFullRecipe(FoodTokPost post);
    }

    public FoodTokAdapter(List<FoodTokPost> postList, OnRecipeClickListener listener) {
        this.postList = postList;
        this.onRecipeClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_tok, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(postList.get(position));
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.player != null) {
            holder.player.play();
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder.player != null) {
            holder.player.pause();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPost;
        PlayerView playerView;
        TextView tvChefName, tvRecipeTitle;
        Button btnViewFullRecipe;
        ExoPlayer player;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPost = itemView.findViewById(R.id.ivPost);
            playerView = itemView.findViewById(R.id.playerView);
            tvChefName = itemView.findViewById(R.id.tvChefName);
            tvRecipeTitle = itemView.findViewById(R.id.tvRecipeTitle);
            btnViewFullRecipe = itemView.findViewById(R.id.btnViewFullRecipe);
        }

        void bind(FoodTokPost post) {
            tvChefName.setText("@" + post.getChefName());
            tvRecipeTitle.setText(post.getRecipeTitle());

            if ("video".equalsIgnoreCase(post.getMediaType())) {
                ivPost.setVisibility(View.GONE);
                playerView.setVisibility(View.VISIBLE);
                setupVideoPlayer(post.getMediaUrl());
            } else {
                playerView.setVisibility(View.GONE);
                ivPost.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(post.getMediaUrl()).into(ivPost);
                releasePlayer();
            }

            btnViewFullRecipe.setOnClickListener(v -> {
                if (onRecipeClickListener != null) {
                    onRecipeClickListener.onShowFullRecipe(post);
                }
            });
        }

        private void setupVideoPlayer(String url) {
            if (player == null) {
                player = new ExoPlayer.Builder(itemView.getContext()).build();
                player.setRepeatMode(Player.REPEAT_MODE_ALL);
                playerView.setPlayer(player);
            }
            MediaItem mediaItem = MediaItem.fromUri(url);
            player.setMediaItem(mediaItem);
            player.prepare();
        }

        private void releasePlayer() {
            if (player != null) {
                player.release();
                player = null;
            }
        }
    }
}
