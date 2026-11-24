package com.goner.app.data;

// Firestore / Realtime DB 경로 이름은 여기에만 둠 (팀 ERD 기준)
public final class DbPaths {

    private DbPaths() {
    }

    // Firestore
    public static final String USERS = "users";
    public static final String FRIEND_CODES = "friendCodes";
    public static final String FCM_TOKENS = "fcmTokens";
    public static final String FRIENDS = "friends";               // users/{uid}/friends
    public static final String FRIEND_REQUESTS = "friendRequests";
    public static final String APPOINTMENTS = "appointments";
    public static final String PARTICIPANTS = "participants";     // appointments/{id}/participants
    public static final String POSTPONE_VOTES = "postponeVotes";  // appointments/{id}/postponeVotes
    public static final String RECORDS = "records";               // users/{uid}/records

    // Realtime Database
    public static final String RTDB_MEMBERS = "members";          // members/{appointmentId}/{uid}
}
