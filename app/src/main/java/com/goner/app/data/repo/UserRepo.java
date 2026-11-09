package com.goner.app.data.repo;

import com.goner.app.data.DbPaths;
import com.goner.app.data.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

// 사용자 관련 Firestore 접근
public class UserRepo {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // users/{uid} 한 명 (문서 없으면 null)
    public Task<User> getUser(String uid) {
        return db.collection(DbPaths.USERS).document(uid).get()
                .continueWith(task -> task.getResult().toObject(User.class));
    }
}
