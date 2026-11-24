package com.goner.app.data.model;

import com.google.firebase.Timestamp;

// friendRequests/{id}
// 보낸 사람·받는 사람 정보를 복사해서 같이 저장. expiresAt = createdAt + 7일
public class FriendRequest {

    private String id;
    private String fromUid;
    private String fromFriendCode;
    private String fromNickname;
    private String fromPhotoUrl;
    private String toUid;
    private String toFriendCode;
    private String toNickname;
    private String toPhotoUrl;
    private Timestamp createdAt;
    private Timestamp expiresAt;

    public FriendRequest() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromUid() { return fromUid; }
    public void setFromUid(String fromUid) { this.fromUid = fromUid; }

    public String getFromFriendCode() { return fromFriendCode; }
    public void setFromFriendCode(String fromFriendCode) { this.fromFriendCode = fromFriendCode; }

    public String getFromNickname() { return fromNickname; }
    public void setFromNickname(String fromNickname) { this.fromNickname = fromNickname; }

    public String getFromPhotoUrl() { return fromPhotoUrl; }
    public void setFromPhotoUrl(String fromPhotoUrl) { this.fromPhotoUrl = fromPhotoUrl; }

    public String getToUid() { return toUid; }
    public void setToUid(String toUid) { this.toUid = toUid; }

    public String getToFriendCode() { return toFriendCode; }
    public void setToFriendCode(String toFriendCode) { this.toFriendCode = toFriendCode; }

    public String getToNickname() { return toNickname; }
    public void setToNickname(String toNickname) { this.toNickname = toNickname; }

    public String getToPhotoUrl() { return toPhotoUrl; }
    public void setToPhotoUrl(String toPhotoUrl) { this.toPhotoUrl = toPhotoUrl; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
}
