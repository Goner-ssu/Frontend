package com.goner.app.common;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

/** users/{uid}/notifications/{id} */
public class AppNotification {

    public static final String TYPE_POKE = "poke";
    public static final String TYPE_POSTPONE = "postpone";
    public static final String TYPE_FRIEND_REQUEST = "friendRequest";
    public static final String TYPE_NEW_PROMISE = "newPromise";
    public static final String TYPE_INVITE = "invite";
    public static final String TYPE_FRIEND_ACCEPTED = "friendAccepted";

    @DocumentId
    public String id;
    public String type;
    public String fromUid;
    public String promiseId;
    public String text;
    public boolean read;
    @ServerTimestamp
    public Timestamp createdAt;

    /** Firestore 역직렬화용 */
    public AppNotification() {
    }
}
