package com.daasuu.mp4compose.filter;

import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.daasuu.mp4compose.utils.GlUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;

/**
 * Created by sudamasayuki on 2017/11/14.
 */

public class GlFilter {

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private final float[] triangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f, 1.0f, 0, 0.f, 1.f,
            1.0f, 1.0f, 0, 1.f, 1.f,
    };
    private FloatBuffer triangleVertices;

    private String vertexShaderSource;
    private String fragmentShaderSource;


    private int program;
    private int textureID = -12345;
    protected float[] clearColor = new float[]{0f, 0f, 0f, 1f};

    private final HashMap<String, Integer> handleMap = new HashMap<>();


    public GlFilter() {
        this(GlUtils.DEFAULT_VERTEX_SHADER, GlUtils.DEFAULT_FRAGMENT_SHADER);
    }

    public GlFilter(final Resources res, final int vertexShaderSourceResId, final int fragmentShaderSourceResId) {
        this(res.getString(vertexShaderSourceResId), res.getString(fragmentShaderSourceResId));
    }

    public GlFilter(final String vertexShaderSource, final String fragmentShaderSource) {
        this.vertexShaderSource = vertexShaderSource;
        this.fragmentShaderSource = fragmentShaderSource;

        triangleVertices = ByteBuffer.allocateDirect(
                triangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        triangleVertices.put(triangleVerticesData).position(0);
    }


    public int getTextureId() {
        return textureID;
    }


    public void draw(SurfaceTexture surfaceTexture, float[] STMatrix, float[] MVPMatrix) {
        GlUtils.checkGlError("onDrawFrame start");


        GLES20.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(program);
        GlUtils.checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID);
        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(getHandle("aPosition"), 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        GLES20.glEnableVertexAttribArray(getHandle("aPosition"));

        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(getHandle("aTextureCoord"), 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        GlUtils.checkGlError("glVertexAttribPointer aTextureHandle");

        GLES20.glEnableVertexAttribArray(getHandle("aTextureCoord"));
        GlUtils.checkGlError("glEnableVertexAttribArray aTextureHandle");

        surfaceTexture.getTransformMatrix(STMatrix);

        GLES20.glUniformMatrix4fv(getHandle("uMVPMatrix"), 1, false, MVPMatrix, 0);
        GLES20.glUniformMatrix4fv(getHandle("uSTMatrix"), 1, false, STMatrix, 0);


        onDraw();


        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GlUtils.checkGlError("glDrawArrays");

        GLES20.glFinish();
    }

    protected void onDraw() {
    }


    public void setUpSurface() {
        final int vertexShader = GlUtils.loadShader(vertexShaderSource, GLES20.GL_VERTEX_SHADER);
        final int fragmentShader = GlUtils.loadShader(fragmentShaderSource, GLES20.GL_FRAGMENT_SHADER);
        program = GlUtils.createProgram(vertexShader, fragmentShader);
        if (program == 0) {
            throw new RuntimeException("failed creating program");
        }

        getHandle("aPosition");
        getHandle("aTextureCoord");
        getHandle("uMVPMatrix");
        getHandle("uSTMatrix");

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID);
        GlUtils.checkGlError("glBindTexture textureID");
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GlUtils.checkGlError("glTexParameter");
    }

    protected final int getHandle(final String name) {
        final Integer value = handleMap.get(name);
        if (value != null) {
            return value;
        }

        int location = GLES20.glGetAttribLocation(program, name);
        if (location == -1) {
            location = GLES20.glGetUniformLocation(program, name);
        }
        if (location == -1) {
            throw new IllegalStateException("Could not get attrib or uniform location for " + name);
        }
        handleMap.put(name, location);
        return location;
    }

    public void release() {
    }

    public void setClearColor(float red,
                              float green,
                              float blue,
                              float alpha) {
        this.clearColor = new float[]{red, green, blue, alpha};
    }
}
