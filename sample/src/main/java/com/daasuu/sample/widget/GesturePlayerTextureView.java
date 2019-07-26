package com.daasuu.sample.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

@SuppressLint("ViewConstructor")
public class GesturePlayerTextureView extends PlayerTextureView implements View.OnTouchListener {

    private final AllGestureDetector allGestureDetector;

    // 基準となる枠のサイズ
    private float baseWidthSize = 0;

    public GesturePlayerTextureView(Context context, String path) {
        super(context, path);
        setOnTouchListener(this);
        allGestureDetector = new AllGestureDetector(this);
        allGestureDetector.setLimitScaleMin(0.1f);
        allGestureDetector.noRotate();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        allGestureDetector.onTouch(event);
        return true;
    }

    public void setBaseWidthSize(float baseSize) {
        this.baseWidthSize = baseSize;
        requestLayout();
    }

    public void updateRotate() {
        final int rotation = (int) getRotation();

        switch (rotation) {
            case 0:
                super.setRotation(90f);
                break;
            case 90:
                super.setRotation(180f);
                break;
            case 180:
                super.setRotation(270f);
                break;
            case 270:
                super.setRotation(0f);
                break;
        }

        allGestureDetector.updateAngle();
    }


    @Override
    public void setRotation(float rotation) {
        // do nothing
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (videoAspect == DEFAULT_ASPECT || baseWidthSize == 0) return;

        // 正方形
        if (videoAspect == 1.0f) {
            setMeasuredDimension((int) baseWidthSize, (int) baseWidthSize);
            return;
        }

        // 縦長 or 横長
        setMeasuredDimension((int) baseWidthSize, (int) (baseWidthSize / videoAspect));

    }
}

