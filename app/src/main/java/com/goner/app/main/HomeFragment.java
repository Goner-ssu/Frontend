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
import com.goner.app.data.model.FriendRequest;
import com.goner.app.data.repo.AppointmentRepo;
import com.goner.app.data.repo.FriendRepo;
import com.goner.app.data.repo.NotificationRepo;
import com.goner.app.friend.AddFriendActivity;
import com.goner.app.friend.FriendActivity;
import com.goner.app.notification.NotificationActivity;
import com.goner.app.notification.NotificationSeen;
import com.goner.app.promise.CreatePromiseActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String REQUEST_DELETE = "delete_promise";
    private static final String KEY_DELETE_ID = "delete_id";

    private final AppointmentRepo appointmentRepo = new AppointmentRepo();
    private final FriendRepo friendRepo = new FriendRepo();
    private final NotificationRepo notificationRepo = new NotificationRepo();
    private String uid;
    private PromiseAdapter adapter;
    private RecyclerView recycler;
    private View emptyLayout;
    private View progress;
    private View unreadDot;
    private ListenerRegistration promiseListener;
    private String deleteId; // 삭제 확인창 띄운 약속
    private List<Appointment> promises = new ArrayList<>(); // 지금 보이는 약속

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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

    // 다른 화면 갔다 오면: 참여자만 바뀐 경우 위 리스너가 안 불려서 참여자 다시 읽음 + 알림 화면 봤으면 벨 점 끄기
    @Override
    public void onResume() {
        super.onResume();
        loadParticipants(promises);
        checkUnread();
    }

    // 하단 탭으로 홈이 다시 보일 때
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) checkUnread();
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

    // 안 읽은 알림 있으면 벨에 점. 알림 화면과 같은 기준 (받은 친구 요청 + 약속 초대 중 아직 안 본 것)
    private void checkUnread() {
        if (uid == null) return;
        Tasks.whenAllSuccess(friendRepo.listReceived(uid), notificationRepo.listInvites(uid))
                .addOnSuccessListener(results -> {
                    if (!isAdded()) return;
                    @SuppressWarnings("unchecked")
                    List<FriendRequest> requests = (List<FriendRequest>) results.get(0);
                    @SuppressWarnings("unchecked")
                    List<Appointment> invites = (List<Appointment>) results.get(1);
                    boolean unread = NotificationSeen.hasUnread(requireContext(), requests, invites);
                    unreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);
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
            Intent intent = new Intent(requireContext(), CreatePromiseActivity.class);
            intent.putExtra(CreatePromiseActivity.EXTRA_APPOINTMENT_ID, appointment.getId());
            startActivity(intent);
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
        adapter = null;
    }
}
