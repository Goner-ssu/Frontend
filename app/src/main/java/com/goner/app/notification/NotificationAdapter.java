package com.goner.app.notification;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.R;
import com.goner.app.common.AvatarView;
import com.goner.app.common.TimeText;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 알림 목록. 친구 요청은 한 줄, 약속 초대는 큰 카드(item_notification_invite). 편집 모드면 체크박스로 여러 개 선택
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface Listener {
        void onClick(NotificationItem item);

        void onAccept(NotificationItem item);

        void onReject(NotificationItem item);

        void onSelectionChanged(int count);
    }

    private static final int GROUP_TODAY = 0;
    private static final int GROUP_WEEK = 1;
    private static final int GROUP_OLDER = 2;

    private final Listener listener;
    private final List<NotificationItem> items = new ArrayList<>();
    private final Set<String> selectedKeys = new HashSet<>();
    private boolean editMode;

    public NotificationAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<NotificationItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public void remove(NotificationItem item) {
        items.remove(item);
        selectedKeys.remove(item.key);
        notifyDataSetChanged();
    }

    public List<NotificationItem> getItems() {
        return items;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        selectedKeys.clear();
        notifyDataSetChanged();
        listener.onSelectionChanged(0);
    }

    public boolean isAllSelected() {
        return !items.isEmpty() && selectedKeys.size() == items.size();
    }

    // 전체 선택 누르면 다 선택, 이미 다 선택돼 있으면 다 해제
    public void toggleSelectAll() {
        boolean selectAll = !isAllSelected();
        selectedKeys.clear();
        if (selectAll) {
            for (NotificationItem item : items) selectedKeys.add(item.key);
        }
        notifyDataSetChanged();
        listener.onSelectionChanged(selectedKeys.size());
    }

    public Set<String> getSelectedKeys() {
        return selectedKeys;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == NotificationItem.TYPE_INVITE ? R.layout.item_notification_invite : R.layout.item_notification;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        NotificationItem item = items.get(position);
        Context context = h.itemView.getContext();
        boolean invite = item.type == NotificationItem.TYPE_INVITE;

        // 그룹 제목은 그룹이 바뀌는 첫 줄에만
        int group = groupOf(item.time);
        boolean firstOfGroup = position == 0 || groupOf(items.get(position - 1).time) != group;
        h.textGroup.setVisibility(firstOfGroup ? View.VISIBLE : View.GONE);
        h.textGroup.setText(group == GROUP_TODAY ? R.string.noti_today
                : group == GROUP_WEEK ? R.string.noti_this_week : R.string.noti_older);

        h.viewUnread.setVisibility(item.unread && !editMode ? View.VISIBLE : View.GONE);
        h.avatar.setUser(item.uid, item.name, item.photoUrl);
        h.textSub.setText(item.sub);

        if (invite) {
            // 초대 카드: 제목 전체 굵게, 셋째 줄은 "장소 · 몇 분 전" (Figma 370:98)
            h.textTitle.setText(context.getString(R.string.noti_invite, item.name));
            h.textTime.setText(context.getString(R.string.noti_invite_info, item.place, TimeText.ago(context, item.time)));
            h.buttonDetail.setOnClickListener(v -> listener.onClick(item));
            h.textNotice.setVisibility(editMode ? View.GONE : View.VISIBLE);
        } else {
            // 친구 요청 줄: 안 읽음이면 배경색, 이름만 굵게
            h.layoutRow.setBackgroundColor(ContextCompat.getColor(context, item.unread ? R.color.bg_soft : R.color.white));
            h.imageBadge.setImageResource(R.drawable.ic_noti_friend);
            String title = context.getString(R.string.noti_friend_request, item.name);
            SpannableString span = new SpannableString(title);
            span.setSpan(new StyleSpan(Typeface.BOLD), 0, item.name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            h.textTitle.setText(span);
            h.textTime.setText(TimeText.ago(context, item.time));

            // 편집 모드: 왼쪽 여백 18dp (Figma 377:398)
            int paddingStart = context.getResources().getDimensionPixelSize(
                    editMode ? R.dimen.noti_row_padding_edit_start : R.dimen.screen_padding);
            h.layoutContent.setPaddingRelative(paddingStart, h.layoutContent.getPaddingTop(),
                    h.layoutContent.getPaddingEnd(), h.layoutContent.getPaddingBottom());
        }

        h.imageCheck.setVisibility(editMode ? View.VISIBLE : View.GONE);
        h.imageCheck.setImageResource(selectedKeys.contains(item.key)
                ? R.drawable.ic_check_box_on : R.drawable.ic_check_box_off);

        h.layoutButtons.setVisibility(editMode ? View.GONE : View.VISIBLE);
        h.buttonAccept.setOnClickListener(v -> listener.onAccept(item));
        h.buttonReject.setOnClickListener(v -> listener.onReject(item));

        h.layoutRow.setOnClickListener(v -> {
            if (!editMode) {
                listener.onClick(item);
                return;
            }
            if (!selectedKeys.remove(item.key)) selectedKeys.add(item.key);
            notifyItemChanged(h.getAdapterPosition());
            listener.onSelectionChanged(selectedKeys.size());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // 오늘 / 이번 주(7일 안) / 이전
    private int groupOf(Date time) {
        long days = TimeText.daysAgo(time);
        if (days == 0) return GROUP_TODAY;
        if (days < 7) return GROUP_WEEK;
        return GROUP_OLDER;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textGroup, textTitle, textSub, textTime, buttonAccept, buttonReject;
        final View layoutRow, layoutContent, layoutButtons, viewUnread;
        final ImageView imageCheck;
        final AvatarView avatar;
        final ImageView imageBadge;                // 친구 요청 줄만
        final TextView buttonDetail, textNotice;   // 초대 카드만

        ViewHolder(View v) {
            super(v);
            textGroup = v.findViewById(R.id.text_group);
            layoutRow = v.findViewById(R.id.layout_row);
            layoutContent = v.findViewById(R.id.layout_content);
            imageCheck = v.findViewById(R.id.image_check);
            avatar = v.findViewById(R.id.avatar);
            imageBadge = v.findViewById(R.id.image_badge);
            textTitle = v.findViewById(R.id.text_title);
            textSub = v.findViewById(R.id.text_sub);
            textTime = v.findViewById(R.id.text_time);
            layoutButtons = v.findViewById(R.id.layout_buttons);
            buttonAccept = v.findViewById(R.id.button_accept);
            buttonReject = v.findViewById(R.id.button_reject);
            buttonDetail = v.findViewById(R.id.button_detail);
            textNotice = v.findViewById(R.id.text_notice);
            viewUnread = v.findViewById(R.id.view_unread);
        }
    }
}
