package com.goner.app.promise;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.goner.app.R;

// 약속 만들기: 이름 → 시간 → 장소 → 친구 → 벌칙 → 요약
public class CreatePromiseActivity extends AppCompatActivity {

    // 단계 번호 (요약에서 "수정" 누르면 그 단계로 돌아감)
    static final int STEP_NAME = 0;
    static final int STEP_TIME = 1;
    static final int STEP_PLACE = 2;
    static final int STEP_FRIENDS = 3;
    static final int STEP_PENALTY = 4;

    private LinearLayout steps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_promise);

        // 키보드 올라오면 그만큼 아래 여백 줘서 버튼이 키보드 위로 오게 함
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        steps = findViewById(R.id.layout_steps);
        findViewById(R.id.button_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // 뒤로 가서 단계 바뀌면 위 표시도 맞춰줌
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateSteps);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.create_container, new CreateNameFragment())
                    .commit();
        } else if (getSupportFragmentManager().findFragmentById(R.id.created_container) != null) {
            // 화면 회전 등으로 다시 만들어졌을 때 완료 화면 다시 보이게
            findViewById(R.id.created_container).setVisibility(View.VISIBLE);
        }
        updateSteps();
    }

    // 다음 단계 Fragment로 넘어감 (뒤로가기로 이전 단계 돌아갈 수 있게 back stack에 넣음)
    void showNext(Fragment next) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.create_container, next)
                .addToBackStack(null)
                .commit();
    }

    // 요약에서 "수정" → 그 단계까지 back stack 되돌림. 입력값은 draft에 남아 있어서 그대로 보임
    void goToStep(int step) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() == 0) return;
        if (step == STEP_NAME) {
            fm.popBackStack(fm.getBackStackEntryAt(0).getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            // back stack 의 (step-1)번째가 그 단계를 띄운 기록
            fm.popBackStack(fm.getBackStackEntryAt(step - 1).getId(), 0);
        }
    }

    // 저장하는 동안 로딩 화면으로 덮음
    void showLoading(boolean show) {
        findViewById(R.id.layout_loading).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // 저장 끝나면 완료 화면을 맨 위에 덮어서 보여줌
    void showCreated(String appointmentId) {
        findViewById(R.id.created_container).setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.created_container, PromiseCreatedFragment.newInstance(appointmentId))
                .commit();
        showLoading(false);
    }

    private void updateSteps() {
        int current = getSupportFragmentManager().getBackStackEntryCount(); // 0 = 첫 단계
        for (int i = 0; i < steps.getChildCount(); i++) {
            steps.getChildAt(i).setBackgroundResource(i <= current ? R.drawable.bg_step_on : R.drawable.bg_step_off);
        }
    }
}
