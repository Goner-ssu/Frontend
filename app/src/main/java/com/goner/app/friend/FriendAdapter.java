package com.goner.app.friend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.R;
import com.goner.app.common.AvatarView;
import com.goner.app.data.model.Friend;

import java.util.ArrayList;
import java.util.List;

// 친구 탭 친구 목록
public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    // ··· 눌렀을 때
    public interface OnMoreListener {
        void onMore(Friend friend);
    }

    private final List<Friend> items = new ArrayList<>();
    private final OnMoreListener listener;

    public FriendAdapter(OnMoreListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Friend> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Friend f = items.get(position);
        h.avatar.setUser(f.getFriendUid(), f.getNickname(), f.getPhotoUrl());
        h.name.setText(f.getNickname());
        h.more.setOnClickListener(v -> listener.onMore(f));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final AvatarView avatar;
        final TextView name, more;

        ViewHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.avatar);
            name = v.findViewById(R.id.text_name);
            more = v.findViewById(R.id.button_more);
        }
    }
}
