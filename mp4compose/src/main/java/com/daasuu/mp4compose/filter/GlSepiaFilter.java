package com.daasuu.mp4compose.filter;

import com.daasuu.mp4compose.utils.GlUtils;

/**
 * Created by sudamasayuki on 2017/11/14.
 */

public class GlSepiaFilter extends GlFilter {
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "   vec4 fragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "   gl_FragColor.r = dot(fragColor.rgb, vec3(.393, .769, .189));\n" +
                    "   gl_FragColor.g = dot(fragColor.rgb, vec3(.349, .686, .168));\n" +
                    "   gl_FragColor.b = dot(fragColor.rgb, vec3(.272, .534, .131));\n" +
                    "}";

    public GlSepiaFilter() {
        super(GlUtils.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
    }

}
