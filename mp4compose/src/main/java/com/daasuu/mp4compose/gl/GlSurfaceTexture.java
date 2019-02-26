package com.daasuu.mp4compose.gl;

import android.graphics.SurfaceTexture;

public class GlSurfaceTexture implements SurfaceTexture.OnFrameAvailableListener {

    private SurfaceTexture surfaceTexture;
    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener;

    public GlSurfaceTexture(final int texName) {
        surfaceTexture = new SurfaceTexture(texName);
        surfaceTexture.setOnFrameAvailableListener(this);
    }


    public void setOnFrameAvailableListener(final SurfaceTexture.OnFrameAvailableListener l) {
        onFrameAvailableListener = l;
    }


    public int getTextureTarget() {
        return GlPreview.GL_TEXTURE_EXTERNAL_OES;
    }

    public void updateTexImage() {
        surfaceTexture.updateTexImage();
    }

    public void getTransformMatrix(final float[] mtx) {
        surfaceTexture.getTransformMatrix(mtx);
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
        if (onFrameAvailableListener != null) {
            onFrameAvailableListener.onFrameAvailable(this.surfaceTexture);
        }
    }

    public void release() {
        surfaceTexture.release();
    }
}


