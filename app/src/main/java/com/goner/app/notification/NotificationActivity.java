package com.goner.app.notification;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.R;
import com.goner.app.common.ConfirmDialog;
import com.goner.app.data.model.Appointment;
import com.goner.app.data.model.FriendRequest;
import com.goner.app.data.repo.FriendRepo;
import com.goner.app.data.repo.NotificationRepo;
import com.goner.app.promise.InviteDetailActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// 알림 화면. 친구 요청 + 약속 초대를 모아서 보여줌
// 삭제/읽음은 DB에 알림 문서가 없어서 이 폰에 key(종류+id)로 저장 (NotificationSeen, 홈 벨 점도 같이 씀)
public class NotificationActivity extends AppCompatActivity {

    private static final String REQUEST_DELETE = "delete_notifications";

    private final NotificationRepo notificationRepo = new NotificationRepo();
    private final FriendRepo friendRepo = new FriendRepo();
    private NotificationAdapter adapter;
    private RecyclerView recycler;
    private View progress, emptyLayout, selectBar, deleteBar;
    private TextView buttonEdit, textSelectedCount;
    private ImageView imageSelectAll;
    private Button buttonDelete;
    private boolean editMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        recycler = findViewById(R.id.recycler_notifications);
        progress = findViewById(R.id.progress_notifications);
        emptyLayout = findViewById(R.id.layout_empty);
        selectBar = findViewById(R.id.layout_select_bar);
        deleteBar = findViewById(R.id.layout_delete_bar);
        buttonEdit = findViewById(R.id.button_edit);
        textSelectedCount = findViewById(R.id.text_selected_count);
        imageSelectAll = findViewById(R.id.image_select_all);
        buttonDelete = findViewById(R.id.button_delete);

        adapter = new NotificationAdapter(new NotificationAdapter.Listener() {
            @Override
            public void onClick(NotificationItem item) {
                if (item.type == NotificationItem.TYPE_INVITE) openInvite(item, null);
            }

            @Override
            public void onAccept(NotificationItem item) {
                // 초대 참여하기 → 상세 화면에서 바로 위치 동의 시트
                if (item.type == NotificationItem.TYPE_INVITE) {
                    openInvite(item, InviteDetailActivity.ACTION_JOIN);
                    return;
                }
                friendRepo.accept(item.request)
                        .addOnSuccessListener(v -> {
                            removeItem(item);
                            Toast.makeText(NotificationActivity.this, R.string.noti_accepted, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> showActionFailed());
            }

            @Override
            public void onReject(NotificationItem item) {
                // 초대 거절 → 상세 화면에서 바로 거절 확인 팝업
                if (item.type == NotificationItem.TYPE_INVITE) {
                    openInvite(item, InviteDetailActivity.ACTION_REJECT);
                    return;
                }
                friendRepo.deleteRequest(item.request)
                        .addOnSuccessListener(v -> removeItem(item))
                        .addOnFailureListener(e -> showActionFailed());
            }

            @Override
            public void onSelectionChanged(int count) {
                updateSelection(count);
            }
        });
        recycler.setAdapter(adapter);

        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        buttonEdit.setOnClickListener(v -> setEditMode(!editMode));
        findViewById(R.id.button_select_all).setOnClickListener(v -> adapter.toggleSelectAll());
        buttonDelete.setOnClickListener(v -> ConfirmDialog.show(getSupportFragmentManager(), REQUEST_DELETE,
                getString(R.string.noti_delete_title),
                getString(R.string.noti_delete_message),
                getString(R.string.delete)));
        getSupportFragmentManager().setFragmentResultListener(REQUEST_DELETE, this,
                (key, result) -> deleteSelected());

        load();
    }

    // 초대 상세에서 거절하고 돌아오면 목록에서 빠지게 다시 불러옴
    @Override
    protected void onRestart() {
        super.onRestart();
        load();
    }

    private void openInvite(NotificationItem item, String action) {
        Intent intent = new Intent(this, InviteDetailActivity.class);
        intent.putExtra(InviteDetailActivity.EXTRA_APPOINTMENT_ID, item.appointmentId);
        intent.putExtra(InviteDetailActivity.EXTRA_ACTION, action);
        startActivity(intent);
    }

    // 친구 요청 + 약속 초대 둘 다 받아오면 합쳐서 최신순으로
    private void load() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { // 로그인 전이면 받을 알림이 없음
            showList();
            return;
        }
        Tasks.whenAllSuccess(friendRepo.listReceived(uid), notificationRepo.listInvites(uid))
                .addOnSuccessListener(results -> {
                    @SuppressWarnings("unchecked")
                    List<FriendRequest> requests = (List<FriendRequest>) results.get(0);
                    @SuppressWarnings("unchecked")
                    List<Appointment> invites = (List<Appointment>) results.get(1);
                    adapter.setItems(makeItems(requests, invites));
                    showList();
                })
                .addOnFailureListener(e -> {
                    showList();
                    Snackbar.make(recycler, R.string.noti_load_failed, Snackbar.LENGTH_SHORT).show();
                });
    }

    private List<NotificationItem> makeItems(List<FriendRequest> requests, List<Appointment> invites) {
        Set<String> hidden = NotificationSeen.hidden(this);
        Set<String> seen = NotificationSeen.seen(this);
        SimpleDateFormat format = new SimpleDateFormat("M월 d일 (E) HH:mm", Locale.KOREA);
        List<NotificationItem> list = new ArrayList<>();

        for (FriendRequest r : requests) {
            NotificationItem item = new NotificationItem();
            item.type = NotificationItem.TYPE_FRIEND;
            item.key = NotificationSeen.friendKey(r);
            item.uid = r.getFromUid();
            item.name = r.getFromNickname() == null ? "" : r.getFromNickname();
            item.photoUrl = r.getFromPhotoUrl();
            item.sub = getString(R.string.create_friends_code, r.getFromFriendCode());
            item.time = r.getCreatedAt() == null ? new Date() : r.getCreatedAt().toDate();
            item.request = r;
            list.add(item);
        }
        for (Appointment a : invites) {
            NotificationItem item = new NotificationItem();
            item.type = NotificationItem.TYPE_INVITE;
            item.key = NotificationSeen.inviteKey(a);
            item.uid = a.getHostUid();
            item.name = a.getHostNickname() == null ? "" : a.getHostNickname();
            String when = a.getAppointmentTime() == null ? "" : format.format(a.getAppointmentTime().toDate());
            item.sub = getString(R.string.noti_invite_info, a.getTitle(), when);
            item.place = a.getPlaceName() == null ? "" : a.getPlaceName();
            item.time = a.getCreatedAt() == null ? new Date() : a.getCreatedAt().toDate();
            item.appointmentId = a.getId();
            list.add(item);
        }

        list.removeIf(item -> hidden.contains(item.key));
        list.sort((x, y) -> y.time.compareTo(x.time));

        // 처음 보는 알림만 점 표시, 이번에 본 건 다음부터 읽은 걸로
        for (NotificationItem item : list) {
            item.unread = !seen.contains(item.key);
            seen.add(item.key);
        }
        NotificationSeen.saveSeen(this, seen);
        return list;
    }

    // 로딩 숨기고 목록 / 빈 상태 중 하나만. 알림 없으면 편집 버튼도 숨김
    private void showList() {
        boolean hasItems = adapter.getItemCount() > 0;
        progress.setVisibility(View.GONE);
        recycler.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        emptyLayout.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        buttonEdit.setVisibility(hasItems ? View.VISIBLE : View.INVISIBLE);
        if (!hasItems && editMode) setEditMode(false);
    }

    private void setEditMode(boolean on) {
        editMode = on;
        buttonEdit.setText(on ? R.string.noti_done : R.string.noti_edit);
        selectBar.setVisibility(on ? View.VISIBLE : View.GONE);
        deleteBar.setVisibility(on ? View.VISIBLE : View.GONE);
        adapter.setEditMode(on);
    }

    private void updateSelection(int count) {
        textSelectedCount.setText(getString(R.string.noti_selected_count, count));
        buttonDelete.setText(getString(R.string.noti_delete_selected, count));
        buttonDelete.setEnabled(count > 0);
        boolean all = adapter.isAllSelected();
        imageSelectAll.setBackgroundResource(all ? R.drawable.bg_select_all_on : R.drawable.bg_select_all_off);
        imageSelectAll.setImageAlpha(all ? 255 : 0);
    }

    // 선택한 알림 key 를 hidden 에 넣고 목록에서 뺌
    private void deleteSelected() {
        NotificationSeen.addHidden(this, adapter.getSelectedKeys());
        Set<String> hidden = NotificationSeen.hidden(this);

        List<NotificationItem> left = new ArrayList<>();
        for (NotificationItem item : adapter.getItems()) {
            if (!hidden.contains(item.key)) left.add(item);
        }
        adapter.setItems(left);
        setEditMode(false);
        showList();
    }

    private void removeItem(NotificationItem item) {
        adapter.remove(item);
        showList();
    }

    private void showActionFailed() {
        Toast.makeText(this, R.string.noti_action_failed, Toast.LENGTH_SHORT).show();
    }
}
