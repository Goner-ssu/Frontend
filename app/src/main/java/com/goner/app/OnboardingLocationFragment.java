package com.goner.app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.goner.app.databinding.FragmentOnboardingLocationBinding;

public class OnboardingLocationFragment extends Fragment {

    private FragmentOnboardingLocationBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingLocationBinding.inflate(inflater, container, false);

        binding.locationButton.setOnClickListener(v -> ((OnboardingActivity) requireActivity()).next());
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}


