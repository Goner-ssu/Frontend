package com.goner.app.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Firestore 조회/쓰기. 모든 메서드는 Task 를 돌려주며, 결과 처리는 호출하는 화면에서 한다. */
public final class FirestoreRepo {

    public static final Pattern FRIEND_CODE_PATTERN = Pattern.compile("^[a-z0-9]{4,16}$");

    private static final String USERS = "users";
    private static final String FRIENDS = "friends";
    private static final String NOTIFICATIONS = "notifications";
    private static final String FRIEND_REQUESTS = "friendRequests";
    private static final String PROMISES = "promises";

    private static FirestoreRepo instance;

    private final FirebaseFirestore db;

    private FirestoreRepo(FirebaseFirestore db) {
        this.db = db;
    }

    public static synchronized FirestoreRepo get() {
        if (instance == null) {
            instance = new FirestoreRepo(FirebaseFirestore.getInstance());
        }
        return instance;
    }

    // ---------- users ----------

    private DocumentReference userDoc(String uid) {
        return db.collection(USERS).document(uid);
    }

    /** 문서가 없으면 null. */
    public Task<User> getUser(@NonNull String uid) {
        return userDoc(uid).get().continueWith(t -> t.getResult().toObject(User.class));
    }

    /** createdAt·friendCodeChangedAt 은 서버 시간으로 채워진다. */
    public Task<Void> createUser(@NonNull User user) {
        return userDoc(user.uid).set(user);
    }

    public Task<Void> updateNickname(@NonNull String uid, @NonNull String nickname) {
        return userDoc(uid).update("nickname", nickname);
    }

    public Task<Void> updatePhotoUrl(@NonNull String uid, @Nullable String photoUrl) {
        return userDoc(uid).update("photoUrl", photoUrl);
    }

    public Task<Void> updateFriendCode(@NonNull String uid, @NonNull String friendCode) {
        return userDoc(uid).update(
                "friendCode", friendCode,
                "friendCodeChangedAt", FieldValue.serverTimestamp());
    }

    /** 해당 친구 코드를 가진 사용자. 없으면 null. */
    public Task<User> findUserByFriendCode(@NonNull String friendCode) {
        return db.collection(USERS).whereEqualTo("friendCode", friendCode).limit(1).get()
                .continueWith(t -> t.getResult().isEmpty()
                        ? null
                        : t.getResult().getDocuments().get(0).toObject(User.class));
    }

    /** 아무도 쓰지 않는 코드면 true. 형식 검사는 FRIEND_CODE_PATTERN 으로 먼저 한다. */
    public Task<Boolean> isFriendCodeAvailable(@NonNull String friendCode) {
        return findUserByFriendCode(friendCode).continueWith(t -> t.getResult() == null);
    }

    // ---------- friends ----------

    public Task<List<String>> getFriendUids(@NonNull String uid) {
        return userDoc(uid).collection(FRIENDS).get().continueWith(t -> {
            List<String> uids = new ArrayList<>();
            for (DocumentSnapshot doc : t.getResult().getDocuments()) {
                uids.add(doc.getId());
            }
            return uids;
        });
    }

    /** 양쪽 friends 문서를 함께 지운다. */
    public Task<Void> removeFriend(@NonNull String uid, @NonNull String friendUid) {
        WriteBatch batch = db.batch();
        batch.delete(userDoc(uid).collection(FRIENDS).document(friendUid));
        batch.delete(userDoc(friendUid).collection(FRIENDS).document(uid));
        return batch.commit();
    }

    // ---------- friendRequests ----------

    public Task<DocumentReference> sendFriendRequest(@NonNull String fromUid, @NonNull String toUid) {
        FriendRequest request = new FriendRequest();
        request.fromUid = fromUid;
        request.toUid = toUid;
        request.status = FriendRequest.STATUS_PENDING;
        return db.collection(FRIEND_REQUESTS).add(request);
    }

    /** 내가 받은 대기 중 요청(7일 지난 것 제외). */
    public Task<List<FriendRequest>> getIncomingRequests(@NonNull String uid) {
        return pendingRequests("toUid", uid);
    }

    /** 내가 보낸 대기 중 요청(7일 지난 것 제외). */
    public Task<List<FriendRequest>> getOutgoingRequests(@NonNull String uid) {
        return pendingRequests("fromUid", uid);
    }

    private Task<List<FriendRequest>> pendingRequests(String field, String uid) {
        return db.collection(FRIEND_REQUESTS)
                .whereEqualTo(field, uid)
                .whereEqualTo("status", FriendRequest.STATUS_PENDING)
                .get()
                .continueWith(t -> {
                    List<FriendRequest> result = new ArrayList<>();
                    for (FriendRequest r : t.getResult().toObjects(FriendRequest.class)) {
                        if (!r.isExpired()) {
                            result.add(r);
                        }
                    }
                    return result;
                });
    }

    /** 요청을 수락하고 양쪽 friends 에 서로를 추가한다. */
    public Task<Void> acceptFriendRequest(@NonNull FriendRequest request) {
        WriteBatch batch = db.batch();
        batch.update(db.collection(FRIEND_REQUESTS).document(request.id),
                "status", FriendRequest.STATUS_ACCEPTED);
        Map<String, Object> since = Collections.singletonMap("since", FieldValue.serverTimestamp());
        batch.set(userDoc(request.toUid).collection(FRIENDS).document(request.fromUid), since);
        batch.set(userDoc(request.fromUid).collection(FRIENDS).document(request.toUid), since);
        return batch.commit();
    }

    /** 거절(rejected) 또는 보낸 쪽 취소(canceled). */
    public Task<Void> updateFriendRequestStatus(@NonNull String requestId, @NonNull String status) {
        return db.collection(FRIEND_REQUESTS).document(requestId).update("status", status);
    }

    // ---------- promises ----------

    /** 문서가 없으면 null. */
    public Task<Promise> getPromise(@NonNull String promiseId) {
        return db.collection(PROMISES).document(promiseId).get()
                .continueWith(t -> t.getResult().toObject(Promise.class));
    }

    /** 새 약속 문서를 만들고 id 를 돌려준다. */
    public Task<String> createPromise(@NonNull Promise promise) {
        DocumentReference ref = db.collection(PROMISES).document();
        return ref.set(promise).continueWith(t -> {
            t.getResult();
            return ref.getId();
        });
    }

    /** 내가 초대됐거나 참여 중인 약속, 시간순. */
    public Task<List<Promise>> getMyPromises(@NonNull String uid) {
        return db.collection(PROMISES)
                .whereIn("members." + uid, Arrays.asList(Promise.MEMBER_INVITED, Promise.MEMBER_JOINED))
                .get()
                .continueWith(t -> {
                    List<Promise> promises = t.getResult().toObjects(Promise.class);
                    promises.sort(Comparator.comparing(p -> p.time,
                            Comparator.nullsLast(Comparator.naturalOrder())));
                    return promises;
                });
    }

    /** 초대 응답: joined | rejected */
    public Task<Void> updateMemberStatus(@NonNull String promiseId, @NonNull String uid,
                                         @NonNull String memberStatus) {
        return db.collection(PROMISES).document(promiseId).update("members." + uid, memberStatus);
    }

    public Task<Void> updatePromiseStatus(@NonNull String promiseId, @NonNull String status) {
        return db.collection(PROMISES).document(promiseId).update("status", status);
    }

    // ---------- notifications ----------

    private CollectionReference notifications(String uid) {
        return userDoc(uid).collection(NOTIFICATIONS);
    }

    /** 최신순. */
    public Task<List<AppNotification>> getNotifications(@NonNull String uid) {
        return notifications(uid).orderBy("createdAt", Query.Direction.DESCENDING).get()
                .continueWith(t -> t.getResult().toObjects(AppNotification.class));
    }

    /** toUid 사용자에게 알림을 추가한다. createdAt 은 서버 시간. */
    public Task<DocumentReference> addNotification(@NonNull String toUid,
                                                   @NonNull AppNotification notification) {
        return notifications(toUid).add(notification);
    }

    public Task<Void> markNotificationRead(@NonNull String uid, @NonNull String notificationId) {
        return notifications(uid).document(notificationId).update("read", true);
    }

    public Task<Void> deleteNotification(@NonNull String uid, @NonNull String notificationId) {
        return notifications(uid).document(notificationId).delete();
    }
}
