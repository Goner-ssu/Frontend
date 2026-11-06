package com.goner.app.data.model;

import com.google.firebase.Timestamp;

// users/{ownerUid}/friends/{friendUid}
public class Friend {

    private String ownerUid;
    private String friendUid;
    private String friendCode;
    private String nickname;
    private String photoUrl;
    private Timestamp createdAt;

    public Friend() {
    }

    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

    public String getFriendUid() { return friendUid; }
    public void setFriendUid(String friendUid) { this.friendUid = friendUid; }

    public String getFriendCode() { return friendCode; }
    public void setFriendCode(String friendCode) { this.friendCode = friendCode; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
