package com.daasuu.sample.widget;

import android.view.MotionEvent;

class RotateGestureDetector {

    private final static int SLOPE_0 = 10000;

    private RotateGestureListener rotationGestureListener;

    private float angle;
    private float downX = 0;
    private float downY = 0;
    private float downX2 = 0;
    private float downY2 = 0;
    private boolean isFirstPointerUp = false;

    public interface RotateGestureListener {
        void onRotation(float deltaAngle);
    }

    RotateGestureDetector(RotateGestureListener rotationGestureListener2) {
        this.rotationGestureListener = rotationGestureListener2;
    }

    @SuppressWarnings("deprecation")
    synchronized public boolean onTouchEvent(MotionEvent event) {

        float eventX = event.getX();
        float eventY = event.getY();
        int count = event.getPointerCount();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                downX = eventX;
                downY = eventY;
                if (count >= 2) {
                    downX2 = event.getX(1);
                    downY2 = event.getY(1);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                downX2 = event.getX(1);
                downY2 = event.getY(1);
                break;
            case MotionEvent.ACTION_MOVE:

                if (count >= 2) {

                    // 回転角度の取得
                    float angle = getAngle(downX, downY, downX2, downY2, eventX, eventY, event.getX(1), event.getY(1));

                    // 画像の回転
                    if (angle != SLOPE_0) {
                        this.angle -= angle * 180d / Math.PI;
                    }

                    downX2 = event.getX(1);
                    downY2 = event.getY(1);

                    if (rotationGestureListener != null) {
                        rotationGestureListener.onRotation(getDeltaAngle());
                    }
                }

                break;
            case MotionEvent.ACTION_POINTER_UP:
                switch (event.getAction()) {
                    case MotionEvent.ACTION_POINTER_1_UP:
                        isFirstPointerUp = true;
                        break;
                    default:
                }
                break;
            default:
        }

        if (isFirstPointerUp) {
            downX = downX2;
            downY = downY2;
            isFirstPointerUp = false;
        } else {
            downX = eventX;
            downY = eventY;
        }

        return true;
    }

    private float getDeltaAngle() {
        return angle;
    }

    private static float getAngle(float xi1, float yi1, float xm1, float ym1, float xi2, float yi2, float xm2, float ym2) {

        // 2本の直線の傾き・y切片を算出
        float firstLinearSlope;
        if ((xm1 - xi1) != 0 && (ym1 - yi1) != 0) {
            firstLinearSlope = (xm1 - xi1) / (ym1 - yi1);
        } else {
            return SLOPE_0;
        }

        float secondLinearSlope = (xm2 - xi2) / (ym2 - yi2);
        if ((xm2 - xi2) != 0 && (ym2 - yi2) != 0) {
            secondLinearSlope = (xm2 - xi2) / (ym2 - yi2);
        } else {
            return SLOPE_0;
        }

        if (firstLinearSlope * secondLinearSlope == -1) {
            return 90.0f;
        }

        float tan = (secondLinearSlope - firstLinearSlope) / (1 + secondLinearSlope * firstLinearSlope);

        return (float) Math.atan(tan);
    }

}
