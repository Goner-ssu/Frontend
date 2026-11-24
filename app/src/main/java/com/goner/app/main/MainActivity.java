package com.goner.app.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.goner.app.R;
import com.goner.app.common.InviteLink;
import com.goner.app.friend.InviteLandingActivity;
import com.goner.app.mypage.MyPageFragment;
import com.goner.app.record.RecordFragment;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    // 다른 화면에서 홈으로 돌아올 때 이 값에 문구 넣어주면 토스트 뜸 (약속 수정, 초대 참여)
    public static final String EXTRA_TOAST = "toast";

    private static final String[] TAGS = {"record", "home", "mypage"};
    private static final String KEY_TAB = "tab";
    private static final long TOAST_DURATION = 2500;

    private BottomNavView bottomNav;
    private TextView toast;
    private final Fragment[] fragments = new Fragment[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        toast = findViewById(R.id.main_toast);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            bottomNav.setBottomInset(bars.bottom);
            return insets;
        });

        FragmentManager fm = getSupportFragmentManager();
        int tab = BottomNavView.TAB_HOME;
        if (savedInstanceState == null) {
            // 탭 3개 다 미리 넣어두고 show/hide로 바꿈
            fragments[0] = new RecordFragment();
            fragments[1] = new HomeFragment();
            fragments[2] = new MyPageFragment();
            FragmentTransaction ft = fm.beginTransaction();
            for (int i = 0; i < 3; i++) {
                ft.add(R.id.main_container, fragments[i], TAGS[i]);
                if (i != tab) ft.hide(fragments[i]);
            }
            ft.commit();
        } else {
            for (int i = 0; i < 3; i++) {
                fragments[i] = fm.findFragmentByTag(TAGS[i]);
            }
            tab = savedInstanceState.getInt(KEY_TAB, BottomNavView.TAB_HOME);
        }

        bottomNav.setSelectedTab(tab);
        bottomNav.setOnTabSelectedListener(this::showTab);

        // 뒤로가기: 홈 말고 다른 탭이면 홈으로, 홈이면 원래대로(앱 종료)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (bottomNav.getSelectedTab() != BottomNavView.TAB_HOME) {
                    bottomNav.setSelectedTab(BottomNavView.TAB_HOME);
                    showTab(BottomNavView.TAB_HOME);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });

        checkToast(getIntent());
        if (savedInstanceState == null) openPendingInvite();
    }

    // 로그인 전에 친구 초대 링크로 들어왔던 경우: 로그인·온보딩 끝나고 홈 오면 그 초대 화면 다시 열기
    private void openPendingInvite() {
        if (FirebaseAuth.getInstance().getUid() == null) return;
        String code = InviteLink.takePendingFriendCode(this);
        if (code == null) return;
        Intent intent = new Intent(this, InviteLandingActivity.class);
        intent.putExtra(InviteLandingActivity.EXTRA_FRIEND_CODE, code);
        startActivity(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkToast(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB, bottomNav.getSelectedTab());
    }

    private void showTab(int tab) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        for (int i = 0; i < 3; i++) {
            if (i == tab) ft.show(fragments[i]);
            else ft.hide(fragments[i]);
        }
        ft.commit();
    }

    private void checkToast(Intent intent) {
        String text = intent.getStringExtra(EXTRA_TOAST);
        if (text == null) return;
        intent.removeExtra(EXTRA_TOAST); // 화면 돌려도 다시 안 뜨게

        bottomNav.setSelectedTab(BottomNavView.TAB_HOME);
        showTab(BottomNavView.TAB_HOME);
        toast.setText(text);
        toast.setVisibility(View.VISIBLE);
        toast.removeCallbacks(hideToast);
        toast.postDelayed(hideToast, TOAST_DURATION);
    }

    private final Runnable hideToast = () -> toast.setVisibility(View.GONE);
}
