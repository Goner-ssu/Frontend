package com.goner.app.promise;

import androidx.lifecycle.ViewModel;

// 화면 돌려도 입력값 유지되게 ViewModel에 둠
public class CreatePromiseViewModel extends ViewModel {

    private final AppointmentDraft draft = new AppointmentDraft();

    // 테스트용: 친구 단계에서 가짜 친구 쓰는 중인지 (debug에서만 켜짐)
    boolean useTestFriends = false;

    public AppointmentDraft getDraft() {
        return draft;
    }
}
