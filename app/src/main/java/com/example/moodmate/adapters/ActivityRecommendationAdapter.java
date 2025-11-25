package com.example.moodmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmate.R;
import com.example.moodmate.models.ActivityRecommendation;

import java.util.List;

public class ActivityRecommendationAdapter extends RecyclerView.Adapter<ActivityRecommendationAdapter.ViewHolder> {
    private List<ActivityRecommendation> recommendations;
    private OnRecommendationClickListener listener;

    public interface OnRecommendationClickListener {
        void onRecommendationClick(ActivityRecommendation recommendation);
    }

    public ActivityRecommendationAdapter(List<ActivityRecommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public void setOnRecommendationClickListener(OnRecommendationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityRecommendation recommendation = recommendations.get(position);
        holder.bind(recommendation);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecommendationClick(recommendation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    public void updateRecommendations(List<ActivityRecommendation> newRecommendations) {
        this.recommendations = newRecommendations;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView iconText;
        private TextView titleText;
        private TextView descriptionText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconText = itemView.findViewById(R.id.recommendation_icon);
            titleText = itemView.findViewById(R.id.recommendation_title);
            descriptionText = itemView.findViewById(R.id.recommendation_description);
        }

        public void bind(ActivityRecommendation recommendation) {
            iconText.setText(recommendation.getIcon());
            titleText.setText(recommendation.getTitle());
            // Set description as plain text to avoid any formatting issues
            descriptionText.setText(recommendation.getDescription());
            descriptionText.setAutoLinkMask(0); // Disable auto-linking
        }
    }
}