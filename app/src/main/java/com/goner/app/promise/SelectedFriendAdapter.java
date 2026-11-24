package com.goner.app.promise;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.R;
import com.goner.app.common.AvatarView;
import com.goner.app.data.model.Friend;

import java.util.List;

// 위쪽 가로 칩 (선택한 친구들)
public class SelectedFriendAdapter extends RecyclerView.Adapter<SelectedFriendAdapter.ViewHolder> {

    public interface OnRemoveListener {
        void onRemove(Friend friend);
    }

    private final List<Friend> selected; // draft.selectedFriends 그대로 씀
    private final OnRemoveListener listener;

    public SelectedFriendAdapter(List<Friend> selected, OnRemoveListener listener) {
        this.selected = selected;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = selected.get(position);
        holder.avatar.setUser(friend.getFriendUid(), friend.getNickname(), friend.getPhotoUrl());
        holder.name.setText(friend.getNickname());
        holder.remove.setContentDescription(holder.itemView.getContext()
                .getString(R.string.create_friends_remove, friend.getNickname()));
        holder.remove.setOnClickListener(v -> listener.onRemove(friend));
    }

    @Override
    public int getItemCount() {
        return selected.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        AvatarView avatar;
        TextView name;
        View remove;

        ViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.text_name);
            remove = itemView.findViewById(R.id.button_remove);
        }
    }
}
