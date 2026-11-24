package com.goner.app.friend;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.R;
import com.goner.app.common.AvatarView;
import com.goner.app.common.TimeText;
import com.goner.app.data.model.FriendRequest;

import java.util.ArrayList;
import java.util.List;

// 친구 요청 목록. sent=true 면 보낸 요청(상대 = to*, 요청 취소), false 면 받은 요청(상대 = from*, 수락/거절)
public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    public interface Listener {
        void onCancel(FriendRequest request);

        void onAccept(FriendRequest request);

        void onReject(FriendRequest request);
    }

    private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    private final List<FriendRequest> items = new ArrayList<>();
    private final Listener listener;
    private boolean sent;

    public FriendRequestAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<FriendRequest> list, boolean sent) {
        this.sent = sent;
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public void remove(FriendRequest request) {
        items.remove(request);
        notifyDataSetChanged();
    }

    // 회전 후 확인창 결과 받을 때 id 로 다시 찾음
    public FriendRequest find(String id) {
        for (FriendRequest r : items) {
            if (r.getId().equals(id)) return r;
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        FriendRequest r = items.get(position);
        Context context = h.itemView.getContext();

        String uid = sent ? r.getToUid() : r.getFromUid();
        String name = sent ? r.getToNickname() : r.getFromNickname();
        String code = sent ? r.getToFriendCode() : r.getFromFriendCode();
        h.avatar.setUser(uid, name, sent ? r.getToPhotoUrl() : r.getFromPhotoUrl());
        h.name.setText(name);

        // "@code · 방금 전 보냄", 하루 안 남았으면 " · 내일 만료"
        String ago = TimeText.ago(context, r.getCreatedAt().toDate());
        String info = context.getString(sent ? R.string.request_sent_info : R.string.request_received_info, code, ago);
        long left = r.getExpiresAt().toDate().getTime() - System.currentTimeMillis();
        if (left < ONE_DAY) info = context.getString(R.string.request_expire_soon, info);
        h.info.setText(info);

        h.cancel.setVisibility(sent ? View.VISIBLE : View.GONE);
        h.accept.setVisibility(sent ? View.GONE : View.VISIBLE);
        h.reject.setVisibility(sent ? View.GONE : View.VISIBLE);
        h.cancel.setOnClickListener(v -> listener.onCancel(r));
        h.accept.setOnClickListener(v -> listener.onAccept(r));
        h.reject.setOnClickListener(v -> listener.onReject(r));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final AvatarView avatar;
        final TextView name, info, cancel, accept, reject;

        ViewHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.avatar);
            name = v.findViewById(R.id.text_name);
            info = v.findViewById(R.id.text_info);
            cancel = v.findViewById(R.id.button_cancel);
            accept = v.findViewById(R.id.button_accept);
            reject = v.findViewById(R.id.button_reject);
        }
    }
}
