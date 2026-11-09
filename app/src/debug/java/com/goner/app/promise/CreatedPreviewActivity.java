package com.goner.app.promise;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;

import com.goner.app.data.model.Friend;
import com.google.firebase.Timestamp;

import java.util.Calendar;

// 임시 확인용 (확인 끝나면 삭제)
public class CreatedPreviewActivity extends CreatePromiseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppointmentDraft d = new ViewModelProvider(this).get(CreatePromiseViewModel.class).getDraft();
        d.title = "홍대 저녁 모임";
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 24, 19, 0);
        d.appointmentTime = new Timestamp(c.getTime());
        d.placeName = "강남역 11번 출구";
        String[] names = {"민수", "지현", "준호"};
        for (int i = 0; i < names.length; i++) {
            Friend f = new Friend();
            f.setFriendUid("u" + i);
            f.setNickname(names[i]);
            d.selectedFriends.add(f);
        }
        if (savedInstanceState == null) showCreated("test");
    }
}
