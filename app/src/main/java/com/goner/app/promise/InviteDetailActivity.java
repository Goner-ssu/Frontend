package com.goner.app.promise;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.goner.app.R;
import com.goner.app.common.AvatarView;
import com.goner.app.common.ConfirmDialog;
import com.goner.app.common.TopBar;
import com.goner.app.data.DbValues;
import com.goner.app.data.model.Appointment;
import com.goner.app.data.model.Participant;
import com.goner.app.data.repo.AppointmentRepo;
import com.goner.app.main.MainActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// 약속 초대 상세 (알림에서 들어옴). 참여하기 → 위치 공유 동의 시트 → 참여 / 거절 → 확인 팝업
public class InviteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_APPOINTMENT_ID = "appointmentId";
    // 알림 목록에서 참여하기/거절을 눌러서 왔으면 들어오자마자 시트/팝업 띄움
    public static final String EXTRA_ACTION = "action";
    public static final String ACTION_JOIN = "join";
    public static final String ACTION_REJECT = "reject";

    private static final String REQUEST_REJECT = "reject_invite";

    private final AppointmentRepo appointmentRepo = new AppointmentRepo();
    private String appointmentId;
    private String uid;
    private Appointment appointment;
    private int memberCount;
    private View scroll, progress;
    private Button buttonReject, buttonJoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_detail);

        appointmentId = getIntent().getStringExtra(EXTRA_APPOINTMENT_ID);
        uid = FirebaseAuth.getInstance().getUid();
        if (appointmentId == null || uid == null) { // 초대는 로그인한 사람만 받음
            finish();
            return;
        }

        TopBar.bind(findViewById(R.id.top_bar), getString(R.string.invite_title), v -> finish());
        scroll = findViewById(R.id.scroll_content);
        progress = findViewById(R.id.progress_invite);
        buttonReject = findViewById(R.id.button_reject);
        buttonJoin = findViewById(R.id.button_join);

        buttonJoin.setOnClickListener(v -> showConsentSheet());
        buttonReject.setOnClickListener(v -> showRejectDialog());
        getSupportFragmentManager().setFragmentResultListener(REQUEST_REJECT, this, (key, result) -> reject());

        load(savedInstanceState == null);
    }

    // 약속 먼저 읽고, 내가 참여자면 참여자 목록까지 받아서 화면 채움
    // (링크로 들어온 사람이 참여자가 아니면 참여자 목록은 안 읽고 안내만)
    private void load(boolean firstOpen) {
        appointmentRepo.get(appointmentId)
                .continueWithTask(task -> {
                    Appointment a = task.getResult();
                    if (a == null) throw new IllegalStateException("약속 없음");
                    appointment = a;
                    if (!a.getParticipantUids().contains(uid)) return Tasks.forResult((List<Participant>) null);
                    return appointmentRepo.listParticipants(appointmentId);
                })
                .addOnSuccessListener(participants -> {
                    if (participants == null) {
                        showNotInvited();
                        return;
                    }
                    bind(appointment, participants);

                    String action = getIntent().getStringExtra(EXTRA_ACTION);
                    if (!firstOpen || action == null) return;
                    if (action.equals(ACTION_JOIN)) showConsentSheet();
                    else if (action.equals(ACTION_REJECT)) showRejectDialog();
                })
                .addOnFailureListener(e -> loadFailed());
    }

    // "초대받은 사람만 볼 수 있어요" 만 보이고 안내·버튼 숨김
    private void showNotInvited() {
        progress.setVisibility(View.GONE);
        findViewById(R.id.text_not_invited).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_location_notice).setVisibility(View.GONE);
        findViewById(R.id.layout_cta).setVisibility(View.GONE);
    }

    private void loadFailed() {
        Toast.makeText(this, R.string.invite_load_failed, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void bind(Appointment a, List<Participant> participants) {
        String host = a.getHostNickname() == null ? "" : a.getHostNickname();

        // 거절한 사람 빼고, 방장 맨 앞 / 나 맨 뒤 (Figma 순서)
        Participant hostP = null, me = null;
        List<Participant> others = new ArrayList<>();
        int joined = 0, waiting = 0;
        for (Participant p : participants) {
            if (DbValues.PARTICIPANT_REJECTED.equals(p.getStatus())) continue;
            if (DbValues.PARTICIPANT_JOINED.equals(p.getStatus())) joined++;
            if (DbValues.PARTICIPANT_INVITED.equals(p.getStatus())) waiting++;
            if (p.getUid().equals(a.getHostUid())) hostP = p;
            else if (p.getUid().equals(uid)) me = p;
            else others.add(p);
        }
        List<Participant> members = new ArrayList<>();
        if (hostP != null) members.add(hostP);
        members.addAll(others);
        if (me != null) members.add(me);
        memberCount = members.size();

        ((AvatarView) findViewById(R.id.avatar_host)).setUser(a.getHostUid(), host,
                hostP == null ? null : hostP.getPhotoUrl());
        ((TextView) findViewById(R.id.text_host)).setText(getString(R.string.invite_host, host));
        ((TextView) findViewById(R.id.text_title)).setText(a.getTitle());
        SimpleDateFormat format = new SimpleDateFormat("M월 d일 (E) a h:mm", Locale.KOREA);
        ((TextView) findViewById(R.id.text_time)).setText(
                a.getAppointmentTime() == null ? "" : format.format(a.getAppointmentTime().toDate()));
        ((TextView) findViewById(R.id.text_place)).setText(a.getPlaceName());

        // 벌칙 없음이면 줄 숨김
        boolean noPenalty = DbValues.PENALTY_NONE.equals(a.getPenaltyType())
                || a.getPenaltyText() == null || a.getPenaltyText().isEmpty();
        findViewById(R.id.layout_penalty).setVisibility(noPenalty ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.text_penalty)).setText(getString(R.string.invite_penalty, a.getPenaltyText()));

        ((TextView) findViewById(R.id.text_members)).setText(getString(R.string.invite_members, memberCount));
        ((TextView) findViewById(R.id.text_members_status)).setText(
                getString(R.string.invite_members_status, joined, waiting));

        LinearLayout layoutMembers = findViewById(R.id.layout_members);
        layoutMembers.removeAllViews();
        for (Participant p : members) addMember(layoutMembers, p, p == hostP, p == me);

        progress.setVisibility(View.GONE);
        scroll.setVisibility(View.VISIBLE);
        buttonReject.setEnabled(true);
        buttonJoin.setEnabled(true);
    }

    // 참여자 한 명 (아바타 40 + 이름 + 방장/참여/대기 칩)
    private void addMember(LinearLayout parent, Participant p, boolean host, boolean me) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_created_member, parent, false);
        AvatarView avatar = item.findViewById(R.id.avatar);
        int size = getResources().getDimensionPixelSize(R.dimen.invite_member_avatar);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        String name = me ? getString(R.string.created_me) : p.getNickname();
        avatar.setUser(p.getUid(), name, p.getPhotoUrl());
        ((TextView) item.findViewById(R.id.text_name)).setText(name);

        TextView chip = item.findViewById(R.id.text_chip);
        if (host) {
            chip.setText(R.string.created_host);
            chip.setBackgroundResource(R.drawable.bg_chip_host);
            chip.setTextColor(getColor(R.color.white));
        } else if (DbValues.PARTICIPANT_JOINED.equals(p.getStatus())) {
            chip.setText(R.string.invite_chip_joined);
            chip.setBackgroundResource(R.drawable.bg_pill_soft);
            chip.setTextColor(getColor(R.color.primary));
        } else {
            chip.setText(R.string.created_waiting);
            chip.setBackgroundResource(R.drawable.bg_chip_waiting);
            chip.setTextColor(getColor(R.color.text_sub));
        }

        if (parent.getChildCount() > 0) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginStart(getResources().getDimensionPixelSize(R.dimen.invite_member_gap));
            item.setLayoutParams(lp);
        }
        parent.addView(item);
    }

    // 위치 공유 동의 시트. "위 내용을 확인했어요" 체크해야 버튼 켜짐
    private void showConsentSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.sheet_invite_consent, null);
        ((TextView) view.findViewById(R.id.text_consent_members))
                .setText(getString(R.string.consent_members, memberCount));

        ImageView check = view.findViewById(R.id.image_consent_check);
        Button join = view.findViewById(R.id.button_consent_join);
        check.setImageAlpha(0);
        view.findViewById(R.id.button_consent_check).setOnClickListener(v -> {
            boolean checked = !join.isEnabled();
            join.setEnabled(checked);
            check.setBackgroundResource(checked ? R.drawable.bg_select_all_on : R.drawable.bg_select_all_off);
            check.setImageAlpha(checked ? 255 : 0);
        });
        join.setOnClickListener(v -> {
            sheet.dismiss();
            join();
        });
        sheet.setContentView(view);
        sheet.show();
    }

    private void join() {
        setButtonsEnabled(false);
        appointmentRepo.join(appointmentId, uid)
                .addOnSuccessListener(v -> {
                    // 홈으로 가서 토스트 (알림 화면도 같이 닫힘)
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra(MainActivity.EXTRA_TOAST, getString(R.string.invite_joined_toast, appointment.getTitle()));
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> actionFailed());
    }

    private void showRejectDialog() {
        String host = appointment.getHostNickname() == null ? "" : appointment.getHostNickname();
        ConfirmDialog.show(getSupportFragmentManager(), REQUEST_REJECT,
                getString(R.string.invite_reject_title),
                getString(R.string.invite_reject_message, host),
                getString(R.string.invite_reject_confirm));
    }

    private void reject() {
        setButtonsEnabled(false);
        appointmentRepo.reject(appointmentId, uid)
                .addOnSuccessListener(v -> finish())
                .addOnFailureListener(e -> actionFailed());
    }

    private void actionFailed() {
        setButtonsEnabled(true);
        Toast.makeText(this, R.string.invite_failed, Toast.LENGTH_SHORT).show();
    }

    private void setButtonsEnabled(boolean enabled) {
        buttonReject.setEnabled(enabled);
        buttonJoin.setEnabled(enabled);
    }
}
