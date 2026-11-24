package com.goner.app.promise;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.goner.app.R;

import java.util.ArrayList;
import java.util.List;

// 위아래로 굴려서 고르는 휠 (시/분/AM·PM 한 칸씩)
public class WheelPicker extends RecyclerView {

    public interface OnValueChangeListener {
        void onValueChanged(int index);
    }

    private static final float FAR_ALPHA = 0.25f; // 가운데에서 2칸 이상 떨어진 줄

    private final List<String> items = new ArrayList<>();
    private final LinearLayoutManager layoutManager;
    private final LinearSnapHelper snapHelper = new LinearSnapHelper();
    private final int rowHeight;
    private final Typeface typeface;
    private int selected = 0;
    private OnValueChangeListener listener;

    public WheelPicker(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        rowHeight = getResources().getDimensionPixelSize(R.dimen.wheel_row_height);
        typeface = ResourcesCompat.getFont(context, R.font.ibm_plex_sans_kr);

        layoutManager = new LinearLayoutManager(context);
        setLayoutManager(layoutManager);
        setClipToPadding(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        snapHelper.attachToRecyclerView(this);
        setAdapter(new WheelAdapter());

        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                updateStyles();
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState != SCROLL_STATE_IDLE) return;
                View center = snapHelper.findSnapView(layoutManager);
                if (center == null) return;
                int pos = getChildAdapterPosition(center);
                if (pos != NO_POSITION && pos != selected) {
                    selected = pos;
                    if (listener != null) listener.onValueChanged(pos);
                }
            }
        });
    }

    public void setItems(List<String> values) {
        items.clear();
        items.addAll(values);
        getAdapter().notifyDataSetChanged();
    }

    public void setOnValueChangeListener(OnValueChangeListener listener) {
        this.listener = listener;
    }

    public void setSelected(int index) {
        selected = index;
        layoutManager.scrollToPositionWithOffset(index, 0);
        post(this::updateStyles);
    }

    public int getSelected() {
        return selected;
    }

    // 가운데 줄이 정확히 가운데 오게 위아래 여백 줌
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int pad = (h - rowHeight) / 2;
        setPadding(getPaddingLeft(), pad, getPaddingRight(), pad);
        post(() -> setSelected(selected));
    }

    // 가운데는 크고 굵게, 바로 옆은 연하게, 그 밖은 거의 안 보이게 (피그마 마스크처럼)
    private void updateStyles() {
        float center = getHeight() / 2f;
        for (int i = 0; i < getChildCount(); i++) {
            TextView tv = (TextView) getChildAt(i);
            float childCenter = (tv.getTop() + tv.getBottom()) / 2f;
            int distance = Math.round(Math.abs(childCenter - center) / rowHeight);

            if (distance == 0) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_22));
                tv.setTypeface(typeface, Typeface.BOLD);
                tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_main));
                tv.setAlpha(1f);
            } else {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_17));
                tv.setTypeface(typeface, Typeface.NORMAL);
                int color = distance == 1 ? R.color.text_hint : R.color.wheel_far;
                tv.setTextColor(ContextCompat.getColor(getContext(), color));
                tv.setAlpha(distance == 1 ? 1f : FAR_ALPHA);
            }
        }
    }

    private class WheelAdapter extends Adapter<ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight));
            tv.setGravity(Gravity.CENTER);
            tv.setIncludeFontPadding(false);
            tv.setTypeface(typeface);
            return new ViewHolder(tv) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(items.get(position));
            post(WheelPicker.this::updateStyles);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
