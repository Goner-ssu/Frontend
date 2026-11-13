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
                ((OnboardingActivity) requireActivity()).next();
            }
        });

        binding.laterButton.setOnClickListener(v ->
                ((OnboardingActivity) requireActivity()).next());

        return binding.getRoot();
    }


    private final ActivityResultLauncher<String> PermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                ((OnboardingActivity) requireActivity()).next();
            });



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
