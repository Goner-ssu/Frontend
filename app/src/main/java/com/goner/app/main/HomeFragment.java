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
import com.goner.app.common.Promise;
import com.goner.app.common.User;
import com.goner.app.friend.AddFriendActivity;
import com.goner.app.promise.CreatePromiseActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    private static final String REQUEST_DELETE = "delete_promise";
    private static final String KEY_DELETE_ID = "delete_id";

    private FirebaseFirestore db;
    private String uid;
    private PromiseAdapter adapter;
    private View emptyLayout;
    private View unreadDot;
    private ListenerRegistration promiseListener;
    private ListenerRegistration notificationListener;
    private final Set<String> requestedUsers = new HashSet<>(); // 이미 불러온 사용자
    private String deleteId; // 삭제 확인창 띄운 약속

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        if (savedInstanceState != null) deleteId = savedInstanceState.getString(KEY_DELETE_ID);

        emptyLayout = view.findViewById(R.id.layout_empty);
        unreadDot = view.findViewById(R.id.unread_dot);
        RecyclerView recycler = view.findViewById(R.id.recycler_promises);

        adapter = new PromiseAdapter(uid, this::showMenu);
        recycler.setAdapter(adapter);

        view.findViewById(R.id.button_bell).setOnClickListener(v -> {
        });
        view.findViewById(R.id.button_add).setOnClickListener(v -> showAddMenu());
        view.findViewById(R.id.button_create).setOnClickListener(v -> openCreatePromise());
        view.findViewById(R.id.button_add_friend).setOnClickListener(v -> openAddFriend());

        getChildFragmentManager().setFragmentResultListener(REQUEST_DELETE, getViewLifecycleOwner(),
                (key, result) -> deletePromise());

        if (uid == null) return;
        listenPromises();
        listenUnread();
    }

    // 내가 초대됐거나 참여한 약속 실시간으로 받아옴
    private void listenPromises() {
        promiseListener = db.collection("promises")
                .whereIn("members." + uid, Arrays.asList(Promise.MEMBER_INVITED, Promise.MEMBER_JOINED))
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    List<Promise> list = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Promise promise = doc.toObject(Promise.class);
                        if (promise != null && !Promise.STATUS_DONE.equals(promise.status)) {
                            list.add(promise);
                        }
                    }
                    list.sort(Comparator.comparing(p -> p.time, Comparator.nullsLast(Comparator.naturalOrder())));

                    adapter.setItems(list);
                    emptyLayout.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    loadMembers(list);
                });
    }

    // 아바타용 사진 닉네임
    private void loadMembers(List<Promise> promises) {
        for (Promise promise : promises) {
            for (String memberUid : promise.members.keySet()) {
                if (requestedUsers.contains(memberUid)) continue;
                requestedUsers.add(memberUid);
                db.collection("users").document(memberUid).get().addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user != null && adapter != null) adapter.putUser(user);
                });
            }
        }
    }

    // 안 읽은 알림 있으면 벨에 점 표시
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

    private void showMenu(Promise promise) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.sheet_promise_menu, null);
        view.findViewById(R.id.menu_edit).setOnClickListener(v -> {
            sheet.dismiss();
            // TODO 약속 수정 화면 만들면 promise.id 넘겨서 연결
        });
        view.findViewById(R.id.menu_delete).setOnClickListener(v -> {
            sheet.dismiss();
            deleteId = promise.id;
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
        // 지우면 snapshot 리스너가 알아서 목록 갱신함
        db.collection("promises").document(deleteId).delete()
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
        requestedUsers.clear();
        adapter = null;
    }
}
