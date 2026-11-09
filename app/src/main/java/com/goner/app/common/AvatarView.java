package com.goner.app.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.goner.app.R;

/**
 * 원형 아바타. photoUrl이 있으면 사진, 없으면 이름 첫 글자를 uid 해시 색 원 위에 그린다.
 * 크기는 app:avatarSize="small|large|medium"(24/48/36dp).
 */
public class AvatarView extends AppCompatImageView {

    private static final int[] BACKGROUND_COLORS = {
            R.color.orange_100, R.color.orange_200, R.color.orange_300, R.color.orange_700
    };
    private static final float TEXT_SIZE_RATIO = 0.42f;

    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int sizePx;
    private String initial = "";

    public AvatarView(@NonNull Context context) {
        this(context, null);
    }

    public AvatarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AvatarView);
        int size = a.getInt(R.styleable.AvatarView_avatarSize, 0);
        a.recycle();
        int sizeRes = size == 1 ? R.dimen.avatar_large : size == 2 ? R.dimen.avatar_medium : R.dimen.avatar_small;
        sizePx = getResources().getDimensionPixelSize(sizeRes);

        setScaleType(ScaleType.CENTER_CROP);
        circlePaint.setColor(ContextCompat.getColor(context, BACKGROUND_COLORS[0]));
        textPaint.setColor(ContextCompat.getColor(context, R.color.white));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(ResourcesCompat.getFont(context, R.font.ibm_plex_sans_kr), Typeface.BOLD));
    }

    /** uid로 배경색, name으로 첫 글자, photoUrl이 있으면 사진을 표시한다. */
    public void setUser(@NonNull String uid, @Nullable String name, @Nullable String photoUrl) {
        int index = Math.floorMod(uid.hashCode(), BACKGROUND_COLORS.length);
        circlePaint.setColor(ContextCompat.getColor(getContext(), BACKGROUND_COLORS[index]));
        String trimmed = name == null ? "" : name.trim();
        initial = trimmed.isEmpty() ? "" : trimmed.substring(0, trimmed.offsetByCodePoints(0, 1)).toUpperCase();

        if (TextUtils.isEmpty(photoUrl)) {
            Glide.with(this).clear(this);
            setImageDrawable(null);
        } else {
            Glide.with(this).load(photoUrl).circleCrop().into(this);
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(sizePx, sizePx);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (getDrawable() != null) {
            super.onDraw(canvas);
            return;
        }
        float radius = getWidth() / 2f;
        canvas.drawCircle(radius, getHeight() / 2f, radius, circlePaint);
        textPaint.setTextSize(getHeight() * TEXT_SIZE_RATIO);
        float baseline = getHeight() / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(initial, radius, baseline, textPaint);
    }
}
