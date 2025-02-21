package com.project.vortex;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

public class CustomSnapHelper extends LinearSnapHelper {
    private int verticalOffset;
    public CustomSnapHelper(int verticalOffset){
        this.verticalOffset = verticalOffset;
    }

    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView){
        int[] out = super.calculateDistanceToFinalSnap(layoutManager, targetView);
        if(layoutManager.canScrollVertically()){

            int position = layoutManager.getPosition(targetView);
            if(position == 0) {
                out[1] = out[1] - verticalOffset/2 - 100;
            } else {
                out[1] = out[1] - verticalOffset;
            }
        }
        return out;

    }
}
