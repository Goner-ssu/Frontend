package com.goner.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.goner.app.databinding.ActivityWithdrawBinding;

public class WithdrawActivity extends AppCompatActivity {

    private ActivityWithdrawBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityWithdrawBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.topBar.titleText.setText(R.string.mypage3_title);
        binding.topBar.backButton.setOnClickListener(v -> finish());

        binding.cancelButton.setOnClickListener(v -> finish());
        binding.agreeCheck.setOnCheckedChangeListener((buttonView,isChecked) -> {
            binding.withdrawButton.setEnabled(isChecked);
            binding.withdrawButton.setAlpha(isChecked ? 1.0f : 0.4f);
        });

        binding.withdrawButton.setOnClickListener(v -> {
            ConfirmDialog.show(this, R.string.mypage4_title, R.string.mypage4_explain, R.string.mypage34_button, view -> {
                deleteAccount();
            });
        });

        showUpcomingNotice(1);
    }

    private void showUpcomingNotice(int count) {
        if(count<=0) {
            binding.warning.setVisibility(View.GONE);
            return;
        }
        binding.warningText.setText(String.format(getString(R.string.mypage3_toast), count));
        binding.warning.setVisibility(View.VISIBLE);
    }

    private void deleteAccount() {
        binding.withdrawButton.setEnabled(false);

        ApiClient.request("POST", ApiClient.DELETE_ACCOUNT_URL, null, new ApiClient.Callback() {
            @Override
            public void onResult(int statusCode, String body) {
                if(statusCode == 200) {
                    AccountActions.logout(WithdrawActivity.this);
                } else { fail(); }
            }

            @Override
            public void onError(Exception e) {
                fail();
            }
        });
    }

    private void fail() {
        if (binding == null) return;
        binding.withdrawButton.setEnabled(true);
        Toast.makeText(this, R.string.mypage3_failed, Toast.LENGTH_SHORT);
    }
}
