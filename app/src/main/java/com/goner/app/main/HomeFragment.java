package com.goner.app.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.R;
import com.goner.app.common.ConfirmDialog;
import com.goner.app.data.model.Appointment;
import com.goner.app.data.repo.AppointmentRepo;
import com.goner.app.friend.AddFriendActivity;
import com.goner.app.friend.FriendActivity;
import com.goner.app.notification.NotificationActivity;
import com.goner.app.promise.CreatePromiseActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String REQUEST_DELETE = "delete_promise";
    private static final String KEY_DELETE_ID = "delete_id";

    private final AppointmentRepo appointmentRepo = new AppointmentRepo();
    private FirebaseFirestore db;
    private String uid;
    private PromiseAdapter adapter;
    private RecyclerView recycler;
    private View emptyLayout;
    private View progress;
    private View unreadDot;
    private ListenerRegistration promiseListener;
    private ListenerRegistration notificationListener;
    private String deleteId; // 삭제 확인창 띄운 약속
    private List<Appointment> promises = new ArrayList<>(); // 지금 보이는 약속

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        if (savedInstanceState != null) deleteId = savedInstanceState.getString(KEY_DELETE_ID);

        emptyLayout = view.findViewById(R.id.layout_empty);
        progress = view.findViewById(R.id.progress_home);
        unreadDot = view.findViewById(R.id.unread_dot);
        recycler = view.findViewById(R.id.recycler_promises);

        adapter = new PromiseAdapter(uid == null ? "" : uid, this::showMenu);
        recycler.setAdapter(adapter);

        view.findViewById(R.id.button_bell).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationActivity.class)));
        view.findViewById(R.id.button_add).setOnClickListener(v -> showAddMenu());
        view.findViewById(R.id.button_create).setOnClickListener(v -> openCreatePromise());
        view.findViewById(R.id.button_add_friend).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), FriendActivity.class)));

        getChildFragmentManager().setFragmentResultListener(REQUEST_DELETE, getViewLifecycleOwner(),
                (key, result) -> deletePromise());

        if (uid == null) { // 로그인 전이면 불러올 게 없어서 바로 빈 상태
            showList(false);
            return;
        }
        listenPromises();
        listenUnread();
    }

    // 내가 들어간 진행 중 약속 실시간으로 받아옴. 생기거나 없어지면 빈 상태 ↔ 목록 자동으로 바뀜
    private void listenPromises() {
        promiseListener = appointmentRepo.listMyActive(uid, new AppointmentRepo.ListListener() {
            @Override
            public void onChanged(List<Appointment> list) {
                list.sort(Comparator.comparing(Appointment::getAppointmentTime,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                promises = list;
                adapter.setItems(list);
                showList(!list.isEmpty());
                loadParticipants(list);
            }

            @Override
            public void onError(Exception e) {
                showList(false);
                Snackbar.make(requireView(), R.string.home_load_failed, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    // 초대 참여하고 돌아오면 참여자 문서만 바뀌어서 위 리스너가 안 불림 → 참여자 다시 읽어서 칩 갱신
    @Override
    public void onResume() {
        super.onResume();
        loadParticipants(promises);
    }

    // 불러오기 끝나면 로딩 숨기고 목록 / 빈 상태 중 하나만 보임
    private void showList(boolean hasItems) {
        progress.setVisibility(View.GONE);
        recycler.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        emptyLayout.setVisibility(hasItems ? View.GONE : View.VISIBLE);
    }

    // 카드 아바타·수락 대기 수에 쓸 참여자 (participants 하위 컬렉션)
    private void loadParticipants(List<Appointment> list) {
        for (Appointment a : list) {
            appointmentRepo.listParticipants(a.getId()).addOnSuccessListener(participants -> {
                if (adapter != null) adapter.putParticipants(a.getId(), participants);
            });
        }
    }

    // 안 읽은 알림 있으면 벨에 점 표시 (TODO 알림 repo 생기면 그쪽으로 옮기기)
    private void listenUnread() {
        notificationListener = db.collection("users").document(uid).collection("notifications")
                .whereEqualTo("read", false)
                .limit(1)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    unreadDot.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    // + 버튼
    private void showAddMenu() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.sheet_add_menu, null);
        view.findViewById(R.id.menu_add_promise).setOnClickListener(v -> {
            sheet.dismiss();
            openCreatePromise();
        });
        view.findViewById(R.id.menu_add_friend).setOnClickListener(v -> {
            sheet.dismiss();
            openAddFriend();
        });
        sheet.setContentView(view);
        sheet.show();
    }

    private void openCreatePromise() {
        startActivity(new Intent(requireContext(), CreatePromiseActivity.class));
    }

    private void openAddFriend() {
        startActivity(new Intent(requireContext(), AddFriendActivity.class));
    }

    private void showMenu(Appointment appointment) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.sheet_promise_menu, null);
        view.findViewById(R.id.menu_edit).setOnClickListener(v -> {
            sheet.dismiss();
            // TODO 약속 수정 화면 만들면 appointment.getId() 넘겨서 연결
        });
        view.findViewById(R.id.menu_delete).setOnClickListener(v -> {
            sheet.dismiss();
            deleteId = appointment.getId();
            ConfirmDialog.show(getChildFragmentManager(), REQUEST_DELETE,
                    getString(R.string.delete_promise_title),
                    getString(R.string.delete_promise_message),
                    getString(R.string.delete));
        });
        sheet.setContentView(view);
        sheet.show();
    }

    private void deletePromise() {
        if (deleteId == null) return;
        // canceled 로 바꾸면 listMyActive 에서 빠져서 리스너가 알아서 목록 갱신함
        appointmentRepo.cancel(deleteId)
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(requireContext(), R.string.delete_failed, Toast.LENGTH_SHORT).show();
                });
        deleteId = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_DELETE_ID, deleteId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (promiseListener != null) promiseListener.remove();
        if (notificationListener != null) notificationListener.remove();
        adapter = null;
    }
}
