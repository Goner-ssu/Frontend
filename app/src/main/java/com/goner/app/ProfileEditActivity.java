package com.goner.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.goner.app.databinding.ActivityProfileEditBinding;

import org.json.JSONException;
import org.json.JSONObject;

public class ProfileEditActivity extends AppCompatActivity {
    private ActivityProfileEditBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_profile_edit);
        binding = ActivityProfileEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        binding.topBar.titleText.setText(R.string.mypage1_title);
        binding.topBar.backButton.setOnClickListener(v -> finish());

        binding.nicknameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                updateNicknameCount();
            }
        });
        updateNicknameCount();

        binding.checkButton.setOnClickListener(v -> checkFriendCode());

        binding.friendCodeEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.friendCheck.setVisibility(View.GONE);
            }
        });
    }

    private void updateNicknameCount() {
        int count = binding.nicknameEditText.getText().length();
        binding.nicknameCount.setText(getString(R.string.onboarding1_nickcount, count));
    }
    private void checkFriendCode() {
        String code = binding.friendCodeEdit.getText().toString();
        if (code.isEmpty()) return;

        // 형식 검사 (서버도 똑같이 검사함)
        if (!code.matches("^[a-z0-9]{4,16}$")) {
            showCheckResult(R.string.onboarding1_invalid, false);
            return;
        }

        binding.checkButton.setEnabled(false); // 응답이 올 때까지 중복 클릭 방지

        ApiClient.request("GET", ApiClient.CHECK_FRIEND_CODE_URL + "?code=" + code, null,
                new ApiClient.Callback() {
                    @Override
                    public void onResult(int statusCode, String body) {
                        Log.d("API", "status=" + statusCode + " body=" + body);   // 추가
                        if (binding == null) return; // 응답이 오기 전에 화면이 사라진 경우
                        binding.checkButton.setEnabled(true);

                        // 확인하는 사이에 코드를 고쳤으면 이 결과는 버림
                        if (!code.equals(binding.friendCodeEdit.getText().toString())) return;

                        if (statusCode != 200) {
                            showCheckResult(R.string.onboarding1_error, false);
                            return;
                        }
                        try {
                            boolean available = new JSONObject(body).getBoolean("available");
                            showCheckResult(available ? R.string.onboarding1_canuse
                                    : R.string.onboarding1_taken, available);
                        } catch (JSONException e) {
                            showCheckResult(R.string.onboarding1_error, false);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("API", "요청 실패", e);                                // 추가
                        if (binding == null) return;
                        binding.checkButton.setEnabled(true);
                        showCheckResult(R.string.onboarding1_error, false);
                    }
                });
    }
    private void showCheckResult(@StringRes int textRes, boolean ok) {
        binding.friendCheck.setVisibility(View.VISIBLE);
        binding.friendCheckText.setText(textRes);
        binding.friendCheckText.setTextColor(ContextCompat.getColor(this,
                ok ? R.color.orange6 : R.color.orange6));
        binding.friendCheckIcon.setVisibility(ok ? View.VISIBLE : View.GONE);
    }

}