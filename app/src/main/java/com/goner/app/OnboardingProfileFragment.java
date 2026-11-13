package com.goner.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;

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
        updateNicknameCount();

        return binding.getRoot();
    }

    private void updateNicknameCount() {
        int count = binding.nicknameEditText.getText().length();
        binding.nicknameCount.setText(getString(R.string.onboarding1_nickcount, count));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}