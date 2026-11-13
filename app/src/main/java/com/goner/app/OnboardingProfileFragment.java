package com.goner.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.goner.app.databinding.FragmentOnboardingProfileBinding;

public class OnboardingProfileFragment extends Fragment {

    private FragmentOnboardingProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingProfileBinding.inflate(inflater, container, false);

        binding.nextButton.setOnClickListener(v ->
                ((OnboardingActivity) requireActivity()).next());

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}