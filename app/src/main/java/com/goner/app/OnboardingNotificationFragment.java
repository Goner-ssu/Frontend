package com.goner.app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
import android.os.Build;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

import com.goner.app.databinding.FragmentOnboardingNotificationBinding;


public class OnboardingNotificationFragment extends Fragment {
    private FragmentOnboardingNotificationBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingNotificationBinding.inflate(inflater, container, false);
        binding.notificationButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                completeOnboarding(true);
            }
        });

        binding.laterButton.setOnClickListener(v -> completeOnboarding(false));
        return binding.getRoot();
    }


    private final ActivityResultLauncher<String> PermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                completeOnboarding(isGranted);
            });


    private void completeOnboarding(boolean notificationEnabled) {
        String json;
        try {
            json = new JSONObject().put("notificationEnabled", notificationEnabled).toString();
        } catch (JSONException e) {
            return;
        }

        binding.notificationButton.setEnabled(false); // 중복 클릭 방지
        binding.laterButton.setEnabled(false);

        ApiClient.request("POST", ApiClient.COMPLETE_ONBOARDING_URL, json, new ApiClient.Callback() {
            @Override
            public void onResult(int statusCode, String body) {
                if (binding == null) return;
                if (statusCode == 200) {
                    ((OnboardingActivity) requireActivity()).next(); // 마지막 단계라 온보딩 종료
                } else {
                    onSaveFailed();
                }
            }

            @Override
            public void onError(Exception e) {
                if (binding == null) return;
                onSaveFailed();
            }
        });
    }

    private void onSaveFailed() {
        binding.notificationButton.setEnabled(true);
        binding.laterButton.setEnabled(true);
        Toast.makeText(requireContext(), R.string.onboarding3_saveerror, Toast.LENGTH_SHORT).show();
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
