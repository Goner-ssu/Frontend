package com.goner.app.notification;

import com.goner.app.data.model.FriendRequest;

import java.util.Date;

// 알림 한 줄 (DB 문서 아님. 친구 요청 / 약속 초대를 화면용으로 바꾼 것)
public class NotificationItem {

    public static final int TYPE_FRIEND = 0;
    public static final int TYPE_INVITE = 1;

    int type;
    String key;       // 삭제·읽음 저장용 이름 (종류 + id)
    String uid;       // 아바타 색
    String name;      // 굵게 나오는 이름
    String photoUrl;
    String sub;       // 둘째 줄 (@친구코드 / 약속 이름 · 시간)
    String place;     // 약속 초대 셋째 줄 장소
    Date time;
    boolean unread;
    FriendRequest request;  // 친구 요청일 때만
    String appointmentId;   // 약속 초대일 때만
}
