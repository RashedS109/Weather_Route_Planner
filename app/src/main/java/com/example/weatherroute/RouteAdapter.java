package com.example.weatherroute;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.weatherroute.databinding.ItemRouteStepBinding;
import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    private List<RouteStep> stepList;

    public RouteAdapter(List<RouteStep> stepList) {
        this.stepList = stepList;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRouteStepBinding binding = ItemRouteStepBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new RouteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        RouteStep step = stepList.get(position);
        holder.binding.tvStepSegment.setText(step.getSegment());
        holder.binding.tvDistance.setText(step.getDistance());
        holder.binding.tvWeatherCondition.setText(step.getWeatherCondition());
    }

    @Override
    public int getItemCount() {
        return stepList == null ? 0 : stepList.size();
    }

    // View Holder Class
    static class RouteViewHolder extends RecyclerView.ViewHolder {
        ItemRouteStepBinding binding;

        public RouteViewHolder(ItemRouteStepBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}