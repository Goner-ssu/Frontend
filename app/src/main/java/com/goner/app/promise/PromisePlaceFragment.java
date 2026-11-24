package com.goner.app.promise;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.BuildConfig;
import com.goner.app.R;
import com.goner.app.data.kakao.KakaoClient;
import com.goner.app.data.kakao.PlaceResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 약속 만들기 3단계: 장소 검색해서 고르기 (지도 칸은 비워 둠)
public class PromisePlaceFragment extends Fragment {

    private AppointmentDraft draft;
    private EditText editSearch;
    private TextView textPlace;
    private RecyclerView recyclerResults;
    private PlaceResultAdapter adapter;
    private Button buttonNext;

    public PromisePlaceFragment() {
        super(R.layout.fragment_create_place);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        draft = new ViewModelProvider(requireActivity()).get(CreatePromiseViewModel.class).getDraft();

        editSearch = view.findViewById(R.id.edit_search);
        textPlace = view.findViewById(R.id.text_place);
        recyclerResults = view.findViewById(R.id.recycler_results);
        buttonNext = view.findViewById(R.id.button_next);

        // 카드 모서리 둥글게 잘리게 (검색 결과 목록이 카드 안에 뜸)
        view.findViewById(R.id.map_card).setClipToOutline(true);

        adapter = new PlaceResultAdapter(this::selectPlace);
        recyclerResults.setAdapter(adapter);

        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            search();
            return true;
        });

        buttonNext.setOnClickListener(v -> {
            ((CreatePromiseActivity) requireActivity()).showNext(new PromiseFriendsFragment());
        });

        if (hasPlace()) showPlace(); // 뒤로 왔을 때 고른 장소 다시 보여줌
        updateButton();

        // 테스트용: debug 빌드에서만 버튼 보이고, 누르면 강남역으로 채움 (카카오 키 생기면 지워도 됨)
        if (BuildConfig.DEBUG) {
            View testButton = view.findViewById(R.id.button_test_place);
            testButton.setVisibility(View.VISIBLE);
            testButton.setOnClickListener(v -> {
                PlaceResponse.Place test = new PlaceResponse.Place();
                test.place_name = "강남역 2호선";
                test.road_address_name = "서울 강남구 강남대로 396";
                test.x = "127.027619";
                test.y = "37.497952";
                selectPlace(test);
            });
        }
    }

    // 검색창에서 엔터 → 카카오 키워드 검색
    private void search() {
        String query = editSearch.getText().toString().trim();
        if (query.isEmpty()) return;
        hideKeyboard();

        KakaoClient.get().searchKeyword(query).enqueue(new Callback<PlaceResponse>() {
            @Override
            public void onResponse(@NonNull Call<PlaceResponse> call, @NonNull Response<PlaceResponse> response) {
                if (!isAdded()) return;
                PlaceResponse body = response.body();
                if (body == null || body.documents == null || body.documents.isEmpty()) {
                    recyclerResults.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), R.string.create_place_no_result, Toast.LENGTH_SHORT).show();
                    return;
                }
                adapter.setItems(body.documents);
                recyclerResults.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(@NonNull Call<PlaceResponse> call, @NonNull Throwable t) {
                if (isAdded()) Toast.makeText(requireContext(), R.string.create_place_no_result, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 검색 결과에서 고르면 그 장소 이름·주소·좌표로 채움
    private void selectPlace(PlaceResponse.Place place) {
        recyclerResults.setVisibility(View.GONE);
        draft.placeName = place.place_name;
        draft.placeAddress = place.getAddress();
        draft.placeLat = Double.parseDouble(place.y);
        draft.placeLng = Double.parseDouble(place.x);
        showPlace();
        updateButton();
    }

    private void showPlace() {
        textPlace.setText(getString(R.string.create_place_info, draft.placeName, draft.placeAddress));
    }

    private boolean hasPlace() {
        return draft.placeName != null && draft.placeAddress != null
                && (draft.placeLat != 0 || draft.placeLng != 0);
    }

    // 이름·주소·좌표 다 있을 때만 다음 가능
    private void updateButton() {
        buttonNext.setEnabled(hasPlace());
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
        editSearch.clearFocus();
    }
}
