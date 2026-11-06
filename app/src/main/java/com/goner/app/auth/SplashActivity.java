package com.goner.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.goner.app.R;
import com.goner.app.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable navigate = this::navigateNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(navigate, SPLASH_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        // 대기 중에 뒤로가기로 나가면 다음 화면을 띄우지 않는다.
        handler.removeCallbacks(navigate);
        super.onDestroy();
    }

    private void navigateNext() {
        boolean signedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        Class<?> next = signedIn ? MainActivity.class : LoginActivity.class;
        startActivity(new Intent(this, next));
        finish();
    }
}
