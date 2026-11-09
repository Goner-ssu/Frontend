package com.goner.app.promise;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.goner.app.R;
import com.goner.app.common.AvatarView;
import com.goner.app.data.model.Friend;
import com.goner.app.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// 약속 생성 완료 화면 (Figma 372:203)
public class PromiseCreatedFragment extends Fragment {

    private static final String ARG_ID = "appointmentId";

    public PromiseCreatedFragment() {
        super(R.layout.fragment_promise_created);
    }

    public static PromiseCreatedFragment newInstance(String appointmentId) {
        PromiseCreatedFragment f = new PromiseCreatedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, appointmentId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // 방금 저장한 내용이라 draft 그대로 씀
        AppointmentDraft draft = new ViewModelProvider(requireActivity()).get(CreatePromiseViewModel.class).getDraft();

        List<String> names = new ArrayList<>();
        for (Friend f : draft.selectedFriends) names.add(f.getNickname());
        ((TextView) view.findViewById(R.id.text_desc))
                .setText(getString(R.string.created_desc, String.join(", ", names)));

        SimpleDateFormat timeFormat = new SimpleDateFormat("M월 d일 (E) a h:mm", Locale.KOREA);
        ((TextView) view.findViewById(R.id.text_title)).setText(draft.title);
        ((TextView) view.findViewById(R.id.text_time)).setText(timeFormat.format(draft.appointmentTime.toDate()));
        ((TextView) view.findViewById(R.id.text_place)).setText(draft.placeName);

        // 참여자: 나(방장) + 초대한 친구들(대기)
        LinearLayout members = view.findViewById(R.id.layout_members);
        String myUid = FirebaseAuth.getInstance().getUid();
        addMember(members, myUid == null ? "" : myUid, getString(R.string.created_me), null, true);
        for (Friend f : draft.selectedFriends) {
            addMember(members, f.getFriendUid(), f.getNickname(), f.getPhotoUrl(), false);
        }

        view.findViewById(R.id.button_share).setOnClickListener(v ->
                // TODO 21번 초대 링크 만들면 InviteLink.appointment(id) 공유로 바꿔야 함
                Toast.makeText(requireContext(), R.string.created_share_soon, Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.button_home).setOnClickListener(v -> goHome());

        // 여기서 뒤로가기 하면 요약으로 안 돌아가고 홈으로 감
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        goHome();
                    }
                });
    }

    private void addMember(LinearLayout parent, String uid, String name, @Nullable String photoUrl, boolean host) {
        View item = LayoutInflater.from(requireContext()).inflate(R.layout.item_created_member, parent, false);
        ((AvatarView) item.findViewById(R.id.avatar)).setUser(uid, name, photoUrl);
        ((TextView) item.findViewById(R.id.text_name)).setText(name);

        TextView chip = item.findViewById(R.id.text_chip);
        chip.setText(host ? R.string.created_host : R.string.created_waiting);
        chip.setBackgroundResource(host ? R.drawable.bg_chip_host : R.drawable.bg_chip_waiting);
        chip.setTextColor(requireContext().getColor(host ? R.color.white : R.color.text_sub));

        if (parent.getChildCount() > 0) {
            ((LinearLayout.LayoutParams) item.getLayoutParams())
                    .setMarginStart(getResources().getDimensionPixelSize(R.dimen.created_member_gap));
        }
        parent.addView(item);
    }

    private void goHome() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        requireActivity().finish();
    }
}
