package com.goner.app.promise;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.R;
import com.goner.app.common.AvatarView;
import com.goner.app.data.model.Friend;

import java.util.ArrayList;
import java.util.List;

// 친구 목록 (누르면 선택/해제, 오른쪽 동그라미로 표시)
public class FriendCheckAdapter extends RecyclerView.Adapter<FriendCheckAdapter.ViewHolder> {

    public interface OnFriendClickListener {
        void onFriendClick(Friend friend);
    }

    private final List<Friend> items = new ArrayList<>();
    private final List<Friend> selected; // draft.selectedFriends 그대로 씀
    private final OnFriendClickListener listener;

    public FriendCheckAdapter(List<Friend> selected, OnFriendClickListener listener) {
        this.selected = selected;
        this.listener = listener;
    }

    public void setItems(List<Friend> friends) {
        items.clear();
        items.addAll(friends);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_check, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = items.get(position);
        holder.avatar.setUser(friend.getFriendUid(), friend.getNickname(), friend.getPhotoUrl());
        holder.name.setText(friend.getNickname());
        holder.code.setText(holder.itemView.getContext().getString(R.string.create_friends_code, friend.getFriendCode()));
        boolean selected = isSelected(friend);
        holder.check.setBackgroundResource(selected ? R.drawable.bg_check_on : R.drawable.bg_check_off);
        holder.check.setImageResource(selected ? R.drawable.ic_check_white : 0);
        holder.itemView.setOnClickListener(v -> listener.onFriendClick(friend));
    }

    private boolean isSelected(Friend friend) {
        for (Friend f : selected) {
            if (f.getFriendUid().equals(friend.getFriendUid())) return true;
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        AvatarView avatar;
        TextView name, code;
        ImageView check;

        ViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.text_name);
            code = itemView.findViewById(R.id.text_code);
            check = itemView.findViewById(R.id.view_check);
        }
    }
}
