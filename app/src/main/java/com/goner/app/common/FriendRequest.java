package com.goner.app.common;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.concurrent.TimeUnit;

/** friendRequests/{id} */
public class FriendRequest {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_CANCELED = "canceled";
    public static final String STATUS_REJECTED = "rejected";

    public static final long EXPIRE_MILLIS = TimeUnit.DAYS.toMillis(7);

    @DocumentId
    public String id;
    public String fromUid;
    public String toUid;
    public String status;
    @ServerTimestamp
    public Timestamp createdAt;

    /** Firestore 역직렬화용 */
    public FriendRequest() {
    }

    /** 보낸 지 7일이 지났으면 만료. */
    @Exclude
    public boolean isExpired() {
        return createdAt != null
                && System.currentTimeMillis() - createdAt.toDate().getTime() > EXPIRE_MILLIS;
    }
}
