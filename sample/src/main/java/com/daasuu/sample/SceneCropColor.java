package com.daasuu.sample;

import android.support.annotation.ColorRes;

public enum SceneCropColor {

    WHITE(R.color.colorWhite, new ClearColorItem(1f, 1f, 1f, 1f)),
    GRAY(R.color.crop_background_gray, new ClearColorItem(0.867f, 0.867f, 0.867f, 1f)),
    DARK(R.color.crop_background_dark, new ClearColorItem(0.267f, 0.267f, 0.267f, 1f)),
    BLACK(R.color.colorBlack, new ClearColorItem(0f, 0f, 0f, 1f)),
    PINK(R.color.crop_background_pink, new ClearColorItem(1f, 0.827f, 0.87f, 1f)),
    FLESH(R.color.crop_background_flesh, new ClearColorItem(1f, 0.945f, 0.768f, 1f)),
    GREEN(R.color.crop_background_green, new ClearColorItem(0.905f, 1f, 0.898f, 1f)),
    BLUE(R.color.crop_background_blue, new ClearColorItem(0.898f, 0.937f, 1f, 1f)),
    BROWN(R.color.crop_background_brown, new ClearColorItem(0.85f, 0.807f, 0.745f, 1f));

    private final int colorRes;
    private final ClearColorItem clearColorItem;

    SceneCropColor(@ColorRes int colorRes, ClearColorItem ClearColorItem) {
        this.colorRes = colorRes;
        this.clearColorItem = ClearColorItem;
    }

    public int getColorRes() {
        return colorRes;
    }

    public ClearColorItem getClearColorItem() {
        return clearColorItem;
    }
}
