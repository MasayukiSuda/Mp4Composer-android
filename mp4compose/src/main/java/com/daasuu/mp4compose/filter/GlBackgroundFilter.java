package com.daasuu.mp4compose.filter;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class GlBackgroundFilter extends GlOverlayFilter {

    private Bitmap backgorund;

    public GlBackgroundFilter(Bitmap backgorund) {
        this.backgorund = backgorund;
    }

    @Override
    protected void drawCanvas(Canvas canvas) {
        canvas.drawBitmap(backgorund, 0, 0, null);
    }

}