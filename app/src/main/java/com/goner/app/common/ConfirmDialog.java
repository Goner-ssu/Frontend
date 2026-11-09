package com.goner.app.common;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.goner.app.R;

/**
 * 가운데 확인 팝업(제목 + 설명 + 취소/확인).
 * 확인을 누르면 requestKey 로 Fragment Result 를 보낸다:
 * <pre>
 * getSupportFragmentManager().setFragmentResultListener(KEY, this, (key, result) -> deleteNotification());
 * ConfirmDialog.show(getSupportFragmentManager(), KEY, "알림을 삭제할까요?", "삭제한 알림은 되돌릴 수 없어요", "삭제");
 * </pre>
 */
public class ConfirmDialog extends DialogFragment {

    private static final String ARG_REQUEST_KEY = "requestKey";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_CONFIRM_TEXT = "confirmText";

    public static void show(@NonNull FragmentManager fm, @NonNull String requestKey,
                            @NonNull String title, @Nullable String message,
                            @Nullable String confirmText) {
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_KEY, requestKey);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_CONFIRM_TEXT, confirmText);
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setArguments(args);
        dialog.show(fm, requestKey);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_confirm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        ((TextView) view.findViewById(R.id.dialog_title)).setText(args.getString(ARG_TITLE));

        TextView message = view.findViewById(R.id.dialog_message);
        String messageText = args.getString(ARG_MESSAGE);
        message.setText(messageText);
        message.setVisibility(messageText == null ? View.GONE : View.VISIBLE);

        Button confirm = view.findViewById(R.id.dialog_confirm);
        String confirmText = args.getString(ARG_CONFIRM_TEXT);
        if (confirmText != null) {
            confirm.setText(confirmText);
        }
        confirm.setOnClickListener(v -> {
            getParentFragmentManager().setFragmentResult(args.getString(ARG_REQUEST_KEY), Bundle.EMPTY);
            dismiss();
        });
        view.findViewById(R.id.dialog_cancel).setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
