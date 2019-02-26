package com.daasuu.mp4compose.filter;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;

import com.daasuu.mp4compose.utils.EglUtil;

//OpenGL Lut filter using 512x512 color LUTs
public class GlLut512Filter extends GlFilter {

    private int hTex;
    private final int NO_TEXTURE = -1;
    private Bitmap lutTexture;

    private final static String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "uniform mediump sampler2D lutTexture; \n" +
                    "uniform lowp sampler2D sTexture; \n" +
                    "varying vec2 vTextureCoord; \n" +
                    "vec4 lookup(in vec4 textureColor, in sampler2D lookupTable);\n" +
                    "void main() {\n" +
                    "   vec4 color = texture2D(sTexture, vTextureCoord);\n" +
                    "   gl_FragColor = lookup(color, lutTexture);\n " +
                    "}\n" +
                    "vec4 lookup(in vec4 textureColor, in sampler2D lookupTable) {\n" +
                    "  textureColor = clamp(textureColor, 0.0, 1.0);\n" +
                    "  mediump float blueColor = textureColor.b * 63.0;\n" +
                    "  \n" +
                    "  mediump vec2 quad1;\n" +
                    "  quad1.y = floor(floor(blueColor) / 8.0);\n" +
                    "  quad1.x = floor(blueColor) - (quad1.y * 8.0);\n" +
                    "  \n" +
                    "  mediump vec2 quad2;\n" +
                    "  quad2.y = floor(ceil(blueColor) / 8.0);\n" +
                    "  quad2.x = ceil(blueColor) - (quad2.y * 8.0);\n" +
                    "  \n" +
                    "  highp vec2 texPos1;\n" +
                    "  texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);\n" +
                    "  texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n" +
                    "  \n" +
                    "  highp vec2 texPos2;\n" +
                    "  texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);\n" +
                    "  texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n" +
                    "  \n" +
                    "  lowp vec4 newColor1 = texture2D(lookupTable, texPos1);\n" +
                    "  lowp vec4 newColor2 = texture2D(lookupTable, texPos2);\n" +
                    "  \n" +
                    "  lowp vec4 newColor = mix(newColor1, newColor2, fract(blueColor));\n" +
                    "  return newColor;\n" +
                    "}";

    public GlLut512Filter(Bitmap bitmap) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
        this.lutTexture = bitmap;
        hTex = NO_TEXTURE;
    }

    public GlLut512Filter(Resources resources, int fxID) {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
        this.lutTexture = BitmapFactory.decodeResource(resources, fxID);
        hTex = NO_TEXTURE;
    }


    @Override
    public void setup() {
        super.setup();
        loadTexture();
    }

    private void loadTexture() {
        if (hTex == EglUtil.NO_TEXTURE) {
            hTex = EglUtil.loadTexture(lutTexture, EglUtil.NO_TEXTURE, false);
        }
    }

    @Override
    public void onDraw() {
        int offsetDepthMapTextureUniform = getHandle("lutTexture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, hTex);
        GLES20.glUniform1i(offsetDepthMapTextureUniform, 3);
    }

}
