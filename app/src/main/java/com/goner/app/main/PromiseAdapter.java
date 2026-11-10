package com.goner.app.main;

import android.content.Context;
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
import com.goner.app.data.DbValues;
import com.goner.app.data.model.Appointment;
import com.goner.app.data.model.Participant;

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
        void onMenu(Appointment appointment);
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("M월 d일 (E) HH:mm", Locale.KOREA);
    private final List<Appointment> items = new ArrayList<>();
    private final Map<String, List<Participant>> participants = new HashMap<>(); // 약속 id → 참여자
    private final String myUid;
    private final OnPromiseMenuListener listener;

    public PromiseAdapter(String myUid, OnPromiseMenuListener listener) {
        this.myUid = myUid;
        this.listener = listener;
    }

    public void setItems(List<Appointment> appointments) {
        items.clear();
        items.addAll(appointments);
        notifyDataSetChanged();
    }

    public void putParticipants(String appointmentId, List<Participant> list) {
        participants.put(appointmentId, list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = items.get(position);
        holder.title.setText(appointment.getTitle());

        String time = appointment.getAppointmentTime() == null ? "" : dateFormat.format(appointment.getAppointmentTime().toDate());
        String place = appointment.getPlaceName() == null ? "" : appointment.getPlaceName();
        holder.info.setText(holder.itemView.getContext().getString(R.string.home_promise_info, time, place));

        // 거절한 사람 빼고 참여자 / 수락한 사람 수 셈 (참여자 아직 못 불러왔으면 빈 목록)
        List<Participant> members = new ArrayList<>();
        int joined = 0;
        boolean meJoined = false;
        List<Participant> loaded = participants.get(appointment.getId());
        if (loaded != null) {
            for (Participant p : loaded) {
                if (DbValues.PARTICIPANT_REJECTED.equals(p.getStatus())) continue;
                members.add(p);
                if (DbValues.PARTICIPANT_JOINED.equals(p.getStatus())) {
                    joined++;
                    if (myUid.equals(p.getUid())) meJoined = true;
                }
            }
        }

        boolean isHost = myUid.equals(appointment.getHostUid());
        Context context = holder.itemView.getContext();
        if (!isHost && meJoined) {
            // 초대받아서 참여한 약속 (Figma 370:342)
            holder.chip.setVisibility(View.VISIBLE);
            holder.chip.setText(R.string.home_chip_joined);
            holder.chip.setBackgroundResource(R.drawable.bg_pill_soft);
            holder.chip.setTextColor(context.getColor(R.color.primary));
        } else if (joined < members.size()) {
            holder.chip.setVisibility(View.VISIBLE);
            holder.chip.setText(context.getString(R.string.home_pending, joined, members.size()));
            holder.chip.setBackgroundResource(R.drawable.bg_chip);
            holder.chip.setTextColor(context.getColor(R.color.text_body));
        } else {
            holder.chip.setVisibility(View.GONE);
        }

        holder.avatars.removeAllViews();
        int gap = holder.itemView.getResources().getDimensionPixelSize(R.dimen.promise_avatar_gap);
        for (Participant p : members) {
            AvatarView avatar = new AvatarView(holder.itemView.getContext());
            avatar.setUser(p.getUid(), p.getNickname(), p.getPhotoUrl());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(gap);
            holder.avatars.addView(avatar, lp);
        }

        holder.more.setVisibility(isHost ? View.VISIBLE : View.INVISIBLE);
        holder.more.setOnClickListener(v -> listener.onMenu(appointment));
        holder.itemView.setOnLongClickListener(v -> {
            if (!isHost) return false;
            listener.onMenu(appointment);
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
