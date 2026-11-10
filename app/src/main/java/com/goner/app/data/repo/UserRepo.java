package com.goner.app.data.repo;

import com.goner.app.data.DbPaths;
import com.goner.app.data.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

// 사용자 관련 Firestore 접근
public class UserRepo {

    // 실시간으로 받을 때 (문서 없으면 null)
    public interface UserListener {
        void onChanged(User user);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // users/{uid} 한 명 (문서 없으면 null)
    public Task<User> getUser(String uid) {
        return db.collection(DbPaths.USERS).document(uid).get()
                .continueWith(task -> task.getResult().toObject(User.class));
    }

    // users/{uid} 바뀔 때마다 받음 (마이페이지). 화면 끝날 때 remove() 해야 함
    public ListenerRegistration listenUser(String uid, UserListener listener) {
        return db.collection(DbPaths.USERS).document(uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    listener.onChanged(value.toObject(User.class));
                });
    }

    // 알람 ON/OFF (users.notiEnabled)
    public Task<Void> setNotiEnabled(String uid, boolean enabled) {
        return db.collection(DbPaths.USERS).document(uid).update("notiEnabled", enabled);
    }
}
