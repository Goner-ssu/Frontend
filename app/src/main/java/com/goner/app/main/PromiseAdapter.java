package com.goner.app.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.R;
import com.goner.app.common.AvatarView;
import com.goner.app.common.Promise;
import com.goner.app.common.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// 홈 약속 카드 목록
public class PromiseAdapter extends RecyclerView.Adapter<PromiseAdapter.ViewHolder> {

    // 방장만 (길게 누르기, ⋮)
    public interface OnPromiseMenuListener {
        void onMenu(Promise promise);
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("M월 d일 (E) HH:mm", Locale.KOREA);
    private final List<Promise> items = new ArrayList<>();
    private final Map<String, User> users = new HashMap<>(); // 참여자 이름/사진
    private final String myUid;
    private final OnPromiseMenuListener listener;

    public PromiseAdapter(String myUid, OnPromiseMenuListener listener) {
        this.myUid = myUid;
        this.listener = listener;
    }

    public void setItems(List<Promise> promises) {
        items.clear();
        items.addAll(promises);
        notifyDataSetChanged();
    }

    public void putUser(User user) {
        users.put(user.uid, user);
        notifyDataSetChanged();
    }

    public boolean hasUser(String uid) {
        return users.containsKey(uid);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Promise promise = items.get(position);
        holder.title.setText(promise.title);

        String time = promise.time == null ? "" : dateFormat.format(promise.time.toDate());
        String place = promise.place == null ? "" : promise.place.name;
        holder.info.setText(holder.itemView.getContext().getString(R.string.home_promise_info, time, place));

        // 거절한 사람 빼고 참여자 수 / 수락한 사람 수 셈
        List<String> memberUids = new ArrayList<>();
        int joined = 0;
        for (Map.Entry<String, String> member : promise.members.entrySet()) {
            if (Promise.MEMBER_REJECTED.equals(member.getValue())) continue;
            memberUids.add(member.getKey());
            if (Promise.MEMBER_JOINED.equals(member.getValue())) joined++;
        }

        if (joined < memberUids.size()) {
            holder.chip.setVisibility(View.VISIBLE);
            holder.chip.setText(holder.itemView.getContext()
                    .getString(R.string.home_pending, joined, memberUids.size()));
        } else {
            holder.chip.setVisibility(View.GONE);
        }

        holder.avatars.removeAllViews();
        int gap = holder.itemView.getResources().getDimensionPixelSize(R.dimen.promise_avatar_gap);
        for (String uid : memberUids) {
            AvatarView avatar = new AvatarView(holder.itemView.getContext());
            User user = users.get(uid);
            avatar.setUser(uid, user == null ? null : user.nickname, user == null ? null : user.photoUrl);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(gap);
            holder.avatars.addView(avatar, lp);
        }

        boolean isHost = myUid.equals(promise.hostUid);
        holder.more.setVisibility(isHost ? View.VISIBLE : View.INVISIBLE);
        holder.more.setOnClickListener(v -> listener.onMenu(promise));
        holder.itemView.setOnLongClickListener(v -> {
            if (!isHost) return false;
            listener.onMenu(promise);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, info, chip;
        ImageView more;
        LinearLayout avatars;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            info = itemView.findViewById(R.id.text_info);
            chip = itemView.findViewById(R.id.chip_pending);
            more = itemView.findViewById(R.id.button_more);
            avatars = itemView.findViewById(R.id.layout_avatars);
        }
    }
}
