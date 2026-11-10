package com.goner.app.auth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.goner.app.R;

// 온보딩 (프로필·친구 코드). TODO 화면은 온보딩 작업에서
public class OnboardingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
    }
}
