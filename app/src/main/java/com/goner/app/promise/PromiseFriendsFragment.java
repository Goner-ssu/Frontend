package com.goner.app.promise;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.R;
import com.goner.app.data.model.Friend;
import com.goner.app.data.repo.FriendRepo;
import com.goner.app.friend.AddFriendActivity;

import java.util.ArrayList;
import java.util.List;

// 약속 만들기 4단계: 초대할 친구 고르기
public class PromiseFriendsFragment extends Fragment {

    private final FriendRepo friendRepo = new FriendRepo();
    private final List<Friend> allFriends = new ArrayList<>();

    private AppointmentDraft draft;
    private FriendCheckAdapter friendAdapter;
    private SelectedFriendAdapter selectedAdapter;
    private EditText editSearch;
    private View layoutEmpty;
    private Button buttonNext;

    public PromiseFriendsFragment() {
        super(R.layout.fragment_create_friends);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        draft = new ViewModelProvider(requireActivity()).get(CreatePromiseViewModel.class).getDraft();

        editSearch = view.findViewById(R.id.edit_search);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        buttonNext = view.findViewById(R.id.button_next);

        friendAdapter = new FriendCheckAdapter(draft.selectedFriends, this::toggle);
        selectedAdapter = new SelectedFriendAdapter(draft.selectedFriends, this::toggle);
        ((RecyclerView) view.findViewById(R.id.recycler_friends)).setAdapter(friendAdapter);
        ((RecyclerView) view.findViewById(R.id.recycler_selected)).setAdapter(selectedAdapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                showFiltered();
            }
        });

        view.findViewById(R.id.button_add_friend).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddFriendActivity.class)));

        buttonNext.setOnClickListener(v -> {
            // TODO 5단계(벌칙) Fragment 만들면 ((CreatePromiseActivity) requireActivity()).showNext(...)
        });

        updateButton();
    }

    // 친구 추가하고 돌아와도 목록 새로 보이게 onResume에서 불러옴
    @Override
    public void onResume() {
        super.onResume();
        friendRepo.listFriends()
                .addOnSuccessListener(list -> {
                    if (!isAdded()) return;
                    allFriends.clear();
                    allFriends.addAll(list);
                    showFiltered();
                    layoutEmpty.setVisibility(allFriends.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) layoutEmpty.setVisibility(View.VISIBLE);
                });
    }

    // 검색어가 이름이나 친구 코드에 들어간 친구만 보여줌
    private void showFiltered() {
        String query = editSearch.getText().toString().trim().toLowerCase();
        List<Friend> filtered = new ArrayList<>();
        for (Friend f : allFriends) {
            String name = f.getNickname() == null ? "" : f.getNickname().toLowerCase();
            String code = f.getFriendCode() == null ? "" : f.getFriendCode().toLowerCase();
            if (query.isEmpty() || name.contains(query) || code.contains(query)) filtered.add(f);
        }
        friendAdapter.setItems(filtered);
    }

    // 목록에서 누르거나 칩의 ⓧ 누르면 선택/해제
    private void toggle(Friend friend) {
        Friend found = null;
        for (Friend f : draft.selectedFriends) {
            if (f.getFriendUid().equals(friend.getFriendUid())) found = f;
        }
        if (found != null) draft.selectedFriends.remove(found);
        else draft.selectedFriends.add(friend);

        friendAdapter.notifyDataSetChanged();
        selectedAdapter.notifyDataSetChanged();
        updateButton();
    }

    // 한 명 이상 골라야 다음 가능
    private void updateButton() {
        buttonNext.setEnabled(!draft.selectedFriends.isEmpty());
    }
}
