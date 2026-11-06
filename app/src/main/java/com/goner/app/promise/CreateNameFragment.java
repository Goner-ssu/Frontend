package com.goner.app.promise;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.goner.app.R;

// 약속 만들기 1단계: 약속 이름
public class CreateNameFragment extends Fragment {

    private static final int MAX_LENGTH = 20;

    private EditText editTitle;
    private Button buttonNext;

    public CreateNameFragment() {
        super(R.layout.fragment_create_name);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        AppointmentDraft draft = new ViewModelProvider(requireActivity()).get(CreatePromiseViewModel.class).getDraft();

        editTitle = view.findViewById(R.id.edit_title);
        buttonNext = view.findViewById(R.id.button_next);

        editTitle.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_LENGTH)});
        if (draft.title != null) editTitle.setText(draft.title); // 뒤로 왔을 때 입력값 다시 보여줌

        editTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateButton();
            }
        });
        updateButton();

        buttonNext.setOnClickListener(v -> {
            draft.title = editTitle.getText().toString().trim();
            // TODO 2단계(시간) Fragment 만들면 ((CreatePromiseActivity) requireActivity()).showNext(...)
        });
    }

    // 공백 빼고 1~20자일 때만 다음 가능
    private void updateButton() {
        int length = editTitle.getText().toString().trim().length();
        buttonNext.setEnabled(length >= 1 && length <= MAX_LENGTH);
    }
}
