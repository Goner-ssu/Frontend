package com.goner.app.promise;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.goner.app.R;

import java.util.Calendar;

// 한 달짜리 달력 (7열). 오늘 이전 날짜는 못 고름
public class MonthCalendarView extends GridLayout {

    public interface OnDateSelectedListener {
        void onDateSelected(int year, int month, int day);
    }

    private int shownYear, shownMonth;          // 지금 보여주는 달 (month는 Calendar 기준 0~11)
    private int selYear = -1, selMonth, selDay; // 선택한 날짜
    private OnDateSelectedListener listener;

    public MonthCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setColumnCount(7);
        Calendar now = Calendar.getInstance();
        shownYear = now.get(Calendar.YEAR);
        shownMonth = now.get(Calendar.MONTH);
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    public void setSelected(int year, int month, int day) {
        selYear = year;
        selMonth = month;
        selDay = day;
        showMonth(year, month);
    }

    public void showMonth(int year, int month) {
        shownYear = year;
        shownMonth = month;
        build();
    }

    public void showPrevMonth() {
        Calendar c = firstDayOfShownMonth();
        c.add(Calendar.MONTH, -1);
        showMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH));
    }

    public void showNextMonth() {
        Calendar c = firstDayOfShownMonth();
        c.add(Calendar.MONTH, 1);
        showMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH));
    }

    // 이번 달보다 앞으로는 못 감
    public boolean canGoPrev() {
        Calendar now = Calendar.getInstance();
        return shownYear > now.get(Calendar.YEAR)
                || (shownYear == now.get(Calendar.YEAR) && shownMonth > now.get(Calendar.MONTH));
    }

    public int getShownYear() {
        return shownYear;
    }

    public int getShownMonth() {
        return shownMonth;
    }

    private Calendar firstDayOfShownMonth() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(shownYear, shownMonth, 1);
        return c;
    }

    private void build() {
        removeAllViews();

        // 요일 줄
        for (String weekday : getResources().getStringArray(R.array.calendar_weekdays)) {
            TextView tv = makeCell(R.dimen.calendar_weekday_height);
            TextViewCompat.setTextAppearance(tv, R.style.Text_Label13);
            tv.setText(weekday);
            addView(tv);
        }

        Calendar first = firstDayOfShownMonth();
        int blanks = first.get(Calendar.DAY_OF_WEEK) - 1; // 일요일 시작
        int daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < blanks; i++) {
            addView(makeCell(R.dimen.calendar_cell_height));
        }

        for (int day = 1; day <= daysInMonth; day++) {
            TextView tv = makeDayCell(day);
            Calendar date = firstDayOfShownMonth();
            date.set(Calendar.DAY_OF_MONTH, day);

            boolean past = date.before(today);
            boolean selected = shownYear == selYear && shownMonth == selMonth && day == selDay;

            if (selected) {
                tv.setBackgroundResource(R.drawable.bg_day_selected);
                tv.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            } else if (past) {
                tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_hint));
            }

            if (!past) {
                final int d = day;
                tv.setOnClickListener(v -> {
                    setSelected(shownYear, shownMonth, d);
                    if (listener != null) listener.onDateSelected(shownYear, shownMonth, d);
                });
            }
            addView(tv);
        }

        // 마지막 줄 남는 칸은 다음 달 날짜 흐리게 (피그마처럼)
        int used = blanks + daysInMonth;
        int trailing = (7 - used % 7) % 7;
        for (int day = 1; day <= trailing; day++) {
            TextView tv = makeDayCell(day);
            tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_hint));
            addView(tv);
        }
    }

    private TextView makeDayCell(int day) {
        TextView tv = makeCell(R.dimen.calendar_cell_height);
        TextViewCompat.setTextAppearance(tv, R.style.Text_CalendarDay);
        tv.setText(String.valueOf(day));
        return tv;
    }

    // 7칸이 가로를 똑같이 나눠 가짐
    private TextView makeCell(int heightRes) {
        TextView tv = new TextView(getContext());
        tv.setGravity(Gravity.CENTER);
        tv.setIncludeFontPadding(false);
        LayoutParams lp = new LayoutParams(spec(UNDEFINED), spec(UNDEFINED, 1f));
        lp.width = 0;
        lp.height = getResources().getDimensionPixelSize(heightRes);
        tv.setLayoutParams(lp);
        tv.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        return tv;
    }
}
