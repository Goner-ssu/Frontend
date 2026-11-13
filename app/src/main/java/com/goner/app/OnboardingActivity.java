// onboarding xml 작성
// fragment xml 해결
// progress drawable 파일 생성
//

package com.goner.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.goner.app.databinding.ActivityOnboardingBinding;


public class OnboardingActivity extends AppCompatActivity {

    private static final String KEY_STEP = "step";
    private static final int LAST_STEP = 2;

    private ActivityOnboardingBinding binding;
    private int step = 0;
    private View[] progressBar;

    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            goToStep(step - 1);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        progressBar = new View[]{
                binding.progressBar1,
                binding.progressBar2,
                binding.progressBar3
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);

        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, createFragment(0)).commit();
        } else {
            step = savedInstanceState.getInt(KEY_STEP,0);
        }
        updateProgress();
    }

    @Override
    protected  void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_STEP, step);
    }

    public void next() {
        if(step<LAST_STEP) {
            goToStep(step+1);
        } else {
            completeOnboarding();
        }
    }

    private void goToStep(int target) {
        if (target < 0 || target > LAST_STEP) return;
        step = target;
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, createFragment(target)).commit();
        updateProgress();
    }

    private Fragment createFragment(int target) {
        switch (target) {
            case 0:
                return new OnboardingProfileFragment();
            case 1:
                return new OnboardingLocationFragment();
            default:
                return new OnboardingNotificationFragment();
        }
    }

    private void updateProgress(){
        for (int i = 0; i < progressBar.length; i++) {
            progressBar[i].setBackgroundResource(
                    i <= step ? R.drawable.bg_progress_on : R.drawable.bg_progress_off);}
        backCallback.setEnabled(step > 0);
    }

    private void completeOnboarding() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}