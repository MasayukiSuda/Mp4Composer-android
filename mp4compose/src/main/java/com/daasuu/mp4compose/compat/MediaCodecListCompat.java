package com.daasuu.mp4compose.compat;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.Nullable;

/**
 * A backwards compatible version of {@link MediaCodecList}.
 */
public class MediaCodecListCompat {

    @Nullable
    private final MediaCodecList mediaCodecList;

    public MediaCodecListCompat() {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            mediaCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        } else {
            mediaCodecList = null;
        }
    }

    /**
     * @see MediaCodecList#findDecoderForFormat(MediaFormat)
     */
    @Nullable
    public String findDecoderForFormat(final MediaFormat mediaFormat) {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP && mediaCodecList != null) {
            return mediaCodecList.findDecoderForFormat(mediaFormat);
        } else {
            return findEncoderDecoderV19(mediaFormat, false);
        }
    }

    /**
     * @see MediaCodecList#findEncoderForFormat(MediaFormat)
     */
    @Nullable
    public String findEncoderForFormat(final MediaFormat mediaFormat) {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP && mediaCodecList != null) {
            return mediaCodecList.findEncoderForFormat(mediaFormat);
        } else {
            return findEncoderDecoderV19(mediaFormat, true);
        }
    }

    @Nullable
    private static String findEncoderDecoderV19(final MediaFormat mediaFormat, final boolean encoderOnly) {
        final String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
        final int codecCount = MediaCodecList.getCodecCount();
        for (int i = 0; i < codecCount; i++) {
            final MediaCodecInfo mediaCodecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!mediaCodecInfo.isEncoder() && encoderOnly) {
                continue;
            }

            final String[] supportedTypes = mediaCodecInfo.getSupportedTypes();
            for (final String supportedType : supportedTypes) {
                if (supportedType.equals(mimeType)) {
                    return mediaCodecInfo.getName();
                }
            }
        }
        return null;
    }

}
