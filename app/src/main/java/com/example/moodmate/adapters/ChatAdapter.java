package com.example.moodmate.adapters;

import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmate.R;
import com.example.moodmate.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;
    private static final int VIEW_TYPE_TYPING = 3;
    
    private List<ChatMessage> messages;
    private SimpleDateFormat timeFormat;
    
    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }
    
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.isTyping()) {
            return VIEW_TYPE_TYPING;
        }
        return message.isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }
    
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        
        if (viewType == VIEW_TYPE_USER) {
            view = inflater.inflate(R.layout.item_chat_user, parent, false);
        } else if (viewType == VIEW_TYPE_TYPING) {
            view = inflater.inflate(R.layout.item_chat_typing, parent, false);
        } else {
            view = inflater.inflate(R.layout.item_chat_ai, parent, false);
        }
        
        return new ChatViewHolder(view, viewType);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        if (message.isTyping()) {
            holder.startTypingAnimation();
        } else {
            if (holder.messageText != null) {
                holder.messageText.setText(message.getMessage());
            }
            if (holder.timeText != null) {
                holder.timeText.setText(timeFormat.format(new Date(message.getTimestamp())));
            }
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        TextView dot1, dot2, dot3;
        Handler animationHandler;
        Runnable animationRunnable;
        boolean isAnimating = false;
        
        ChatViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            
            if (viewType == VIEW_TYPE_TYPING) {
                dot1 = itemView.findViewById(R.id.dot1);
                dot2 = itemView.findViewById(R.id.dot2);
                dot3 = itemView.findViewById(R.id.dot3);
                animationHandler = new Handler(Looper.getMainLooper());
            } else {
                messageText = itemView.findViewById(R.id.message_text);
                timeText = itemView.findViewById(R.id.time_text);
            }
        }
        
        void startTypingAnimation() {
            if (isAnimating || dot1 == null) return;
            
            isAnimating = true;
            animationRunnable = new Runnable() {
                int currentDot = 0;
                
                @Override
                public void run() {
                    if (!isAnimating) return;
                    
                    // Reset all dots
                    dot1.setAlpha(0.3f);
                    dot2.setAlpha(0.3f);
                    dot3.setAlpha(0.3f);
                    
                    // Highlight current dot
                    switch (currentDot) {
                        case 0:
                            dot1.setAlpha(1.0f);
                            break;
                        case 1:
                            dot2.setAlpha(1.0f);
                            break;
                        case 2:
                            dot3.setAlpha(1.0f);
                            break;
                    }
                    
                    currentDot = (currentDot + 1) % 3;
                    animationHandler.postDelayed(this, 500);
                }
            };
            animationHandler.post(animationRunnable);
        }
        
        void stopTypingAnimation() {
            isAnimating = false;
            if (animationHandler != null && animationRunnable != null) {
                animationHandler.removeCallbacks(animationRunnable);
            }
        }
    }
}