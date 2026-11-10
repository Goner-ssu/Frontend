package com.goner.app.data.model;

// friendCodes/{friendCode} — 친구 코드로 uid 찾기용 (코드가 문서 이름이라 유일함)
public class FriendCode {

    private String uid;

    public FriendCode() {
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
}
