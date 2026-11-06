package com.goner.app.common;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.goner.app.R;

// layout/view_top_bar.xml 을 include 한 뷰를 설정한다.
public final class TopBar {

    private TopBar() {
    }

    public static void bind(@NonNull View topBar, @NonNull CharSequence title,
                            @NonNull View.OnClickListener onBack) {
        topBar.findViewById(R.id.top_bar_back).setOnClickListener(onBack);
        ((TextView) topBar.findViewById(R.id.top_bar_title)).setText(title);
    }

    // 오른쪽 텍스트 버튼(편집/완료 등). text가 null이면 숨긴다.
    public static void setAction(@NonNull View topBar, @Nullable CharSequence text,
                                 @Nullable View.OnClickListener onClick) {
        TextView action = topBar.findViewById(R.id.top_bar_action);
        action.setText(text);
        action.setOnClickListener(onClick);
        action.setVisibility(text == null ? View.INVISIBLE : View.VISIBLE);
    }
}
