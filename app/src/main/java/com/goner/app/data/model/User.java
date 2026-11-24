package com.goner.app.data.model;

import com.google.firebase.Timestamp;

// users/{uid}
public class User {

    private String uid;
    private String friendCode;
    private String nickname;
    private String photoUrl;
    private boolean notiEnabled;
    private long appointmentCount;  // 서버/레이더가 씀, 화면에서는 읽기만
    private long lateCount;
    private long totalLateMinutes;
    private Timestamp createdAt;

    public User() {
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFriendCode() { return friendCode; }
    public void setFriendCode(String friendCode) { this.friendCode = friendCode; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public boolean isNotiEnabled() { return notiEnabled; }
    public void setNotiEnabled(boolean notiEnabled) { this.notiEnabled = notiEnabled; }

    public long getAppointmentCount() { return appointmentCount; }
    public void setAppointmentCount(long appointmentCount) { this.appointmentCount = appointmentCount; }

    public long getLateCount() { return lateCount; }
    public void setLateCount(long lateCount) { this.lateCount = lateCount; }

    public long getTotalLateMinutes() { return totalLateMinutes; }
    public void setTotalLateMinutes(long totalLateMinutes) { this.totalLateMinutes = totalLateMinutes; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
