package com.goner.app.friend;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.goner.app.R;
import com.goner.app.common.AvatarView;
import com.goner.app.common.InviteLink;
import com.goner.app.data.model.User;
import com.goner.app.data.repo.FriendRepo;
import com.goner.app.data.repo.UserRepo;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

// 친구 추가: 코드로 검색 → 결과(요청 / 요청됨 / 수락 / 이미 친구 / 내 코드 / 없음) + 내 코드 QR 공유
public class AddFriendActivity extends AppCompatActivity {

    private final FriendRepo friendRepo = new FriendRepo();
    private final UserRepo userRepo = new UserRepo();
    private String uid;
    private User me;                       // 요청 보낼 때 내 정보 복사용
    private FriendRepo.SearchResult result;

    private EditText editCode;
    private View layoutDefault, layoutResult, layoutNotFound, progress;
    private TextView textResultName, textResultCode, textResultHint, buttonRequest, chipResult;
    private AvatarView avatarResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);
        uid = FirebaseAuth.getInstance().getUid();

        editCode = findViewById(R.id.edit_code);
        layoutDefault = findViewById(R.id.layout_default);
        layoutResult = findViewById(R.id.layout_result);
        layoutNotFound = findViewById(R.id.layout_not_found);
        progress = findViewById(R.id.progress_search);
        avatarResult = findViewById(R.id.avatar_result);
        textResultName = findViewById(R.id.text_result_name);
        textResultCode = findViewById(R.id.text_result_code);
        textResultHint = findViewById(R.id.text_result_hint);
        buttonRequest = findViewById(R.id.button_request);
        chipResult = findViewById(R.id.chip_result);

        // 코드는 소문자만, 공백 없이
        editCode.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) ->
                source.subSequence(start, end).toString().toLowerCase().replace(" ", "")});
        editCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            search();
            return true;
        });

        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        findViewById(R.id.button_search).setOnClickListener(v -> search());
        findViewById(R.id.button_copy).setOnClickListener(v -> {
            if (me != null) InviteLink.copyCode(this, me.getFriendCode());
        });
        findViewById(R.id.button_share).setOnClickListener(v -> shareMyCode());
        findViewById(R.id.button_invite).setOnClickListener(v -> shareMyCode());
        findViewById(R.id.button_qr_scan).setOnClickListener(v ->
                startActivity(new Intent(this, QrScanActivity.class)));
        buttonRequest.setOnClickListener(v -> onRequestButton());

        loadMe();
    }

    // 내 정보 → 내 코드 카드 + QR
    private void loadMe() {
        if (uid == null) return;
        userRepo.getUser(uid).addOnSuccessListener(user -> {
            if (user == null || user.getFriendCode() == null) return;
            me = user;
            ((TextView) findViewById(R.id.text_my_code)).setText(user.getFriendCode());
            int size = getResources().getDimensionPixelSize(R.dimen.add_qr_size);
            ((ImageView) findViewById(R.id.image_qr)).setImageBitmap(makeQr(InviteLink.friendUrl(user.getFriendCode()), size));
        });
    }

    // ZXing 으로 QR 비트맵 만들기 (검은 점 / 투명 배경)
    private Bitmap makeQr(String text, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.TRANSPARENT);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }

    private void shareMyCode() {
        if (me == null) {
            Toast.makeText(this, R.string.friend_need_login, Toast.LENGTH_SHORT).show();
            return;
        }
        InviteLink.shareFriend(this, me.getFriendCode());
    }

    private void search() {
        String code = editCode.getText().toString().trim();
        hideKeyboard();
        if (code.isEmpty()) {
            showOnly(layoutDefault);
            return;
        }
        if (uid == null) {
            Toast.makeText(this, R.string.friend_need_login, Toast.LENGTH_SHORT).show();
            return;
        }
        showOnly(progress);
        friendRepo.search(code, uid)
                .addOnSuccessListener(r -> {
                    result = r;
                    showResult();
                })
                .addOnFailureListener(e -> {
                    showOnly(layoutDefault);
                    Toast.makeText(this, R.string.add_search_failed, Toast.LENGTH_SHORT).show();
                });
    }

    // 검색 결과 상태에 맞게 버튼/칩/안내 문구
    private void showResult() {
        if (result.state == FriendRepo.SearchResult.NOT_FOUND) {
            showOnly(layoutNotFound);
            return;
        }
        showOnly(layoutResult);
        User user = result.user;
        boolean isMe = result.state == FriendRepo.SearchResult.ME;
        avatarResult.setUser(user.getUid(), user.getNickname(), user.getPhotoUrl());
        textResultName.setText(isMe ? getString(R.string.add_me_name, user.getNickname()) : user.getNickname());
        textResultCode.setText(getString(R.string.create_friends_code, user.getFriendCode()));

        boolean chip = isMe || result.state == FriendRepo.SearchResult.FRIEND;
        chipResult.setVisibility(chip ? View.VISIBLE : View.GONE);
        buttonRequest.setVisibility(chip ? View.GONE : View.VISIBLE);

        switch (result.state) {
            case FriendRepo.SearchResult.ME:
                chipResult.setText(R.string.add_my_code);
                textResultHint.setText(R.string.add_my_code_hint);
                break;
            case FriendRepo.SearchResult.FRIEND:
                chipResult.setText(R.string.add_already_friend);
                textResultHint.setText(R.string.add_already_friend_hint);
                break;
            case FriendRepo.SearchResult.SENT:
                setRequestButton(R.string.add_requested, false);
                textResultHint.setText(R.string.add_cancel_hint);
                break;
            case FriendRepo.SearchResult.RECEIVED:
                setRequestButton(R.string.noti_accept, true);
                textResultHint.setText(R.string.add_exact_hint);
                break;
            default:
                setRequestButton(R.string.add_request, true);
                textResultHint.setText(R.string.add_exact_hint);
        }
    }

    // 주황 버튼(친구 요청 / 수락) 또는 회색 버튼(요청됨)
    private void setRequestButton(int text, boolean primary) {
        buttonRequest.setText(text);
        buttonRequest.setBackgroundResource(primary ? R.drawable.bg_chip_host : R.drawable.bg_pill_soft);
        buttonRequest.setTextColor(getColor(primary ? R.color.white : R.color.text_sub));
    }

    // 친구 요청 → 요청됨, 요청됨 다시 누르면 취소, 수락 → 이미 친구
    private void onRequestButton() {
        if (result == null) return;
        buttonRequest.setEnabled(false);
        switch (result.state) {
            case FriendRepo.SearchResult.NONE:
                if (me == null) {
                    buttonRequest.setEnabled(true);
                    Toast.makeText(this, R.string.friend_need_login, Toast.LENGTH_SHORT).show();
                    return;
                }
                friendRepo.sendRequest(me, result.user)
                        .addOnSuccessListener(request -> {
                            result.state = FriendRepo.SearchResult.SENT;
                            result.request = request;
                            showResult();
                            Snackbar.make(findViewById(R.id.layout_root), R.string.add_sent, Snackbar.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(this::actionFailed)
                        .addOnCompleteListener(t -> buttonRequest.setEnabled(true));
                break;
            case FriendRepo.SearchResult.SENT:
                friendRepo.deleteRequest(result.request)
                        .addOnSuccessListener(v -> {
                            result.state = FriendRepo.SearchResult.NONE;
                            result.request = null;
                            showResult();
                        })
                        .addOnFailureListener(this::actionFailed)
                        .addOnCompleteListener(t -> buttonRequest.setEnabled(true));
                break;
            case FriendRepo.SearchResult.RECEIVED:
                friendRepo.accept(result.request)
                        .addOnSuccessListener(v -> {
                            result.state = FriendRepo.SearchResult.FRIEND;
                            showResult();
                            Toast.makeText(this, R.string.noti_accepted, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(this::actionFailed)
                        .addOnCompleteListener(t -> buttonRequest.setEnabled(true));
                break;
            default:
                buttonRequest.setEnabled(true);
        }
    }

    // 보안 규칙에 막히는 경우 등은 에러 문구 그대로
    private void actionFailed(Exception e) {
        Toast.makeText(this, e.getMessage() == null ? getString(R.string.noti_action_failed) : e.getMessage(),
                Toast.LENGTH_LONG).show();
    }

    // 기본 / 결과 / 없음 / 로딩 중 하나만 보이게
    private void showOnly(View target) {
        layoutDefault.setVisibility(target == layoutDefault ? View.VISIBLE : View.GONE);
        layoutResult.setVisibility(target == layoutResult ? View.VISIBLE : View.GONE);
        layoutNotFound.setVisibility(target == layoutNotFound ? View.VISIBLE : View.GONE);
        progress.setVisibility(target == progress ? View.VISIBLE : View.GONE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editCode.getWindowToken(), 0);
    }
}
