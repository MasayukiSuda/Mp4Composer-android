package com.daasuu.mp4compose.composer;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by TAPOS DATTA on 22,May,2020
 */

abstract class BaseAudioChannel {

    protected static class AudioBuffer {
        int bufferIndex;
        long presentationTimeUs;
        ShortBuffer data;
    }
    protected static class BufferInfo{
        long totaldata;
        long presentationTimeUs;
    }

    static final int BUFFER_INDEX_END_OF_STREAM = -1;
    protected static final int BYTE_PER_SAMPLE = 16 / 8 ;
    protected static final int BYTES_PER_SHORT = 2;
    protected static final long MICROSECS_PER_SEC = 1000000;

    protected final Queue<AudioBuffer> emptyBuffers = new ArrayDeque<>();
    protected final Queue<AudioBuffer> filledBuffers = new ArrayDeque<>();

    protected final MediaCodec decoder;
    protected final MediaCodec encoder;
    protected final MediaFormat encodeFormat;

    protected int inputSampleRate;
    protected int inputChannelCount;
    protected int outputChannelCount;

    protected final AudioBuffer overflowBuffer = new AudioBuffer();

    protected MediaFormat actualDecodedFormat;

    BaseAudioChannel(final MediaCodec decoder,
                     final MediaCodec encoder, final MediaFormat encodeFormat) {
        this.decoder = decoder;
        this.encoder = encoder;
        this.encodeFormat = encodeFormat;
    }

    public void setActualDecodedFormat(final MediaFormat decodedFormat) {
        actualDecodedFormat = decodedFormat;

        inputSampleRate = actualDecodedFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        if (inputSampleRate != encodeFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)) {
            throw new UnsupportedOperationException("Audio sample rate conversion not supported yet.");
        }

        inputChannelCount = actualDecodedFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        outputChannelCount = encodeFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        if (outputChannelCount != 1 && outputChannelCount != 2) {
            throw new UnsupportedOperationException("Output channel count (" + outputChannelCount + ") not supported.");
        }

        overflowBuffer.presentationTimeUs = 0;
    }

    protected abstract long sampleCountToDurationUs(final long sampleCount, final int sampleRate, final int channelCount);

    protected abstract void drainDecoderBufferAndQueue(final int bufferIndex, final long presentationTimeUs);

    protected abstract boolean feedEncoder(long timeoutUs);
}
