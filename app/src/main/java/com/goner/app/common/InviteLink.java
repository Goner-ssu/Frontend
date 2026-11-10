package com.goner.app.common;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.goner.app.R;

import java.util.List;

// 초대 링크 (Firebase Dynamic Links 는 종료돼서 안 씀, 그냥 https 링크 + App Links)
//   친구: https://goner-ssu.web.app/i/{friendCode}
//   약속: https://goner-ssu.web.app/p/{appointmentId}
// 링크로 앱이 열리면 friend/InviteLandingActivity 가 받음 (AndroidManifest intent-filter, docs/APP_LINKS.md)
public final class InviteLink {

    private static final String BASE = "https://goner-ssu.web.app";
    private static final String FRIEND_PREFIX = BASE + "/i/";
    private static final String PATH_FRIEND = "i";
    private static final String PATH_APPOINTMENT = "p";

    // 로그인 전에 친구 링크로 들어온 경우 코드 기억해 둠 (로그인·온보딩 끝나고 홈 오면 다시 초대 화면)
    private static final String PREFS = "invite";
    private static final String KEY_PENDING_CODE = "pending_friend_code";

    private InviteLink() {
    }

    public static String friendUrl(String friendCode) {
        return FRIEND_PREFIX + friendCode;
    }

    public static String appointmentUrl(String appointmentId) {
        return BASE + "/" + PATH_APPOINTMENT + "/" + appointmentId;
    }

    // 친구 초대 링크를 공유 시트로 보냄
    public static void shareFriend(Context context, String friendCode) {
        share(context, friendUrl(friendCode));
    }

    // 약속 초대 링크 공유 (약속 생성 완료 화면)
    public static void shareAppointment(Context context, String appointmentId) {
        share(context, appointmentUrl(appointmentId));
    }

    private static void share(Context context, String url) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, url);
        context.startActivity(Intent.createChooser(send, null));
    }

    // 친구 코드 클립보드에 복사 (친구 탭, 친구 추가)
    public static void copyCode(Context context, String friendCode) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("friendCode", friendCode));
        Toast.makeText(context, R.string.friend_copied, Toast.LENGTH_SHORT).show();
    }

    // QR 내용이 초대 링크면 끝의 코드만, 아니면 그대로 코드로 봄 (소문자, 공백 제거)
    @Nullable
    public static String codeFrom(@Nullable String raw) {
        if (raw == null) return null;
        String text = raw.trim();
        if (text.startsWith(FRIEND_PREFIX)) text = text.substring(FRIEND_PREFIX.length());
        int slash = text.indexOf('/');
        if (slash >= 0) text = text.substring(0, slash);
        text = text.toLowerCase();
        return text.isEmpty() ? null : text;
    }

    // 앱 링크 /i/{code} 에서 친구 코드 (아니면 null)
    @Nullable
    public static String friendCodeFrom(@Nullable Uri uri) {
        String value = segmentAfter(uri, PATH_FRIEND);
        return value == null ? null : value.toLowerCase();
    }

    // 앱 링크 /p/{id} 에서 약속 id (아니면 null)
    @Nullable
    public static String appointmentIdFrom(@Nullable Uri uri) {
        return segmentAfter(uri, PATH_APPOINTMENT);
    }

    @Nullable
    private static String segmentAfter(@Nullable Uri uri, String first) {
        if (uri == null) return null;
        List<String> parts = uri.getPathSegments();
        if (parts.size() < 2 || !parts.get(0).equals(first) || parts.get(1).isEmpty()) return null;
        return parts.get(1);
    }

    public static void savePendingFriendCode(Context context, String friendCode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_PENDING_CODE, friendCode).apply();
    }

    // 기억해 둔 코드 꺼내고 지움 (없으면 null)
    @Nullable
    public static String takePendingFriendCode(Context context) {
        String code = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PENDING_CODE, null);
        if (code != null) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_PENDING_CODE).apply();
        }
        return code;
    }
}
