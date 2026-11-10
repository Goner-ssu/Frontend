package com.goner.app.mypage;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.goner.app.BuildConfig;
import com.goner.app.R;
import com.goner.app.common.AvatarView;
import com.goner.app.common.InviteLink;
import com.goner.app.common.Nav;
import com.goner.app.data.model.User;
import com.goner.app.data.repo.FriendRepo;
import com.goner.app.data.repo.UserRepo;
import com.goner.app.friend.FriendActivity;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

// 마이페이지 탭 (Figma 253:3272). 프로필·통계·메뉴
public class MyPageFragment extends Fragment {

    private final UserRepo userRepo = new UserRepo();
    private final FriendRepo friendRepo = new FriendRepo();
    private String uid;
    private String friendCode;
    private ListenerRegistration userListener;
    private boolean savingNoti;     // 알람 저장 중 (그동안 들어오는 값으로 스위치 안 바꿈)
    private boolean bindingSwitch;  // 코드로 스위치 바꿀 때 리스너 안 타게

    private AvatarView avatar;
    private TextView textNickname, textFriendCode, textStatAppointments, textStatFriends, textStatRequests;
    private TextView badgeRequests, textFriendCount;
    private MaterialSwitch switchNoti;

    public MyPageFragment() {
        super(R.layout.fragment_mypage);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        uid = FirebaseAuth.getInstance().getUid();

        avatar = view.findViewById(R.id.avatar_me);
        textNickname = view.findViewById(R.id.text_nickname);
        textFriendCode = view.findViewById(R.id.text_friend_code);
        textStatAppointments = view.findViewById(R.id.text_stat_appointments);
        textStatFriends = view.findViewById(R.id.text_stat_friends);
        textStatRequests = view.findViewById(R.id.text_stat_requests);
        badgeRequests = view.findViewById(R.id.badge_requests);
        textFriendCount = view.findViewById(R.id.text_friend_manage_count);
        switchNoti = view.findViewById(R.id.switch_noti);

        // 묶음 카드 둥근 모서리 밖으로 줄 배경이 안 나오게
        view.findViewById(R.id.group_friend).setClipToOutline(true);
        view.findViewById(R.id.group_account).setClipToOutline(true);
        view.findViewById(R.id.group_terms).setClipToOutline(true);
        view.findViewById(R.id.group_support).setClipToOutline(true);

        ((TextView) view.findViewById(R.id.text_app_version))
                .setText(getString(R.string.mypage_version_latest, BuildConfig.VERSION_NAME));
        TextView withdraw = view.findViewById(R.id.button_withdraw);
        withdraw.setPaintFlags(withdraw.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        view.findViewById(R.id.button_edit_profile).setOnClickListener(v -> Nav.openEditProfile(requireContext()));
        view.findViewById(R.id.row_friend_manage).setOnClickListener(v -> openFriend(FriendActivity.TAB_HOME));
        view.findViewById(R.id.button_stat_requests).setOnClickListener(v -> openFriend(FriendActivity.TAB_RECEIVED));
        view.findViewById(R.id.row_share_link).setOnClickListener(v -> {
            if (friendCode != null) InviteLink.shareFriend(requireContext(), friendCode);
        });
        view.findViewById(R.id.row_logout).setOnClickListener(v -> Nav.openLogoutDialog(requireContext()));
        view.findViewById(R.id.row_terms_service).setOnClickListener(v -> Nav.openTerms(requireContext(), Nav.TERMS_SERVICE));
        view.findViewById(R.id.row_terms_privacy).setOnClickListener(v -> Nav.openTerms(requireContext(), Nav.TERMS_PRIVACY));
        view.findViewById(R.id.row_terms_location).setOnClickListener(v -> Nav.openTerms(requireContext(), Nav.TERMS_LOCATION));
        view.findViewById(R.id.row_oss).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), OssLicensesMenuActivity.class)));
        view.findViewById(R.id.row_contact).setOnClickListener(v -> openMail());
        withdraw.setOnClickListener(v -> Nav.openWithdraw(requireContext()));

        switchNoti.setEnabled(uid != null);
        switchNoti.setOnCheckedChangeListener((button, checked) -> {
            if (!bindingSwitch) saveNoti(checked);
        });

        // users/{me} 바뀌면 프로필·참여한 약속·알람 스위치 갱신
        if (uid != null) userListener = userRepo.listenUser(uid, this::bindUser);
    }

    // 다른 화면 갔다 오거나 탭 다시 보일 때 친구 수 / 받은 요청 수 다시 셈
    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) refreshCounts();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refreshCounts();
    }

    private void bindUser(@Nullable User user) {
        if (user == null) return;
        friendCode = user.getFriendCode();
        avatar.setUser(user.getUid(), user.getNickname(), user.getPhotoUrl());
        textNickname.setText(user.getNickname());
        textFriendCode.setText(getString(R.string.create_friends_code, friendCode));
        textStatAppointments.setText(String.valueOf(user.getAppointmentCount()));
        if (!savingNoti) setSwitch(user.isNotiEnabled());
    }

    private void refreshCounts() {
        if (uid == null) return;
        friendRepo.countFriends(uid).addOnSuccessListener(count -> {
            if (!isAdded()) return;
            textStatFriends.setText(String.valueOf(count));
            textFriendCount.setText(getString(R.string.mypage_friend_count, count.intValue()));
        });
        friendRepo.listReceived(uid).addOnSuccessListener(list -> {
            if (!isAdded()) return;
            textStatRequests.setText(String.valueOf(list.size()));
            badgeRequests.setText(String.valueOf(list.size()));
            badgeRequests.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    // 알람 ON/OFF 저장. 저장하는 동안 스위치 끄고, 실패하면 원래 값으로
    private void saveNoti(boolean enabled) {
        savingNoti = true;
        switchNoti.setEnabled(false);
        userRepo.setNotiEnabled(uid, enabled)
                .addOnCompleteListener(task -> {
                    savingNoti = false;
                    if (!isAdded()) return;
                    switchNoti.setEnabled(true);
                    if (!task.isSuccessful()) {
                        setSwitch(!enabled);
                        Snackbar.make(requireView(), R.string.mypage_noti_failed, Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private void setSwitch(boolean checked) {
        bindingSwitch = true;
        switchNoti.setChecked(checked);
        bindingSwitch = false;
    }

    private void openFriend(int tab) {
        Intent intent = new Intent(requireContext(), FriendActivity.class);
        intent.putExtra(FriendActivity.EXTRA_TAB, tab);
        startActivity(intent);
    }

    // 문의하기: 메일 앱 (받는 주소는 strings 의 support_email)
    private void openMail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.support_email)));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(requireView(), R.string.mypage_no_mail_app, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) userListener.remove();
    }
}
