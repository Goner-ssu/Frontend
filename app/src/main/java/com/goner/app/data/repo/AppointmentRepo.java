package com.goner.app.data.repo;

import com.goner.app.data.DbPaths;
import com.goner.app.data.model.Appointment;
import com.goner.app.data.model.Participant;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 약속 관련 Firestore / RTDB 접근
public class AppointmentRepo {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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
