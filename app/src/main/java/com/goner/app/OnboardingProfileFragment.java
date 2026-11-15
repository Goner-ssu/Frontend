package com.goner.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import org.json.JSONException;
import org.json.JSONObject;

import com.goner.app.databinding.FragmentOnboardingProfileBinding;

public class OnboardingProfileFragment extends Fragment {

    private FragmentOnboardingProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingProfileBinding.inflate(inflater, container, false);

        binding.nextButton.setOnClickListener(v -> saveProfile());
        binding.nicknameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                updateNicknameCount();
            }
        });
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

        updateNicknameCount();

        return binding.getRoot();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    private void showCheckResult(@StringRes int textRes, boolean ok) {
        binding.friendCheck.setVisibility(View.VISIBLE);
        binding.friendCheckText.setText(textRes);
        binding.friendCheckText.setTextColor(ContextCompat.getColor(requireContext(),
                ok ? R.color.orange6 : R.color.orange6));
        binding.friendCheckIcon.setVisibility(ok ? View.VISIBLE : View.GONE);
    }

    private void saveProfile() {
        String nickname = binding.nicknameEditText.getText().toString().trim();
        String code = binding.friendCodeEdit.getText().toString();
        if (nickname.isEmpty() || code.isEmpty()) return;

        if (!code.matches("^[a-z0-9]{4,16}$")) {
            showCheckResult(R.string.onboarding1_invalid, false);
            return;
        }

        String json;
        try {
            json = new JSONObject()
                    .put("nickname", nickname)
                    .put("friendCode", code)
                    .toString();
        } catch (JSONException e) {
            return;
        }

        binding.nextButton.setEnabled(false);

        ApiClient.request("POST", ApiClient.SAVE_PROFILE_URL, json, new ApiClient.Callback() {
            @Override
            public void onResult(int statusCode, String body) {
                if (binding == null) return;
                binding.nextButton.setEnabled(true);

                if (statusCode == 200) {
                    ((OnboardingActivity) requireActivity()).next();
                } else if (statusCode == 409) {
                    showCheckResult(R.string.onboarding1_taken, false);
                } else {
                    showCheckResult(R.string.onboarding1_error, false);
                }
            }

            @Override
            public void onError(Exception e) {
                if (binding == null) return;
                binding.nextButton.setEnabled(true);
                showCheckResult(R.string.onboarding1_error, false);
            }
        });
    }
}