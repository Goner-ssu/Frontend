package com.goner.app.promise;

import androidx.lifecycle.ViewModel;

// 화면 돌려도 입력값 유지되게 ViewModel에 둠
public class CreatePromiseViewModel extends ViewModel {

    private final AppointmentDraft draft = new AppointmentDraft();

    public AppointmentDraft getDraft() {
        return draft;
    }
}
