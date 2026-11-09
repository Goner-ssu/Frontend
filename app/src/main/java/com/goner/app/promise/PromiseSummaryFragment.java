package com.goner.app.promise;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.goner.app.R;
import com.goner.app.data.DbValues;
import com.goner.app.data.model.Appointment;
import com.goner.app.data.model.Friend;
import com.goner.app.data.model.Participant;
import com.goner.app.data.model.User;
import com.goner.app.data.repo.AppointmentRepo;
import com.goner.app.data.repo.UserRepo;
import com.goner.app.main.MainActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

// 약속 만들기 마지막: 입력한 내용 확인하고 저장
public class PromiseSummaryFragment extends Fragment {

    private static final long DURATION_MILLIS = TimeUnit.HOURS.toMillis(1); // endTime = 약속 시간 + 1시간

    private final AppointmentRepo appointmentRepo = new AppointmentRepo();
    private final UserRepo userRepo = new UserRepo();
    private AppointmentDraft draft;

    public PromiseSummaryFragment() {
        super(R.layout.fragment_create_summary);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        draft = new ViewModelProvider(requireActivity()).get(CreatePromiseViewModel.class).getDraft();

        SimpleDateFormat timeFormat = new SimpleDateFormat("M월 d일 (E) a h:mm", Locale.KOREA);
        String time = draft.appointmentTime == null ? "" : timeFormat.format(draft.appointmentTime.toDate());

        // 친구 이름 "민수, 지현, 준호"
        List<String> names = new ArrayList<>();
        for (Friend f : draft.selectedFriends) names.add(f.getNickname());
        int count = draft.selectedFriends.size();

        boolean noPenalty = DbValues.PENALTY_NONE.equals(draft.penalty_type);

        setRow(view.findViewById(R.id.row_title), R.string.summary_label_title, draft.title, null,
                CreatePromiseActivity.STEP_NAME);
        setRow(view.findViewById(R.id.row_time), R.string.summary_label_time, time, null,
                CreatePromiseActivity.STEP_TIME);
        setRow(view.findViewById(R.id.row_place), R.string.summary_label_place, draft.placeName, draft.placeAddress,
                CreatePromiseActivity.STEP_PLACE);
        setRow(view.findViewById(R.id.row_friends), R.string.summary_label_friends, String.join(", ", names),
                getString(R.string.summary_friends_count, count, count + 1),
                CreatePromiseActivity.STEP_FRIENDS);
        setRow(view.findViewById(R.id.row_penalty), R.string.summary_label_penalty,
                noPenalty ? getString(R.string.penalty_none) : draft.penalty_text,
                noPenalty ? null : getString(R.string.summary_penalty_target),
                CreatePromiseActivity.STEP_PENALTY);

        view.findViewById(R.id.button_create).setOnClickListener(v -> save());
    }

    // 요약 카드 한 줄 채우기 (sub 가 null 이면 보조 문구 숨김)
    private void setRow(View row, int labelRes, String value, @Nullable String sub, int step) {
        ((TextView) row.findViewById(R.id.text_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.text_value)).setText(value);
        TextView subView = row.findViewById(R.id.text_sub);
        subView.setText(sub);
        subView.setVisibility(sub == null ? View.GONE : View.VISIBLE);
        row.findViewById(R.id.button_edit).setOnClickListener(v ->
                ((CreatePromiseActivity) requireActivity()).goToStep(step));
    }

    private void save() {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) {
            Toast.makeText(requireContext(), R.string.create_need_login, Toast.LENGTH_SHORT).show();
            return;
        }
        CreatePromiseActivity activity = (CreatePromiseActivity) requireActivity();
        activity.showLoading(true);

        // 내 닉네임/사진을 약속·참여자 문서에 같이 넣어야 해서 먼저 내 정보 읽음
        userRepo.getUser(myUid)
                .continueWithTask(task -> {
                    User me = task.getResult();
                    Appointment appointment = buildAppointment(myUid, me);
                    if (draft.appointmentId != null) {
                        // 수정: 약속 정보만 바꿈 (originalTime 은 그대로)
                        appointment.setId(draft.appointmentId);
                        return appointmentRepo.update(appointment).continueWith(t -> {
                            t.getResult();
                            return draft.appointmentId;
                        });
                    }
                    return appointmentRepo.create(appointment, buildParticipants(myUid, me));
                })
                .addOnSuccessListener(id -> {
                    if (!isAdded()) return;
                    if (draft.appointmentId == null) {
                        // 새로 만든 거면 생성 완료 화면
                        activity.showCreated(id);
                        return;
                    }
                    // 수정이면 홈으로 가서 토스트
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra(MainActivity.EXTRA_PROMISE_CREATED, true);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    activity.showLoading(false);
                    Toast.makeText(requireContext(), R.string.create_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private Appointment buildAppointment(String myUid, @Nullable User me) {
        Appointment a = new Appointment();
        a.setHostUid(myUid);
        a.setHostNickname(me == null ? null : me.getNickname());
        a.setTitle(draft.title);
        a.setPlaceName(draft.placeName);
        a.setPlaceAddress(draft.placeAddress);
        a.setPlaceLat(draft.placeLat);
        a.setPlaceLng(draft.placeLng);
        a.setAppointmentTime(draft.appointmentTime);
        a.setOriginalTime(draft.appointmentTime);
        a.setEndTime(new Timestamp(new Date(draft.appointmentTime.toDate().getTime() + DURATION_MILLIS)));
        a.setPenaltyType(draft.penalty_type);
        a.setPenaltyText(draft.penalty_text);
        a.setStatus(DbValues.APPOINTMENT_PENDING);
        a.setNotStartedNotified(false);
        a.setEndedAt(null);

        List<String> uids = new ArrayList<>();
        uids.add(myUid);
        for (Friend f : draft.selectedFriends) uids.add(f.getFriendUid());
        a.setParticipantUids(uids);
        return a;
    }

    // 나는 바로 참여(joined), 친구들은 초대(invited). 친구 정보는 friends 문서에서 복사
    private List<Participant> buildParticipants(String myUid, @Nullable User me) {
        List<Participant> list = new ArrayList<>();

        Participant host = new Participant();
        host.setUid(myUid);
        host.setNickname(me == null ? null : me.getNickname());
        host.setPhotoUrl(me == null ? null : me.getPhotoUrl());
        host.setStatus(DbValues.PARTICIPANT_JOINED);
        host.setJoinedAt(Timestamp.now());
        list.add(host);

        for (Friend f : draft.selectedFriends) {
            Participant p = new Participant();
            p.setUid(f.getFriendUid());
            p.setNickname(f.getNickname());
            p.setPhotoUrl(f.getPhotoUrl());
            p.setStatus(DbValues.PARTICIPANT_INVITED);
            p.setJoinedAt(null);
            list.add(p);
        }
        return list;
    }
}
