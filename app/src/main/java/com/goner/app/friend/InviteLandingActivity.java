package com.goner.app.friend;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.goner.app.R;
import com.goner.app.auth.LoginActivity;
import com.goner.app.common.AvatarView;
import com.goner.app.common.InviteLink;
import com.goner.app.data.model.User;
import com.goner.app.data.repo.FriendRepo;
import com.goner.app.data.repo.UserRepo;
import com.goner.app.main.MainActivity;
import com.goner.app.promise.InviteDetailActivity;
import com.google.firebase.auth.FirebaseAuth;

// 초대 링크로 앱이 열렸을 때 (App Links). 약속 링크는 초대 상세로 넘기고, 친구 링크는 여기서 처리
//   회원 → "친구 되기" (요청 없이 바로 친구) / 이미 친구 → 안내 / 비회원 → 코드 기억 + 로그인
public class InviteLandingActivity extends AppCompatActivity {

    // 링크 말고 앱 안에서 열 때 (로그인 끝나고 기억해 둔 코드로 다시 열기)
    public static final String EXTRA_FRIEND_CODE = "friendCode";

    private final FriendRepo friendRepo = new FriendRepo();
    private final UserRepo userRepo = new UserRepo();
    private String uid;
    private String code;
    private User inviter;

    private View progress, layoutMember, layoutGuest, chipFriend;
    private TextView textTitle, textDesc, textCode, buttonSub;
    private Button buttonMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uid = FirebaseAuth.getInstance().getUid();
        Uri uri = getIntent().getData();

        // 약속 링크 /p/{id} → 초대 상세 (로그인 전이면 로그인부터)
        String appointmentId = InviteLink.appointmentIdFrom(uri);
        if (appointmentId != null) {
            if (uid == null) {
                Toast.makeText(this, R.string.friend_need_login, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                Intent intent = new Intent(this, InviteDetailActivity.class);
                intent.putExtra(InviteDetailActivity.EXTRA_APPOINTMENT_ID, appointmentId);
                startActivity(intent);
            }
            finish();
            return;
        }

        code = uri != null ? InviteLink.friendCodeFrom(uri) : getIntent().getStringExtra(EXTRA_FRIEND_CODE);
        if (code == null) {
            goHome();
            return;
        }

        setContentView(R.layout.activity_invite_landing);
        progress = findViewById(R.id.progress_landing);
        layoutMember = findViewById(R.id.layout_member);
        layoutGuest = findViewById(R.id.layout_guest);
        chipFriend = findViewById(R.id.chip_friend);
        textTitle = findViewById(R.id.text_title);
        textDesc = findViewById(R.id.text_desc);
        textCode = findViewById(R.id.text_code);
        buttonMain = findViewById(R.id.button_main);
        buttonSub = findViewById(R.id.button_sub);

        if (uid == null) loadGuest();
        else loadMember();
    }

    // 비회원: 코드 기억해 두고(홈 오면 MainActivity 가 다시 이 화면 엶) 로그인 화면 구성
    private void loadGuest() {
        InviteLink.savePendingFriendCode(this, code);
        findViewById(R.id.button_google).setOnClickListener(v -> {
            // TODO 로그인(Google) 화면은 담당 팀원. 로그인·온보딩 끝나고 홈 가면 초대 화면으로 돌아옴
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        // 로그인 화면은 바로 보여주고, 초대한 사람 정보는 읽히면 카드로 (못 읽으면 카드 없이)
        progress.setVisibility(View.GONE);
        layoutGuest.setVisibility(View.VISIBLE);
        View banner = findViewById(R.id.layout_guest_banner);
        banner.setVisibility(View.INVISIBLE);
        friendRepo.findByCode(code).addOnSuccessListener(user -> {
            if (user == null) return;
            ((AvatarView) findViewById(R.id.avatar_guest)).setUser(user.getUid(), user.getNickname(), user.getPhotoUrl());
            ((TextView) findViewById(R.id.text_guest_title))
                    .setText(getString(R.string.landing_guest_title, user.getNickname()));
            banner.setVisibility(View.VISIBLE);
        });
    }

    private void loadMember() {
        friendRepo.search(code, uid)
                .addOnSuccessListener(result -> {
                    if (result.state == FriendRepo.SearchResult.NOT_FOUND) {
                        Toast.makeText(this, R.string.add_not_found_title, Toast.LENGTH_SHORT).show();
                        goHome();
                        return;
                    }
                    if (result.state == FriendRepo.SearchResult.ME) { // 내 링크를 내가 연 경우
                        Toast.makeText(this, R.string.add_my_code_hint, Toast.LENGTH_SHORT).show();
                        goHome();
                        return;
                    }
                    inviter = result.user;
                    progress.setVisibility(View.GONE);
                    layoutMember.setVisibility(View.VISIBLE);
                    ((AvatarView) findViewById(R.id.avatar)).setUser(inviter.getUid(), inviter.getNickname(), inviter.getPhotoUrl());
                    textCode.setText(getString(R.string.create_friends_code, inviter.getFriendCode()));
                    if (result.state == FriendRepo.SearchResult.FRIEND) showAlreadyFriend();
                    else showInvite();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.add_search_failed, Toast.LENGTH_SHORT).show();
                    goHome();
                });
    }

    // Figma 253:2256
    private void showInvite() {
        textTitle.setText(getString(R.string.landing_title, inviter.getNickname()));
        textDesc.setText(R.string.landing_desc);
        chipFriend.setVisibility(View.GONE);
        buttonMain.setText(R.string.landing_accept);
        buttonMain.setOnClickListener(v -> becomeFriends());
        buttonSub.setText(R.string.landing_later);
        buttonSub.setOnClickListener(v -> goHome());
    }

    // Figma 376:404
    private void showAlreadyFriend() {
        textTitle.setText(getString(R.string.landing_already_title, inviter.getNickname()));
        textDesc.setText(R.string.landing_already_desc);
        chipFriend.setVisibility(View.VISIBLE);
        buttonMain.setEnabled(true);
        buttonMain.setText(R.string.landing_friend_list);
        buttonMain.setOnClickListener(v -> {
            startActivity(new Intent(this, FriendActivity.class));
            finish();
        });
        buttonSub.setText(R.string.landing_home);
        buttonSub.setOnClickListener(v -> goHome());
    }

    // 요청 없이 바로 양쪽 friends 문서 만들기 (내 정보 복사해야 해서 내 users 먼저 읽음)
    private void becomeFriends() {
        buttonMain.setEnabled(false);
        userRepo.getUser(uid)
                .continueWithTask(task -> friendRepo.addFriend(task.getResult(), inviter))
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, R.string.noti_accepted, Toast.LENGTH_SHORT).show();
                    showAlreadyFriend();
                })
                .addOnFailureListener(e -> {
                    buttonMain.setEnabled(true);
                    Toast.makeText(this, e.getMessage() == null ? getString(R.string.noti_action_failed) : e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
