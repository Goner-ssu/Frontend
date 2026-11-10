package com.goner.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.goner.app.R;
import com.goner.app.data.repo.UserRepo;
import com.goner.app.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

// 스플래시: 1.2초 보여주고 로그인 / 온보딩 / 메인 중 하나로 보냄
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1200L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable navigate = this::navigateNext;
    private final UserRepo userRepo = new UserRepo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(navigate, SPLASH_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        // 대기 중에 뒤로가기로 나가면 다음 화면 안 띄움
        handler.removeCallbacks(navigate);
        super.onDestroy();
    }

    private void navigateNext() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { // 로그인 안 됨
            // TODO 로그인 화면 만들면 goTo(LoginActivity.class) 로 되돌리기. 지금은 테스트하려고 바로 메인
            goTo(MainActivity.class);
            return;
        }
        // 로그인은 했는데 users 문서가 없으면 아직 프로필 안 만든 사람 → 온보딩
        userRepo.getUser(uid)
                .addOnSuccessListener(user -> goTo(user == null ? OnboardingActivity.class : MainActivity.class))
                .addOnFailureListener(e -> goTo(MainActivity.class)); // 네트워크 등으로 못 읽으면 일단 메인
    }

    private void goTo(Class<?> next) {
        if (isFinishing() || isDestroyed()) return;
        Intent intent = new Intent(this, next);
        // 초대 링크(21번)로 들어왔으면 링크 정보 그대로 넘겨줌
        intent.setData(getIntent().getData());
        if (getIntent().getExtras() != null) intent.putExtras(getIntent().getExtras());
        startActivity(intent);
        finish();
    }
}
