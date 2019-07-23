package com.daasuu.sample.widget;

import android.view.MotionEvent;

class PinchGestureDetector {
    private float scale = 1.0f;

    private float adjustDistanceRate = 1f;

    private float distance;

    private float preDistance;

    private PinchGestureListener pinchGestureListener;

    public interface PinchGestureListener {
        void onPinchGestureListener(float scale);
    }

    PinchGestureDetector(PinchGestureListener dragGestureListener) {
        this.pinchGestureListener = dragGestureListener;
    }

    public float getScale() {
        return this.scale;
    }

    public float getDistance() {
        return this.distance;
    }

    public float getPreDistance() {
        return this.preDistance;
    }

    synchronized public boolean onTouchEvent(MotionEvent event) {

        float eventX = event.getX() * scale;
        float eventY = event.getY() * scale;
        int count = event.getPointerCount();

        int action = event.getAction() & MotionEvent.ACTION_MASK;
        int actionPointerIndex = event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK;

        switch (action) {
            case MotionEvent.ACTION_DOWN: {

                /** 最初のpointしか来ない */

                break;
            }
            case MotionEvent.ACTION_MOVE: {

                if (count == 2) {

                    float multiTouchX = event.getX(1) * scale;
                    float multiTouchY = event.getY(1) * scale;

                    distance = culcDistance(eventX, eventY, multiTouchX, multiTouchY);

                    float adjustDistance = distance + ((preDistance - distance) * adjustDistanceRate);

                    pinchGestureListener.onPinchGestureListener(distance / adjustDistance);
                    scale *= distance / preDistance;
                    preDistance = distance;

                }

                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {

                /** 2本の位置を記録　以後、moveにて距離の差分を算出 */

                if (count == 2) {
                    int downId = actionPointerIndex >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

                    float multiTouchX = event.getX(downId) * scale;
                    float multiTouchY = event.getY(downId) * scale;

                    distance = culcDistance(eventX, eventY, multiTouchX, multiTouchY);
                    float adjustDistance = distance + ((preDistance - distance) * adjustDistanceRate);
                    pinchGestureListener.onPinchGestureListener(adjustDistance);
                    preDistance = distance;
                }

                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {

                distance = 0;
                preDistance = 0;
                scale = 1.0f;

                break;
            }

            default:
        }
        return false;
    }

    private float culcDistance(float x1, float y1, float x2, float y2) {
        final float dx = x1 - x2;
        final float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public void setAdjustDistanceRate(float adjustDistanceRate) {
        this.adjustDistanceRate = adjustDistanceRate;
    }
}
