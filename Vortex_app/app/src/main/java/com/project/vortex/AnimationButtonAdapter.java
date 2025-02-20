package com.project.vortex;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AnimationButtonAdapter extends RecyclerView.Adapter<AnimationButtonAdapter.ViewHolder> {
    private List<AnimationItem> animationItems;
    private OnAnimationClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private final String TAG = "AnimationButtonAdapter";

    public AnimationButtonAdapter(List<AnimationItem> animationItems, OnAnimationClickListener listener) {
        this.animationItems = animationItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_animation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int actualPosition = position % animationItems.size();
        AnimationItem item = animationItems.get(actualPosition);
        holder.bind(item, actualPosition);
        if(actualPosition == selectedPosition){
            holder.button.setAlpha(1.0f);
            holder.button.setEnabled(true);
        } else {
            holder.button.setAlpha(0.3f);
            holder.button.setEnabled(false);
        }
    }

    public void setSelectedPosition(int position){
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount(){
        return animationItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        Button button;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.animationsButton);
        }

        public void bind(AnimationItem animationItems, int position) {
            button.setText(animationItems.getName());
            button.setOnClickListener(v ->{
                Log.d(TAG, "Button clicked: " + animationItems.getName() + " at index: " + position + " with command: " + animationItems.getCommand());
                if(listener != null){
                    listener.onAnimationSelected(animationItems.getCommand(), button);
                }
            });
        }
    }
    public interface OnAnimationClickListener{
        void onAnimationSelected(int command, Button clickedButton);
    }
}