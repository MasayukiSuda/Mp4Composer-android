package com.daasuu.mp4compose.composer;


import android.media.MediaCodec;
import android.os.Build;

import java.nio.ByteBuffer;

// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/compat/MediaCodecBufferCompatWrapper.java

/**
 * A Wrapper to MediaCodec that facilitates the use of API-dependent get{Input/Output}Buffer methods,
 * in order to prevent: http://stackoverflow.com/q/30646885
 */

class MediaCodecBufferCompatWrapper {
    private final MediaCodec mMediaCodec;
    private final ByteBuffer[] mInputBuffers;
    private final ByteBuffer[] mOutputBuffers;

    MediaCodecBufferCompatWrapper(MediaCodec mediaCodec) {
        mMediaCodec = mediaCodec;

        if (Build.VERSION.SDK_INT < 21) {
            mInputBuffers = mediaCodec.getInputBuffers();
            mOutputBuffers = mediaCodec.getOutputBuffers();
        } else {
            mInputBuffers = mOutputBuffers = null;
        }
    }

    ByteBuffer getInputBuffer(final int index) {
        if (Build.VERSION.SDK_INT >= 21) {
            return mMediaCodec.getInputBuffer(index);
        }
        return mInputBuffers[index];
    }

    ByteBuffer getOutputBuffer(final int index) {
        if (Build.VERSION.SDK_INT >= 21) {
            return mMediaCodec.getOutputBuffer(index);
        }
        return mOutputBuffers[index];
    }

}
