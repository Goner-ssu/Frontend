package com.goner.app.promise;

import com.goner.app.data.model.Friend;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

// 약속 만들기 단계별 입력값 모아두는 곳 (마지막에 appointments 문서로 저장)
public class AppointmentDraft {

    public String title;
    public Timestamp appointmentTime;
    public String placeName;
    public String placeAddress;
    public double placeLat;
    public double placeLng;
    public List<Friend> selectedFriends = new ArrayList<>();
    public String penalty_type;
    public String penalty_text;
}
