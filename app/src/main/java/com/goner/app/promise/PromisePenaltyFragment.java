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
import com.goner.app.data.DbValues;

// 약속 만들기 5단계: 벌칙 고르기 (하나만)
public class PromisePenaltyFragment extends Fragment {

    private static final int CUSTOM_MAX = 20;

    private AppointmentDraft draft;
    private View cardMeal, cardCoffee, cardCustom, cardNone;
    private EditText editCustom;
    private Button buttonNext;

    public PromisePenaltyFragment() {
        super(R.layout.fragment_create_penalty);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        draft = new ViewModelProvider(requireActivity()).get(CreatePromiseViewModel.class).getDraft();

        cardMeal = view.findViewById(R.id.card_meal);
        cardCoffee = view.findViewById(R.id.card_coffee);
        cardCustom = view.findViewById(R.id.card_custom);
        cardNone = view.findViewById(R.id.card_none);
        editCustom = view.findViewById(R.id.edit_custom);
        buttonNext = view.findViewById(R.id.button_next);

        editCustom.setFilters(new InputFilter[]{new InputFilter.LengthFilter(CUSTOM_MAX)});
        // 뒤로 왔을 때 직접 입력했던 값 다시 보여줌
        if (DbValues.PENALTY_CUSTOM.equals(draft.penalty_type)) editCustom.setText(draft.penalty_text);

        cardMeal.setOnClickListener(v -> select(DbValues.PENALTY_MEAL));
        cardCoffee.setOnClickListener(v -> select(DbValues.PENALTY_COFFEE));
        cardCustom.setOnClickListener(v -> select(DbValues.PENALTY_CUSTOM));
        cardNone.setOnClickListener(v -> select(DbValues.PENALTY_NONE));

        editCustom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (DbValues.PENALTY_CUSTOM.equals(draft.penalty_type)) {
                    draft.penalty_text = s.toString().trim();
                    updateButton();
                }
            }
        });

        buttonNext.setOnClickListener(v -> {
            // TODO 13번 요약 Fragment 만들면 ((CreatePromiseActivity) requireActivity()).showNext(...)
        });

        showSelected();
    }

    // 카드 하나 고르면 type/text 같이 채움
    private void select(String type) {
        draft.penalty_type = type;
        if (DbValues.PENALTY_MEAL.equals(type)) draft.penalty_text = getString(R.string.penalty_meal);
        else if (DbValues.PENALTY_COFFEE.equals(type)) draft.penalty_text = getString(R.string.penalty_coffee);
        else if (DbValues.PENALTY_NONE.equals(type)) draft.penalty_text = "";
        else draft.penalty_text = editCustom.getText().toString().trim(); // 직접 입력

        showSelected();
        if (DbValues.PENALTY_CUSTOM.equals(type)) editCustom.requestFocus();
    }

    private void showSelected() {
        String type = draft.penalty_type;
        cardMeal.setSelected(DbValues.PENALTY_MEAL.equals(type));
        cardCoffee.setSelected(DbValues.PENALTY_COFFEE.equals(type));
        cardCustom.setSelected(DbValues.PENALTY_CUSTOM.equals(type));
        cardNone.setSelected(DbValues.PENALTY_NONE.equals(type));
        editCustom.setVisibility(DbValues.PENALTY_CUSTOM.equals(type) ? View.VISIBLE : View.GONE);
        updateButton();
    }

    // 하나 골라야 하고, 직접 입력이면 내용도 있어야 다음 가능
    private void updateButton() {
        boolean ok = draft.penalty_type != null;
        if (DbValues.PENALTY_CUSTOM.equals(draft.penalty_type)) {
            ok = draft.penalty_text != null && !draft.penalty_text.isEmpty();
        }
        buttonNext.setEnabled(ok);
    }
}
