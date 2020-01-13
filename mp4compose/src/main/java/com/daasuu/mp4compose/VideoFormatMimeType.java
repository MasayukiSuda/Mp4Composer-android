package com.daasuu.mp4compose;

import android.media.MediaFormat;

public enum VideoFormatMimeType {
    HEVC(MediaFormat.MIMETYPE_VIDEO_HEVC),
    AVC(MediaFormat.MIMETYPE_VIDEO_AVC),
    MPEG4(MediaFormat.MIMETYPE_VIDEO_MPEG4),
    H263(MediaFormat.MIMETYPE_VIDEO_H263),
    AUTO("");

    private final String videoFormatMimeType;

    VideoFormatMimeType(String videoFormatMimeType) {
        this.videoFormatMimeType = videoFormatMimeType;
    }

    public String getFormat() {
        return videoFormatMimeType;
    }
}
