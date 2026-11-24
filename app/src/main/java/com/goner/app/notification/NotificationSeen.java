package com.goner.app.notification;

import android.content.Context;
import android.content.SharedPreferences;

import com.goner.app.data.model.Appointment;
import com.goner.app.data.model.FriendRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 알림 읽음/삭제 기록 (DB 에 알림 문서가 없어서 이 폰 SharedPreferences 에 key 로 저장)
// 알림 화면과 홈 벨 점이 같이 씀
public final class NotificationSeen {

    private static final String PREFS = "notifications";
    private static final String KEY_HIDDEN = "hidden"; // 삭제한 알림
    private static final String KEY_SEEN = "seen";     // 한 번 본 알림

    private NotificationSeen() {
    }

    public static String friendKey(FriendRequest r) {
        return "friend:" + r.getId();
    }

    public static String inviteKey(Appointment a) {
        return "invite:" + a.getId();
    }

    public static Set<String> hidden(Context context) {
        return new HashSet<>(prefs(context).getStringSet(KEY_HIDDEN, new HashSet<>()));
    }

    public static Set<String> seen(Context context) {
        return new HashSet<>(prefs(context).getStringSet(KEY_SEEN, new HashSet<>()));
    }

    public static void saveSeen(Context context, Set<String> seen) {
        prefs(context).edit().putStringSet(KEY_SEEN, seen).apply();
    }

    public static void addHidden(Context context, Set<String> keys) {
        Set<String> hidden = hidden(context);
        hidden.addAll(keys);
        prefs(context).edit().putStringSet(KEY_HIDDEN, hidden).apply();
    }

    // 삭제 안 했고 아직 안 본 알림이 하나라도 있으면 true (홈 벨 점)
    public static boolean hasUnread(Context context, List<FriendRequest> requests, List<Appointment> invites) {
        Set<String> hidden = hidden(context);
        Set<String> seen = seen(context);
        for (FriendRequest r : requests) {
            String key = friendKey(r);
            if (!hidden.contains(key) && !seen.contains(key)) return true;
        }
        for (Appointment a : invites) {
            String key = inviteKey(a);
            if (!hidden.contains(key) && !seen.contains(key)) return true;
        }
        return false;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
