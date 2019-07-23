package com.daasuu.sample.widget;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

class AllGestureDetector implements DragGestureDetector.DragGestureListener, RotateGestureDetector.RotateGestureListener, PinchGestureDetector.PinchGestureListener {

    private static final float DEFAULT_LIMIT_SCALE_MAX = 2.7f;
    private static final float DEFAULT_LIMIT_SCALE_MIN = 0.5f;

    private float limitScaleMax = DEFAULT_LIMIT_SCALE_MAX;
    private float limitScaleMin = DEFAULT_LIMIT_SCALE_MIN;

    private float scaleFactor = 1.0f;

    private final RotateGestureDetector rotateGestureDetector;
    private final DragGestureDetector dragGestureDetector;
    private final PinchGestureDetector pinchGestureDetector;
    private final View view;

    private float angle = 0f;
    private boolean rotateFlag = true;

    private MoveDragXYListener moveDragXYListener;

    AllGestureDetector(View view) {
        dragGestureDetector = new DragGestureDetector(this);
        rotateGestureDetector = new RotateGestureDetector(this);
        pinchGestureDetector = new PinchGestureDetector(this);
        this.view = view;
    }

    void onTouch(MotionEvent event) {
        if (rotateFlag) {
            rotateGestureDetector.onTouchEvent(event);
        }
        dragGestureDetector.onTouchEvent(event);
        pinchGestureDetector.onTouchEvent(event);
    }

    void noRotate() {
        rotateFlag = false;
    }

    public void setMoveDragXYListener(MoveDragXYListener moveDragXYListener) {
        this.moveDragXYListener = moveDragXYListener;
    }

    void updateAngle() {
        this.angle = view.getRotation();
    }

    public void setLimitScaleMax(float limit) {
        this.limitScaleMax = limit;
    }

    void setLimitScaleMin(float limit) {
        this.limitScaleMin = limit;
    }


    @Override
    public void onPinchGestureListener(float scale) {
        float tmpScale = scaleFactor * scale;

        if (limitScaleMin <= tmpScale && tmpScale <= limitScaleMax) {
            scaleFactor = tmpScale;
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);
        }

    }


    // rotate
    @Override
    public void onRotation(float deltaAngle) {
        angle += deltaAngle;
        view.setRotation(view.getRotation() + deltaAngle);
    }


    @Override
    synchronized public void onDragGestureListener(float deltaX, float deltaY) {

        // touch move

        float dx = deltaX;
        float dy = deltaY;
        PointF pf = createRotatePointF(0, 0, angle, dx, dy);

        dx = pf.x;
        dy = pf.y;

        float x = view.getX() + dx * scaleFactor;
        float y = view.getY() + dy * scaleFactor;

        if (moveDragXYListener != null) {
            moveDragXYListener.onMove(x, y);
        }

        view.setX(x);
        view.setY(y);
    }


    private static PointF createRotatePointF(float centerX, float centerY, float angle, float x, float y) {

        double rad = Math.toRadians(angle);

        float resultX = (float) ((x - centerX) * Math.cos(rad) - (y - centerY) * Math.sin(rad) + centerX);
        float resultY = (float) ((x - centerX) * Math.sin(rad) + (y - centerY) * Math.cos(rad) + centerY);

        return new PointF(resultX, resultY);
    }

    public interface MoveDragXYListener {
        void onMove(float x, float y);
    }

}

