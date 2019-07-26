package com.daasuu.sample.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class PortraitView extends FrameLayout {
    public PortraitView(@NonNull Context context) {
        super(context);
    }

    public PortraitView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PortraitView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = getMeasuredHeight();
        setMeasuredDimension((int) (height / 16f * 9), height);
    }

}
