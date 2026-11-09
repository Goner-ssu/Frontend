package com.goner.app.main;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.goner.app.R;

// 하단 탭 (기록 보기 / 홈 / 마이페이지)
public class BottomNavView extends LinearLayout {

    public static final int TAB_RECORD = 0;
    public static final int TAB_HOME = 1;
    public static final int TAB_MYPAGE = 2;

    public interface OnTabSelectedListener {
        void onTabSelected(int tab);
    }

    private final LinearLayout[] tabs = new LinearLayout[3];
    private final ImageView[] icons = new ImageView[3];
    private final TextView[] labels = new TextView[3];
    private OnTabSelectedListener listener;
    private int selectedTab = -1;
    private final int basePaddingBottom;

    public BottomNavView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.BOTTOM);
        setBackgroundResource(R.drawable.bg_bottom_nav);
        setElevation(getResources().getDimension(R.dimen.nav_elevation));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setOutlineSpotShadowColor(ContextCompat.getColor(context, R.color.shadow_orange));
        }
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.bottom_nav_height));

        int side = getResources().getDimensionPixelSize(R.dimen.nav_padding_horizontal);
        basePaddingBottom = getResources().getDimensionPixelSize(R.dimen.nav_padding_bottom);
        setPadding(side, 0, side, basePaddingBottom);

        LayoutInflater.from(context).inflate(R.layout.view_bottom_nav, this, true);
        tabs[TAB_RECORD] = findViewById(R.id.tab_record);
        tabs[TAB_HOME] = findViewById(R.id.tab_home);
        tabs[TAB_MYPAGE] = findViewById(R.id.tab_mypage);
        icons[TAB_RECORD] = findViewById(R.id.tab_record_icon);
        icons[TAB_HOME] = findViewById(R.id.tab_home_icon);
        icons[TAB_MYPAGE] = findViewById(R.id.tab_mypage_icon);
        labels[TAB_RECORD] = findViewById(R.id.tab_record_label);
        labels[TAB_HOME] = findViewById(R.id.tab_home_label);
        labels[TAB_MYPAGE] = findViewById(R.id.tab_mypage_label);

        for (int i = 0; i < tabs.length; i++) {
            final int tab = i;
            tabs[i].setOnClickListener(v -> {
                if (tab == selectedTab) return;
                setSelectedTab(tab);
                if (listener != null) listener.onTabSelected(tab);
            });
        }
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    // 선택된 탭만 흰 알약 + 주황색
    public void setSelectedTab(int tab) {
        selectedTab = tab;
        int on = ContextCompat.getColor(getContext(), R.color.primary);
        int off = ContextCompat.getColor(getContext(), R.color.text_tab_off);
        float pillElevation = getResources().getDimension(R.dimen.nav_pill_elevation);

        for (int i = 0; i < tabs.length; i++) {
            boolean selected = i == tab;
            tabs[i].setBackgroundResource(selected ? R.drawable.bg_nav_pill : 0);
            tabs[i].setElevation(selected ? pillElevation : 0);
            icons[i].setImageTintList(ColorStateList.valueOf(selected ? on : off));
            labels[i].setTextColor(selected ? on : off);
        }
    }

    public int getSelectedTab() {
        return selectedTab;
    }

    // 3버튼 -> 시스템 바가 높을 때만 아래 여백 늘림
    public void setBottomInset(int inset) {
        setPadding(getPaddingLeft(), 0, getPaddingRight(), Math.max(basePaddingBottom, inset));
    }
}
