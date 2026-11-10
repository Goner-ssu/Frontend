package com.goner.app.data.repo;

import com.goner.app.data.DbPaths;
import com.goner.app.data.DbValues;
import com.goner.app.data.model.Appointment;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

// 알림 화면용 읽기. ERD 에 알림 컬렉션이 없어서 appointments 를 읽어서 알림처럼 보여줌 (친구 요청은 FriendRepo.listReceived)
// TODO 미루기 요청(postponeVotes)은 필드 정해지면 추가
// TODO 나중에 notifications 컬렉션 생기면 여기서 그걸 읽게 바꾸기
public class NotificationRepo {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // 초대받고 아직 수락 안 한 약속 (participants/{나}.status == invited)
    public Task<List<Appointment>> listInvites(String uid) {
        return db.collection(DbPaths.APPOINTMENTS)
                .whereArrayContains("participantUids", uid)
                .whereEqualTo("status", DbValues.APPOINTMENT_PENDING)
                .get()
                .continueWithTask(task -> {
                    List<Appointment> all = task.getResult().toObjects(Appointment.class);
                    // 약속마다 내 참여자 문서 읽어서 status 확인
                    List<Task<DocumentSnapshot>> reads = new ArrayList<>();
                    for (Appointment a : all) {
                        reads.add(db.collection(DbPaths.APPOINTMENTS).document(a.getId())
                                .collection(DbPaths.PARTICIPANTS).document(uid).get());
                    }
                    return Tasks.whenAllSuccess(reads).continueWith(t -> {
                        List<Object> docs = t.getResult();
                        List<Appointment> invited = new ArrayList<>();
                        for (int i = 0; i < all.size(); i++) {
                            String status = ((DocumentSnapshot) docs.get(i)).getString("status");
                            if (DbValues.PARTICIPANT_INVITED.equals(status)) invited.add(all.get(i));
                        }
                        return invited;
                    });
                });
    }
}
