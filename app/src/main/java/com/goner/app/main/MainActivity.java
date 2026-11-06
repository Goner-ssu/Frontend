package com.goner.app.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.goner.app.R;
import com.goner.app.mypage.MyPageFragment;
import com.goner.app.record.RecordFragment;

public class MainActivity extends AppCompatActivity {

    // 약속 만들기 화면에서 돌아올 때 이 값 true로 넣어주면 토스트 뜸
    public static final String EXTRA_PROMISE_CREATED = "promise_created";

    private static final String[] TAGS = {"record", "home", "mypage"};
    private static final String KEY_TAB = "tab";
    private static final long TOAST_DURATION = 2500;

    private BottomNavView bottomNav;
    private TextView toast;
    private final Fragment[] fragments = new Fragment[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        toast = findViewById(R.id.main_toast);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            bottomNav.setBottomInset(bars.bottom);
            return insets;
        });

        FragmentManager fm = getSupportFragmentManager();
        int tab = BottomNavView.TAB_HOME;
        if (savedInstanceState == null) {
            // 탭 3개 다 미리 넣어두고 show/hide로 바꿈
            fragments[0] = new RecordFragment();
            fragments[1] = new HomeFragment();
            fragments[2] = new MyPageFragment();
            FragmentTransaction ft = fm.beginTransaction();
            for (int i = 0; i < 3; i++) {
                ft.add(R.id.main_container, fragments[i], TAGS[i]);
                if (i != tab) ft.hide(fragments[i]);
            }
            ft.commit();
        } else {
            for (int i = 0; i < 3; i++) {
                fragments[i] = fm.findFragmentByTag(TAGS[i]);
            }
            tab = savedInstanceState.getInt(KEY_TAB, BottomNavView.TAB_HOME);
        }

        bottomNav.setSelectedTab(tab);
        bottomNav.setOnTabSelectedListener(this::showTab);

        checkCreatedToast(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkCreatedToast(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB, bottomNav.getSelectedTab());
    }

    private void showTab(int tab) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        for (int i = 0; i < 3; i++) {
            if (i == tab) ft.show(fragments[i]);
            else ft.hide(fragments[i]);
        }
        ft.commit();
    }

    private void checkCreatedToast(Intent intent) {
        if (!intent.getBooleanExtra(EXTRA_PROMISE_CREATED, false)) return;
        intent.removeExtra(EXTRA_PROMISE_CREATED); // 화면 돌려도 다시 안 뜨게

        bottomNav.setSelectedTab(BottomNavView.TAB_HOME);
        showTab(BottomNavView.TAB_HOME);
        toast.setVisibility(View.VISIBLE);
        toast.removeCallbacks(hideToast);
        toast.postDelayed(hideToast, TOAST_DURATION);
    }

    private final Runnable hideToast = () -> toast.setVisibility(View.GONE);
}
