package com.goner.app.friend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.R;
import com.goner.app.common.ConfirmDialog;
import com.goner.app.common.InviteLink;
import com.goner.app.data.model.Friend;
import com.goner.app.data.model.FriendRequest;
import com.goner.app.data.repo.FriendRepo;
import com.goner.app.data.repo.UserRepo;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

// 친구 탭: 홈(내 코드 + 친구 목록) / 받은 요청 / 보낸 요청
// 들어오는 곳: 홈 "친구 먼저 추가하기", 마이페이지 "친구 관리" / "받은 요청"(EXTRA_TAB = TAB_RECEIVED)
public class FriendActivity extends AppCompatActivity {

    // 처음 열 탭
    public static final String EXTRA_TAB = "tab";
    public static final int TAB_HOME = 0;
    public static final int TAB_RECEIVED = 1;
    public static final int TAB_SENT = 2;

    private static final String REQUEST_DELETE_FRIEND = "delete_friend";
    private static final String REQUEST_CANCEL = "cancel_request";
    private static final String KEY_TAB = "tab";
    private static final String KEY_PENDING_ID = "pending_id";

    private final FriendRepo friendRepo = new FriendRepo();
    private final UserRepo userRepo = new UserRepo();
    private String uid;
    private String myCode;
    private int tab = TAB_HOME;
    private String pendingId;   // 확인창 띄운 친구 uid / 요청 id

    private TextView textTitle, textMyCode, textFriendCount, textRequestEmpty, textRequestNotice;
    private View buttonAdd, layoutHome, layoutRequests, layoutFriendList, layoutFriendEmpty;
    private View[] tabLines;
    private FriendAdapter friendAdapter;
    private FriendRequestAdapter requestAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);
        uid = FirebaseAuth.getInstance().getUid();
        if (savedInstanceState != null) {
            tab = savedInstanceState.getInt(KEY_TAB, TAB_HOME);
            pendingId = savedInstanceState.getString(KEY_PENDING_ID);
        } else {
            tab = getIntent().getIntExtra(EXTRA_TAB, TAB_HOME);
        }

        textTitle = findViewById(R.id.text_title);
        textMyCode = findViewById(R.id.text_my_code);
        textFriendCount = findViewById(R.id.text_friend_count);
        textRequestEmpty = findViewById(R.id.text_request_empty);
        textRequestNotice = findViewById(R.id.text_request_notice);
        buttonAdd = findViewById(R.id.button_add);
        layoutHome = findViewById(R.id.layout_home);
        layoutRequests = findViewById(R.id.layout_requests);
        layoutFriendList = findViewById(R.id.layout_friend_list);
        layoutFriendEmpty = findViewById(R.id.layout_friend_empty);
        tabLines = new View[]{findViewById(R.id.line_home), findViewById(R.id.line_received), findViewById(R.id.line_sent)};

        friendAdapter = new FriendAdapter(this::confirmDeleteFriend);
        ((RecyclerView) findViewById(R.id.recycler_friends)).setAdapter(friendAdapter);
        requestAdapter = new FriendRequestAdapter(new FriendRequestAdapter.Listener() {
            @Override
            public void onCancel(FriendRequest request) {
                confirmCancel(request);
            }

            @Override
            public void onAccept(FriendRequest request) {
                accept(request);
            }

            @Override
            public void onReject(FriendRequest request) {
                friendRepo.deleteRequest(request)
                        .addOnSuccessListener(v -> removeRequest(request))
                        .addOnFailureListener(e -> showError(e));
            }
        });
        ((RecyclerView) findViewById(R.id.recycler_requests)).setAdapter(requestAdapter);

        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        buttonAdd.setOnClickListener(v -> startActivity(new Intent(this, AddFriendActivity.class)));
        findViewById(R.id.tab_home).setOnClickListener(v -> showTab(TAB_HOME));
        findViewById(R.id.tab_received).setOnClickListener(v -> showTab(TAB_RECEIVED));
        findViewById(R.id.tab_sent).setOnClickListener(v -> showTab(TAB_SENT));
        findViewById(R.id.button_copy).setOnClickListener(v -> {
            if (myCode != null) InviteLink.copyCode(this, myCode);
        });
        findViewById(R.id.button_share).setOnClickListener(v -> shareMyCode());
        findViewById(R.id.button_invite).setOnClickListener(v -> shareMyCode());

        getSupportFragmentManager().setFragmentResultListener(REQUEST_DELETE_FRIEND, this, (k, r) -> deleteFriend());
        getSupportFragmentManager().setFragmentResultListener(REQUEST_CANCEL, this, (k, r) -> cancelRequest());

        loadMyCode();
        showTab(tab);
    }

    // 친구 추가 화면 갔다 오면 목록이 바뀌었을 수 있어서 다시 읽음
    @Override
    protected void onRestart() {
        super.onRestart();
        showTab(tab);
    }

    private void loadMyCode() {
        if (uid == null) return;
        userRepo.getUser(uid).addOnSuccessListener(user -> {
            if (user == null) return;
            myCode = user.getFriendCode();
            textMyCode.setText(myCode);
        });
    }

    private void shareMyCode() {
        if (myCode == null) {
            Toast.makeText(this, R.string.friend_need_login, Toast.LENGTH_SHORT).show();
            return;
        }
        InviteLink.shareFriend(this, myCode);
    }

    // 탭 바꾸기. 홈은 "친구"(28) + "+", 요청 탭은 "친구 요청"(22)
    private void showTab(int newTab) {
        tab = newTab;
        for (int i = 0; i < tabLines.length; i++) {
            tabLines[i].setVisibility(i == tab ? View.VISIBLE : View.INVISIBLE);
        }
        boolean home = tab == TAB_HOME;
        textTitle.setText(home ? R.string.friend_title : R.string.friend_requests_title);
        textTitle.setTextAppearance(home ? R.style.Text_Title28 : R.style.Text_TopBarTitle);
        buttonAdd.setVisibility(home ? View.VISIBLE : View.INVISIBLE);
        layoutHome.setVisibility(home ? View.VISIBLE : View.GONE);
        layoutRequests.setVisibility(home ? View.GONE : View.VISIBLE);
        textRequestNotice.setVisibility(tab == TAB_SENT ? View.VISIBLE : View.GONE);

        if (home) loadFriends();
        else loadRequests();
    }

    private void loadFriends() {
        friendRepo.listFriends()
                .addOnSuccessListener(this::showFriends)
                .addOnFailureListener(e -> showFriends(List.of()));
    }

    private void showFriends(List<Friend> list) {
        friendAdapter.setItems(list);
        textFriendCount.setText(getString(R.string.friend_count, list.size()));
        layoutFriendList.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        layoutFriendEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadRequests() {
        if (uid == null) {
            showRequests(List.of());
            return;
        }
        boolean sent = tab == TAB_SENT;
        (sent ? friendRepo.listSent(uid) : friendRepo.listReceived(uid))
                .addOnSuccessListener(list -> {
                    if (tab == (sent ? TAB_SENT : TAB_RECEIVED)) showRequests(list); // 그새 탭 바뀌었으면 무시
                })
                .addOnFailureListener(e -> showRequests(List.of()));
    }

    private void showRequests(List<FriendRequest> list) {
        requestAdapter.setItems(list, tab == TAB_SENT);
        textRequestEmpty.setText(tab == TAB_SENT ? R.string.request_empty_sent : R.string.request_empty_received);
        textRequestEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void removeRequest(FriendRequest request) {
        requestAdapter.remove(request);
        textRequestEmpty.setVisibility(requestAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    // 받은 요청 수락. 보안 규칙에 막히면 에러 문구 그대로 보여줌
    private void accept(FriendRequest request) {
        friendRepo.accept(request)
                .addOnSuccessListener(v -> {
                    removeRequest(request);
                    Toast.makeText(this, R.string.noti_accepted, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(this::showError);
    }

    private void confirmDeleteFriend(Friend friend) {
        pendingId = friend.getFriendUid();
        ConfirmDialog.show(getSupportFragmentManager(), REQUEST_DELETE_FRIEND,
                getString(R.string.friend_delete_title),
                getString(R.string.friend_delete_message, friend.getNickname()),
                getString(R.string.delete));
    }

    private void deleteFriend() {
        if (pendingId == null || uid == null) return;
        friendRepo.removeFriend(uid, pendingId)
                .addOnSuccessListener(v -> loadFriends())
                .addOnFailureListener(this::showError);
        pendingId = null;
    }

    private void confirmCancel(FriendRequest request) {
        pendingId = request.getId();
        ConfirmDialog.show(getSupportFragmentManager(), REQUEST_CANCEL,
                getString(R.string.request_cancel_title),
                getString(R.string.request_cancel_message, request.getToNickname()),
                getString(R.string.request_cancel),
                getString(R.string.request_keep));
    }

    private void cancelRequest() {
        FriendRequest request = pendingId == null ? null : requestAdapter.find(pendingId);
        pendingId = null;
        if (request == null) return;
        friendRepo.deleteRequest(request)
                .addOnSuccessListener(v -> removeRequest(request))
                .addOnFailureListener(this::showError);
    }

    private void showError(Exception e) {
        Toast.makeText(this, e.getMessage() == null ? getString(R.string.noti_action_failed) : e.getMessage(),
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB, tab);
        outState.putString(KEY_PENDING_ID, pendingId);
    }
}
