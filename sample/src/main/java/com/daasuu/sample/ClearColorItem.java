package com.daasuu.sample;

public class ClearColorItem {
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    public ClearColorItem(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public float getRed() {
        return red;
    }

    public float getGreen() {
        return green;
    }

    public float getBlue() {
        return blue;
    }

    public float getAlpha() {
        return alpha;
    }
}
