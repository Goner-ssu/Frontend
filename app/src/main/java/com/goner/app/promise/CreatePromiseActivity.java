package com.goner.app.promise;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.goner.app.R;

// 약속 만들기: 이름 → (시간 → 장소 → 친구 → 벌칙) → 요약
public class CreatePromiseActivity extends AppCompatActivity {

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

    private void updateSteps() {
        int current = getSupportFragmentManager().getBackStackEntryCount(); // 0 = 첫 단계
        for (int i = 0; i < steps.getChildCount(); i++) {
            steps.getChildAt(i).setBackgroundResource(i <= current ? R.drawable.bg_step_on : R.drawable.bg_step_off);
        }
    }
}
