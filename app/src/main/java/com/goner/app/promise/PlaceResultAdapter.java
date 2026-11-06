package com.goner.app.promise;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.R;
import com.goner.app.data.kakao.PlaceResponse;

import java.util.ArrayList;
import java.util.List;

// 장소 검색 결과 목록
public class PlaceResultAdapter extends RecyclerView.Adapter<PlaceResultAdapter.ViewHolder> {

    public interface OnPlaceClickListener {
        void onPlaceClick(PlaceResponse.Place place);
    }

    private final List<PlaceResponse.Place> items = new ArrayList<>();
    private final OnPlaceClickListener listener;

    public PlaceResultAdapter(OnPlaceClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<PlaceResponse.Place> places) {
        items.clear();
        items.addAll(places);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceResponse.Place place = items.get(position);
        holder.name.setText(place.place_name);
        holder.address.setText(place.getAddress());
        holder.itemView.setOnClickListener(v -> listener.onPlaceClick(place));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, address;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_name);
            address = itemView.findViewById(R.id.text_address);
        }
    }
}
