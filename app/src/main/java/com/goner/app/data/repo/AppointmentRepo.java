package com.goner.app.data.repo;

import com.goner.app.data.DbPaths;
import com.goner.app.data.DbValues;
import com.goner.app.data.model.Appointment;
import com.goner.app.data.model.Participant;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 약속 관련 Firestore / RTDB 접근
public class AppointmentRepo {

    // 실시간 목록 결과 받는 곳
    public interface ListListener {
        void onChanged(List<Appointment> list);

        void onError(Exception e);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // 내가 들어가 있는 진행 중 약속(pending/confirmed/ongoing) 실시간으로 받음. 화면 끝날 때 remove() 해야 함
    public ListenerRegistration listMyActive(String uid, ListListener listener) {
        return db.collection(DbPaths.APPOINTMENTS)
                .whereArrayContains("participantUids", uid)
                .whereIn("status", Arrays.asList(DbValues.APPOINTMENT_PENDING,
                        DbValues.APPOINTMENT_CONFIRMED, DbValues.APPOINTMENT_ONGOING))
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        listener.onError(error);
                        return;
                    }
                    List<Appointment> list = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Appointment a = doc.toObject(Appointment.class);
                        if (a != null) list.add(a);
                    }
                    listener.onChanged(list);
                });
    }

    // 약속 참여자 목록 (홈 카드 아바타, 수락 대기 수)
    public Task<List<Participant>> listParticipants(String appointmentId) {
        return db.collection(DbPaths.APPOINTMENTS).document(appointmentId)
                .collection(DbPaths.PARTICIPANTS).get()
                .continueWith(task -> task.getResult().toObjects(Participant.class));
    }

    // 약속 하나 (초대 상세)
    public Task<Appointment> get(String appointmentId) {
        return db.collection(DbPaths.APPOINTMENTS).document(appointmentId).get()
                .continueWith(task -> task.getResult().toObject(Appointment.class));
    }

    // 초대 수락: 내 참여자 문서 joined 로 바꾸고, 다른 사람도 다 joined 면 약속을 confirmed 로 (트랜잭션)
    // 그다음 RTDB members/{약속}/{나} = true
    public Task<Void> join(String appointmentId, String uid) {
        DocumentReference ref = db.collection(DbPaths.APPOINTMENTS).document(appointmentId);
        return db.runTransaction(tx -> {
                    Appointment a = tx.get(ref).toObject(Appointment.class);
                    if (a == null) {
                        throw new FirebaseFirestoreException("약속 없음", FirebaseFirestoreException.Code.NOT_FOUND);
                    }
                    // 트랜잭션은 읽기를 쓰기보다 먼저 다 해야 함
                    boolean allJoined = true;
                    for (String other : a.getParticipantUids()) {
                        if (other.equals(uid)) continue;
                        DocumentSnapshot p = tx.get(ref.collection(DbPaths.PARTICIPANTS).document(other));
                        if (!DbValues.PARTICIPANT_JOINED.equals(p.getString("status"))) allJoined = false;
                    }

                    Map<String, Object> me = new HashMap<>();
                    me.put("status", DbValues.PARTICIPANT_JOINED);
                    me.put("joinedAt", FieldValue.serverTimestamp());
                    tx.update(ref.collection(DbPaths.PARTICIPANTS).document(uid), me);
                    if (allJoined) tx.update(ref, "status", DbValues.APPOINTMENT_CONFIRMED);
                    return null;
                })
                .continueWithTask(task -> {
                    task.getResult(); // 실패했으면 여기서 예외
                    return FirebaseDatabase.getInstance().getReference(DbPaths.RTDB_MEMBERS)
                            .child(appointmentId).child(uid).setValue(true);
                });
    }

    // 초대 거절: 내 참여자 문서 rejected + 약속 participantUids 에서 나 빼기
    public Task<Void> reject(String appointmentId, String uid) {
        DocumentReference ref = db.collection(DbPaths.APPOINTMENTS).document(appointmentId);
        WriteBatch batch = db.batch();
        batch.update(ref.collection(DbPaths.PARTICIPANTS).document(uid), "status", DbValues.PARTICIPANT_REJECTED);
        batch.update(ref, "participantUids", FieldValue.arrayRemove(uid));
        return batch.commit();
    }

    // 약속 삭제 = status 를 canceled 로 바꿈 (문서는 남김)
    public Task<Void> cancel(String appointmentId) {
        return db.collection(DbPaths.APPOINTMENTS).document(appointmentId)
                .update("status", DbValues.APPOINTMENT_CANCELED);
    }

    // 약속 + 참여자 문서를 한 번에(WriteBatch) 저장하고, 방장은 RTDB members 에도 넣음. 새 약속 id를 돌려줌
    // 알림은 서버(Cloud Functions)가 appointments 생성 보고 보냄
    public Task<String> create(Appointment appointment, List<Participant> participants) {
        DocumentReference ref = db.collection(DbPaths.APPOINTMENTS).document();
        String id = ref.getId();
        appointment.setId(id);

        WriteBatch batch = db.batch();
        batch.set(ref, appointment);
        for (Participant p : participants) {
            p.setAppointmentId(id);
            batch.set(ref.collection(DbPaths.PARTICIPANTS).document(p.getUid()), p);
        }

        return batch.commit()
                .continueWithTask(task -> {
                    task.getResult(); // 실패했으면 여기서 예외 → 아래로 안 감
                    // 방장은 만들 때 바로 참여 확정이라 members 에 넣음
                    return FirebaseDatabase.getInstance().getReference(DbPaths.RTDB_MEMBERS)
                            .child(id).child(appointment.getHostUid()).setValue(true);
                })
                .continueWith(task -> {
                    task.getResult();
                    return id;
                });
    }

    // 약속 수정 (홈에서 수정하기로 들어온 경우). originalTime 은 처음 값 그대로 둠
    public Task<Void> update(Appointment appointment) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("title", appointment.getTitle());
        fields.put("placeName", appointment.getPlaceName());
        fields.put("placeAddress", appointment.getPlaceAddress());
        fields.put("placeLat", appointment.getPlaceLat());
        fields.put("placeLng", appointment.getPlaceLng());
        fields.put("appointmentTime", appointment.getAppointmentTime());
        fields.put("endTime", appointment.getEndTime());
        fields.put("penalty_type", appointment.getPenaltyType());
        fields.put("penalty_text", appointment.getPenaltyText());
        return db.collection(DbPaths.APPOINTMENTS).document(appointment.getId()).update(fields);
    }
}
