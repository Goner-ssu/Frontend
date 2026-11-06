package com.goner.app.data.repo;

import com.goner.app.data.DbPaths;
import com.goner.app.data.model.Friend;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

// 친구 관련 Firestore 접근
public class FriendRepo {

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
}
