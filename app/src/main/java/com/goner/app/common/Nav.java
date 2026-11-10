package com.goner.app.common;

import android.content.Context;

// 다른 팀원이 만드는 화면으로 가는 입구. 지금은 비어 있음 (마이페이지에서 호출만 함)
public final class Nav {

    // openTerms type
    public static final String TERMS_SERVICE = "service";
    public static final String TERMS_PRIVACY = "privacy";
    public static final String TERMS_LOCATION = "location";

    private Nav() {
    }

    // TODO 프로필 수정 화면 (담당 팀원)
    public static void openEditProfile(Context context) {
    }

    // TODO 로그아웃 확인 팝업 (담당 팀원)
    public static void openLogoutDialog(Context context) {
    }

    // TODO 약관 화면 (담당 팀원). type = TERMS_SERVICE / TERMS_PRIVACY / TERMS_LOCATION
    public static void openTerms(Context context, String type) {
    }

    // TODO 회원탈퇴 화면 (담당 팀원)
    public static void openWithdraw(Context context) {
    }
}
