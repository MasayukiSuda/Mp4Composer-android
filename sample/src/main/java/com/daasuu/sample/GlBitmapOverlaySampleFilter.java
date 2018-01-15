package com.daasuu.sample;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.daasuu.mp4compose.filter.GlOverlayFilter;

/**
 * Created by sudamasayuki on 2018/01/07.
 */
public class GlBitmapOverlaySampleFilter extends GlOverlayFilter {

    private Bitmap bitmap;

    public GlBitmapOverlaySampleFilter(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    protected void drawCanvas(Canvas canvas) {
        if (bitmap != null && !bitmap.isRecycled()) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
    }

    @Override
    public void release() {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}