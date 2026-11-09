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

import com.goner.app.BuildConfig;
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

    private CreatePromiseViewModel viewModel;
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
        viewModel = new ViewModelProvider(requireActivity()).get(CreatePromiseViewModel.class);
        draft = viewModel.getDraft();

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

        // 테스트용: debug 빌드에서만 버튼 보이고, 누르면 가짜 친구 4명으로 목록 채움 (친구 기능 생기면 지워도 됨)
        if (BuildConfig.DEBUG) {
            View testButton = view.findViewById(R.id.button_test_friends);
            testButton.setVisibility(View.VISIBLE);
            testButton.setOnClickListener(v -> {
                viewModel.useTestFriends = true;
                showTestFriends();
            });
        }

        buttonNext.setOnClickListener(v -> {
            ((CreatePromiseActivity) requireActivity()).showNext(new PromisePenaltyFragment());
        });

        updateButton();
    }

    // 친구 추가하고 돌아와도 목록 새로 보이게 onResume에서 불러옴
    @Override
    public void onResume() {
        super.onResume();
        if (viewModel.useTestFriends) { // 테스트 친구 쓰는 중이면 다시 불러오지 않음
            showTestFriends();
            return;
        }
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

    // 테스트용 가짜 친구 (DB에 저장 안 함)
    private void showTestFriends() {
        String[][] data = {{"test1", "이가희", "gahee"}, {"test2", "이현준", "hyunjun"},
                {"test3", "김종민", "jongmin"}, {"test4", "어나경", "nakyung"}};
        allFriends.clear();
        for (String[] d : data) {
            Friend f = new Friend();
            f.setFriendUid(d[0]);
            f.setNickname(d[1]);
            f.setFriendCode(d[2]);
            allFriends.add(f);
        }
        showFiltered();
        layoutEmpty.setVisibility(View.GONE);
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
