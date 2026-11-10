package com.goner.app.common;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.goner.app.R;

// 친구 초대 링크 (https://goner-ssu.web.app/i/{friendCode}). 공유 시트 + QR 내용에서 코드 꺼내기
// TODO 21번 InviteLink(약속 초대 링크) 만들 때 여기에 같이 넣기
public final class InviteLink {

    private static final String FRIEND_PREFIX = "https://goner-ssu.web.app/i/";

    private InviteLink() {
    }

    public static String friendUrl(String friendCode) {
        return FRIEND_PREFIX + friendCode;
    }

    // 친구 초대 링크를 공유 시트로 보냄
    public static void shareFriend(Context context, String friendCode) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, friendUrl(friendCode));
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
}
