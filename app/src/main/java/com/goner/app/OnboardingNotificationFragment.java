package com.goner.app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.goner.app.databinding.FragmentOnboardingNotificationBinding;


public class OnboardingNotificationFragment extends Fragment {
    private FragmentOnboardingNotificationBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingNotificationBinding.inflate(inflater, container, false);

        binding.notificationButton.setOnClickListener(v -> ((OnboardingActivity) requireActivity()).next());
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}

