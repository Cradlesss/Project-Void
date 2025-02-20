package com.project.vortex;

import android.util.DisplayMetrics;

import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

public class AggressiveSnapHelper extends LinearSnapHelper {
    private RecyclerView recyclerView;

    @Override
    public void attachToRecyclerView(RecyclerView recyclerView) throws IllegalStateException {
        this.recyclerView = recyclerView;
        super.attachToRecyclerView(recyclerView);
    }

    @Override
    public RecyclerView.SmoothScroller createScroller(RecyclerView.LayoutManager layoutManager) {
        if (recyclerView == null) {
            return null;
        }
        return new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            protected int calculateTimeForDeceleration(int dx) {
                int defaultTime = super.calculateTimeForDeceleration(dx);
                // Make snapping more aggressive by reducing deceleration time.
                return defaultTime;  // adjust factor as needed
            }
        };
    }
}
