package com.daasuu.mp4compose.composer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import com.daasuu.mp4compose.SampleType;
import com.daasuu.mp4compose.logger.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;


// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/PassThroughTrackTranscoder.java
class AudioComposer implements IAudioComposer {

    private static final String TAG = "AudioComposer";

    private final MediaExtractor mediaExtractor;
    private final int trackIndex;
    private final MuxRender muxRender;
    private final SampleType sampleType = SampleType.AUDIO;
    private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private int bufferSize;
    private ByteBuffer buffer;
    private boolean isEOS;
    private long writtenPresentationTimeUs;

    private final long trimStartUs;
    private final long trimEndUs;

    private final Logger logger;

    AudioComposer(@NonNull MediaExtractor mediaExtractor, int trackIndex,
                  @NonNull MuxRender muxRender, long trimStartMs, long trimEndMs,
                  @NonNull Logger logger) {
        this.mediaExtractor = mediaExtractor;
        this.trackIndex = trackIndex;
        this.muxRender = muxRender;
        this.trimStartUs = TimeUnit.MILLISECONDS.toMicros(trimStartMs);
        this.trimEndUs = trimEndMs == -1 ? trimEndMs : TimeUnit.MILLISECONDS.toMicros(trimEndMs);
        this.logger = logger;

        final MediaFormat actualOutputFormat = this.mediaExtractor.getTrackFormat(this.trackIndex);
        this.muxRender.setOutputFormat(this.sampleType, actualOutputFormat);
        bufferSize = actualOutputFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE) ? actualOutputFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) : (64 * 1024);
        buffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
        mediaExtractor.seekTo(trimStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
    }


    @SuppressLint("Assert")
    public boolean stepPipeline() {
        if (isEOS) return false;
        int trackIndex = mediaExtractor.getSampleTrackIndex();
        logger.debug(TAG, "stepPipeline trackIndex:" + trackIndex);
        if (trackIndex < 0 || (writtenPresentationTimeUs >= trimEndUs && trimEndUs != -1)) {
            buffer.clear();
            bufferInfo.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            muxRender.writeSampleData(sampleType, buffer, bufferInfo);
            isEOS = true;
            mediaExtractor.unselectTrack(this.trackIndex);
            return true;
        }
        if (trackIndex != this.trackIndex) return false;

        buffer.clear();
        int sampleSize = mediaExtractor.readSampleData(buffer, 0);
        if (sampleSize > bufferSize) {
            logger.warning(TAG, "Sample size smaller than buffer size, resizing buffer: " + sampleSize);
            bufferSize = 2 * sampleSize;
            buffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
        }
        boolean isKeyFrame = (mediaExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        int flags = isKeyFrame ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;

        if (mediaExtractor.getSampleTime() >= trimStartUs && (mediaExtractor.getSampleTime() <= trimEndUs || trimEndUs == -1)) {
            bufferInfo.set(0, sampleSize, mediaExtractor.getSampleTime(), flags);
            muxRender.writeSampleData(sampleType, buffer, bufferInfo);
        }

        writtenPresentationTimeUs = mediaExtractor.getSampleTime();
        mediaExtractor.advance();
        return true;
    }

    @Override
    public long getWrittenPresentationTimeUs() {
        return writtenPresentationTimeUs;
    }

    @Override
    public boolean isFinished() {
        return isEOS;
    }

    @Override
    public void setup() {
        // do nothing
    }

    @Override
    public void release() {
        // do nothing
    }
}
