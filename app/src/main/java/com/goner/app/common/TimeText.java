package com.goner.app.common;

import android.content.Context;

import com.goner.app.R;

import java.util.Calendar;
import java.util.Date;

// "방금 전", "3시간 전" 같은 상대 시간 (알림, 친구 요청이 같이 씀)
public final class TimeText {

    private TimeText() {
    }

    // 방금 전 / N분 전 / N시간 전 / 어제 / N일 전 / M월 d일
    public static String ago(Context context, Date time) {
        long minutes = (System.currentTimeMillis() - time.getTime()) / 60000;
        long days = daysAgo(time);
        if (minutes < 1) return context.getString(R.string.noti_just_now);
        if (minutes < 60) return context.getString(R.string.noti_minutes_ago, (int) minutes);
        if (days == 0) return context.getString(R.string.noti_hours_ago, (int) (minutes / 60));
        if (days == 1) return context.getString(R.string.noti_yesterday);
        if (days < 7) return context.getString(R.string.noti_days_ago, (int) days);
        Calendar c = Calendar.getInstance();
        c.setTime(time);
        return context.getString(R.string.noti_date, c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    // 날짜 기준 며칠 전인지 (어제 밤 11시도 "어제")
    public static long daysAgo(Date time) {
        Calendar today = Calendar.getInstance();
        setMidnight(today);
        Calendar that = Calendar.getInstance();
        that.setTime(time);
        setMidnight(that);
        return Math.round((today.getTimeInMillis() - that.getTimeInMillis()) / 86400000.0);
    }

    private static void setMidnight(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }
}
