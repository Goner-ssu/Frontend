package com.goner.app.data.repo;

import com.goner.app.data.DbPaths;
import com.goner.app.data.model.Friend;
import com.goner.app.data.model.FriendCode;
import com.goner.app.data.model.FriendRequest;
import com.goner.app.data.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

// 친구 관련 Firestore 접근
public class FriendRepo {

    private static final long REQUEST_EXPIRE_MILLIS = TimeUnit.DAYS.toMillis(7);

    // 코드 검색 결과 (친구 추가 검색, QR 스캔이 같이 씀)
    public static class SearchResult {
        public static final int NOT_FOUND = 0; // 없는 코드
        public static final int ME = 1;        // 내 코드
        public static final int FRIEND = 2;    // 이미 친구
        public static final int SENT = 3;      // 내가 이미 요청 보냄
        public static final int RECEIVED = 4;  // 상대가 나한테 요청 보냄 → 수락 가능
        public static final int NONE = 5;      // 요청 보낼 수 있음

        public int state;
        public User user;              // NOT_FOUND 면 null
        public FriendRequest request;  // SENT / RECEIVED 일 때 그 요청
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // 내 친구 목록 (users/{내uid}/friends), 이름순
    public Task<List<Friend>> listFriends() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return Tasks.forResult(new ArrayList<>()); // 로그인 전이면 빈 목록

        return db.collection(DbPaths.USERS).document(uid)
                .collection(DbPaths.FRIENDS)
                .orderBy("nickname")
                .get()
                .continueWith(task -> task.getResult().toObjects(Friend.class));
    }

    // 친구 삭제: 양쪽 friends 문서 둘 다 지움
    public Task<Void> removeFriend(String myUid, String friendUid) {
        WriteBatch batch = db.batch();
        batch.delete(db.collection(DbPaths.USERS).document(myUid).collection(DbPaths.FRIENDS).document(friendUid));
        batch.delete(db.collection(DbPaths.USERS).document(friendUid).collection(DbPaths.FRIENDS).document(myUid));
        return batch.commit();
    }

    // 내가 보낸 요청 (안 지난 것, 최신순)
    public Task<List<FriendRequest>> listSent(String uid) {
        return listRequests("fromUid", uid);
    }

    // 나한테 온 요청 (안 지난 것, 최신순)
    public Task<List<FriendRequest>> listReceived(String uid) {
        return listRequests("toUid", uid);
    }

    // expiresAt 거르기·정렬은 받아와서 함 (복합 색인 안 만들려고)
    private Task<List<FriendRequest>> listRequests(String field, String uid) {
        return db.collection(DbPaths.FRIEND_REQUESTS)
                .whereEqualTo(field, uid)
                .get()
                .continueWith(task -> {
                    Timestamp now = Timestamp.now();
                    List<FriendRequest> list = new ArrayList<>();
                    for (FriendRequest r : task.getResult().toObjects(FriendRequest.class)) {
                        if (r.getExpiresAt() != null && r.getExpiresAt().compareTo(now) > 0) list.add(r);
                    }
                    list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    return list;
                });
    }

    // 친구 요청 수락: 서로의 friends 에 상대 정보 복사해서 넣고 요청 문서는 지움 (WriteBatch 로 한 번에)
    // TODO 보안 규칙에서 상대 friends 쓰기가 막히면 서버 함수 acceptFriendRequest 로 교체
    public Task<Void> accept(FriendRequest request) {
        Timestamp now = Timestamp.now();

        Friend sender = new Friend(); // 내 목록에 들어갈 보낸 사람
        sender.setOwnerUid(request.getToUid());
        sender.setFriendUid(request.getFromUid());
        sender.setFriendCode(request.getFromFriendCode());
        sender.setNickname(request.getFromNickname());
        sender.setPhotoUrl(request.getFromPhotoUrl());
        sender.setCreatedAt(now);

        Friend me = new Friend(); // 보낸 사람 목록에 들어갈 나
        me.setOwnerUid(request.getFromUid());
        me.setFriendUid(request.getToUid());
        me.setFriendCode(request.getToFriendCode());
        me.setNickname(request.getToNickname());
        me.setPhotoUrl(request.getToPhotoUrl());
        me.setCreatedAt(now);

        WriteBatch batch = db.batch();
        batch.set(db.collection(DbPaths.USERS).document(request.getToUid())
                .collection(DbPaths.FRIENDS).document(request.getFromUid()), sender);
        batch.set(db.collection(DbPaths.USERS).document(request.getFromUid())
                .collection(DbPaths.FRIENDS).document(request.getToUid()), me);
        batch.delete(db.collection(DbPaths.FRIEND_REQUESTS).document(request.getId()));
        return batch.commit();
    }

    // 요청 지우기 = 거절(받은 쪽) / 취소(보낸 쪽) 둘 다 문서 삭제
    public Task<Void> deleteRequest(FriendRequest request) {
        return db.collection(DbPaths.FRIEND_REQUESTS).document(request.getId()).delete();
    }

    // 친구 요청 보내기. 내 정보·상대 정보 복사해서 저장, 7일 뒤 만료
    public Task<FriendRequest> sendRequest(User me, User to) {
        DocumentReference ref = db.collection(DbPaths.FRIEND_REQUESTS).document();
        Date now = new Date();
        FriendRequest r = new FriendRequest();
        r.setId(ref.getId());
        r.setFromUid(me.getUid());
        r.setFromFriendCode(me.getFriendCode());
        r.setFromNickname(me.getNickname());
        r.setFromPhotoUrl(me.getPhotoUrl());
        r.setToUid(to.getUid());
        r.setToFriendCode(to.getFriendCode());
        r.setToNickname(to.getNickname());
        r.setToPhotoUrl(to.getPhotoUrl());
        r.setCreatedAt(new Timestamp(now));
        r.setExpiresAt(new Timestamp(new Date(now.getTime() + REQUEST_EXPIRE_MILLIS)));
        return ref.set(r).continueWith(task -> {
            task.getResult();
            return r;
        });
    }

    // 친구 코드로 찾기 (정확히 일치만): friendCodes/{code} → uid → users/{uid}, 그다음 나와의 관계까지
    public Task<SearchResult> search(String code, String myUid) {
        return db.collection(DbPaths.FRIEND_CODES).document(code).get()
                .continueWithTask(task -> {
                    FriendCode fc = task.getResult().toObject(FriendCode.class);
                    if (fc == null || fc.getUid() == null) return Tasks.forResult((User) null);
                    return db.collection(DbPaths.USERS).document(fc.getUid()).get()
                            .continueWith(t -> t.getResult().toObject(User.class));
                })
                .continueWithTask(task -> {
                    SearchResult result = new SearchResult();
                    result.user = task.getResult();
                    if (result.user == null) {
                        result.state = SearchResult.NOT_FOUND;
                        return Tasks.forResult(result);
                    }
                    String uid = result.user.getUid();
                    if (uid.equals(myUid)) {
                        result.state = SearchResult.ME;
                        return Tasks.forResult(result);
                    }
                    // 친구 문서 / 내가 보낸 요청 / 받은 요청 셋 다 확인
                    Task<DocumentSnapshot> friendDoc = db.collection(DbPaths.USERS).document(myUid)
                            .collection(DbPaths.FRIENDS).document(uid).get();
                    Task<FriendRequest> sent = findRequest(myUid, uid);
                    Task<FriendRequest> received = findRequest(uid, myUid);
                    return Tasks.whenAllSuccess(friendDoc, sent, received).continueWith(t -> {
                        if (friendDoc.getResult().exists()) {
                            result.state = SearchResult.FRIEND;
                        } else if (sent.getResult() != null) {
                            result.state = SearchResult.SENT;
                            result.request = sent.getResult();
                        } else if (received.getResult() != null) {
                            result.state = SearchResult.RECEIVED;
                            result.request = received.getResult();
                        } else {
                            result.state = SearchResult.NONE;
                        }
                        return result;
                    });
                });
    }

    // from → to 로 보낸 안 지난 요청 하나 (없으면 null)
    private Task<FriendRequest> findRequest(String fromUid, String toUid) {
        return db.collection(DbPaths.FRIEND_REQUESTS)
                .whereEqualTo("fromUid", fromUid)
                .whereEqualTo("toUid", toUid)
                .get()
                .continueWith(task -> {
                    Timestamp now = Timestamp.now();
                    for (FriendRequest r : task.getResult().toObjects(FriendRequest.class)) {
                        if (r.getExpiresAt() != null && r.getExpiresAt().compareTo(now) > 0) return r;
                    }
                    return null;
                });
    }
}
