package com.goner.app.promise;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.goner.app.R;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

// 약속 만들기 2단계: 날짜 + 시간
public class PromiseTimeFragment extends Fragment {

    private static final int MINUTE_STEP = 5;
    private static final int YEAR_RANGE = 5; // 올해부터 5년 뒤까지 고를 수 있음

    private AppointmentDraft draft;
    private MonthCalendarView calendarView;
    private WheelPicker wheelHour, wheelMinute, wheelAmPm;
    private TextView textMonth, textYear;
    private View buttonPrev;
    private Button buttonNext;

    // 달력에서 고른 날짜
    private int year, month, day;

    public PromiseTimeFragment() {
        super(R.layout.fragment_create_time);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        draft = new ViewModelProvider(requireActivity()).get(CreatePromiseViewModel.class).getDraft();

        calendarView = view.findViewById(R.id.calendar);
        wheelHour = view.findViewById(R.id.wheel_hour);
        wheelMinute = view.findViewById(R.id.wheel_minute);
        wheelAmPm = view.findViewById(R.id.wheel_ampm);
        textMonth = view.findViewById(R.id.text_month);
        textYear = view.findViewById(R.id.text_year);
        buttonPrev = view.findViewById(R.id.button_prev_month);
        buttonNext = view.findViewById(R.id.button_next);

        // 휠에 들어갈 값: 1~12시, 00~55분(5분 단위), AM/PM
        List<String> hours = new ArrayList<>();
        for (int h = 1; h <= 12; h++) hours.add(String.valueOf(h));
        List<String> minutes = new ArrayList<>();
        for (int m = 0; m < 60; m += MINUTE_STEP) minutes.add(String.format("%02d", m));
        wheelHour.setItems(hours);
        wheelMinute.setItems(minutes);
        wheelAmPm.setItems(Arrays.asList("AM", "PM"));

        // 처음 값: 전에 고른 시간 있으면 그거, 없으면 지금부터 1시간 뒤(5분 단위로 올림)
        Calendar start = Calendar.getInstance();
        if (draft.appointmentTime != null) {
            start.setTime(draft.appointmentTime.toDate());
        } else {
            start.add(Calendar.HOUR_OF_DAY, 1);
            int m = start.get(Calendar.MINUTE);
            start.add(Calendar.MINUTE, (MINUTE_STEP - m % MINUTE_STEP) % MINUTE_STEP);
        }
        year = start.get(Calendar.YEAR);
        month = start.get(Calendar.MONTH);
        day = start.get(Calendar.DAY_OF_MONTH);
        calendarView.setSelected(year, month, day);

        int hour12 = start.get(Calendar.HOUR) == 0 ? 12 : start.get(Calendar.HOUR);
        wheelHour.setSelected(hour12 - 1);
        wheelMinute.setSelected(start.get(Calendar.MINUTE) / MINUTE_STEP);
        wheelAmPm.setSelected(start.get(Calendar.AM_PM));

        calendarView.setOnDateSelectedListener((y, mo, d) -> {
            year = y;
            month = mo;
            day = d;
            updateButton();
        });
        wheelHour.setOnValueChangeListener(i -> updateButton());
        wheelMinute.setOnValueChangeListener(i -> updateButton());
        wheelAmPm.setOnValueChangeListener(i -> updateButton());

        buttonPrev.setOnClickListener(v -> {
            calendarView.showPrevMonth();
            updateHeader();
        });
        view.findViewById(R.id.button_next_month).setOnClickListener(v -> {
            calendarView.showNextMonth();
            updateHeader();
        });

        textMonth.setOnClickListener(v -> showMonthList());
        textYear.setOnClickListener(v -> showYearList());

        buttonNext.setOnClickListener(v -> {
            Calendar picked = getPickedTime();
            if (!picked.after(Calendar.getInstance())) { // 고르고 시간이 지나버린 경우
                updateButton();
                return;
            }
            draft.appointmentTime = new Timestamp(picked.getTime());
            ((CreatePromiseActivity) requireActivity()).showNext(new PromisePlaceFragment());
        });

        updateHeader();
        updateButton();
    }

    // 고른 날짜 + 휠 시간을 하나로 합침
    private Calendar getPickedTime() {
        int hour = wheelHour.getSelected() + 1;           // 1~12
        if (hour == 12) hour = 0;                          // 12시는 0으로 바꾼 뒤
        if (wheelAmPm.getSelected() == 1) hour += 12;      // PM이면 12 더함

        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, day, hour, wheelMinute.getSelected() * MINUTE_STEP);
        return c;
    }

    private void updateHeader() {
        textMonth.setText(getString(R.string.calendar_month, calendarView.getShownMonth() + 1));
        textYear.setText(String.valueOf(calendarView.getShownYear()));
        boolean canPrev = calendarView.canGoPrev();
        buttonPrev.setEnabled(canPrev);
        buttonPrev.setAlpha(canPrev ? 1f : 0.3f);
    }

    // 월 목록 (올해면 지난 달은 뺌)
    private void showMonthList() {
        Calendar now = Calendar.getInstance();
        int shownYear = calendarView.getShownYear();
        int firstMonth = shownYear == now.get(Calendar.YEAR) ? now.get(Calendar.MONTH) : 0;

        List<String> labels = new ArrayList<>();
        for (int m = firstMonth; m < 12; m++) labels.add(getString(R.string.calendar_month, m + 1));

        showList(textMonth, labels, position -> {
            calendarView.showMonth(shownYear, firstMonth + position);
            updateHeader();
        });
    }

    // 연도 목록 (올해 ~ 5년 뒤)
    private void showYearList() {
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        List<String> labels = new ArrayList<>();
        for (int y = thisYear; y <= thisYear + YEAR_RANGE; y++) labels.add(getString(R.string.calendar_year, y));

        showList(textYear, labels, position -> {
            int newYear = thisYear + position;
            int newMonth = calendarView.getShownMonth();
            // 올해로 바꿨는데 지난 달이면 이번 달로
            Calendar now = Calendar.getInstance();
            if (newYear == thisYear && newMonth < now.get(Calendar.MONTH)) newMonth = now.get(Calendar.MONTH);
            calendarView.showMonth(newYear, newMonth);
            updateHeader();
        });
    }

    private interface OnPick {
        void onPick(int position);
    }

    // 버튼 아래로 목록 펼치기
    private void showList(View anchor, List<String> labels, OnPick onPick) {
        ListPopupWindow popup = new ListPopupWindow(requireContext());
        popup.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, labels));
        popup.setAnchorView(anchor);
        popup.setWidth(getResources().getDimensionPixelSize(R.dimen.dropdown_width));
        int itemHeight = getResources().getDimensionPixelSize(R.dimen.dropdown_item_height);
        int maxHeight = getResources().getDimensionPixelSize(R.dimen.dropdown_max_height);
        popup.setHeight(Math.min(itemHeight * labels.size(), maxHeight));
        popup.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_dialog));
        popup.setModal(true);
        popup.setOnItemClickListener((parent, view, position, id) -> {
            onPick.onPick(position);
            popup.dismiss();
        });
        popup.show();
    }

    // 지금 이후 시간일 때만 다음 가능
    private void updateButton() {
        buttonNext.setEnabled(getPickedTime().after(Calendar.getInstance()));
    }
}
