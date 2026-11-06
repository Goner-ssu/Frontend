package com.goner.app.common;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.HashMap;
import java.util.Map;

/** promises/{id} */
public class Promise {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_DONE = "done";

    public static final String MEMBER_INVITED = "invited";
    public static final String MEMBER_JOINED = "joined";
    public static final String MEMBER_REJECTED = "rejected";

    @DocumentId
    public String id;
    public String title;
    public Timestamp time;
    public Place place;
    public String penalty;
    public String hostUid;
    /** uid → invited | joined | rejected */
    public Map<String, String> members = new HashMap<>();
    public String status;

    /** Firestore 역직렬화용 */
    public Promise() {
    }

    public static class Place {
        public String name;
        public String address;
        public double lat;
        public double lng;

        /** Firestore 역직렬화용 */
        public Place() {
        }
    }
}
