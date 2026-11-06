package com.goner.app.common;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

/** users/{uid} */
public class User {

    @DocumentId
    public String uid;
    public String nickname;
    /** 유일, 영문 소문자·숫자 4~16자 */
    public String friendCode;
    public String photoUrl;
    @ServerTimestamp
    public Timestamp createdAt;
    @ServerTimestamp
    public Timestamp friendCodeChangedAt;

    /** Firestore 역직렬화용 */
    public User() {
    }
}
