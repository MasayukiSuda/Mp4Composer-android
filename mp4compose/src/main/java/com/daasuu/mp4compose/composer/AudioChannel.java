package com.daasuu.mp4compose.composer;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/AudioChannel.java

/**
 * Created by sudamasayuki2 on 2018/02/22.
 */

class AudioChannel extends BaseAudioChannel {

    AudioChannel(final MediaCodec decoder,
                 final MediaCodec encoder, final MediaFormat encodeFormat) {
        super(decoder,encoder,encodeFormat);
    }

    @Override
    public void setActualDecodedFormat(MediaFormat decodedFormat) {
        super.setActualDecodedFormat(decodedFormat);

        if (inputChannelCount != 1 && inputChannelCount != 2) {
            throw new UnsupportedOperationException("Input channel count (" + inputChannelCount + ") not supported.");
        }
    }

    @Override
    public void drainDecoderBufferAndQueue(int bufferIndex, long presentationTimeUs) {

        if (actualDecodedFormat == null) {
            throw new RuntimeException("Buffer received before format!");
        }

        final ByteBuffer data =
                bufferIndex == BUFFER_INDEX_END_OF_STREAM ?
                        null : decoder.getOutputBuffer(bufferIndex);

        AudioBuffer buffer = emptyBuffers.poll();
        if (buffer == null) {
            buffer = new AudioBuffer();
        }

        buffer.bufferIndex = bufferIndex;
        buffer.presentationTimeUs = presentationTimeUs;
        buffer.data = data == null ? null : data.asShortBuffer();

        if (overflowBuffer.data == null) {
            overflowBuffer.data = ByteBuffer
                    .allocateDirect(data.capacity())
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer();
            overflowBuffer.data.clear().flip();
        }

        filledBuffers.add(buffer);
    }

    @Override
    public boolean feedEncoder(long timeoutUs) {
        final boolean hasOverflow = overflowBuffer.data != null && overflowBuffer.data.hasRemaining();
        if (filledBuffers.isEmpty() && !hasOverflow) {
            // No audio data - Bail out
            return false;
        }

        final int encoderInBuffIndex = encoder.dequeueInputBuffer(timeoutUs);
        if (encoderInBuffIndex < 0) {
            // Encoder is full - Bail out
            return false;
        }

        // Drain overflow first
        final ShortBuffer outBuffer = encoder.getInputBuffer(encoderInBuffIndex).asShortBuffer();
        if (hasOverflow) {
            final long presentationTimeUs = drainOverflow(outBuffer);
            encoder.queueInputBuffer(encoderInBuffIndex,
                    0, outBuffer.position() * BYTES_PER_SHORT,
                    presentationTimeUs, 0);
            return true;
        }

        final AudioBuffer inBuffer = filledBuffers.poll();
        if (inBuffer.bufferIndex == BUFFER_INDEX_END_OF_STREAM) {
            encoder.queueInputBuffer(encoderInBuffIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return false;
        }

        final long presentationTimeUs = remixAndMaybeFillOverflow(inBuffer, outBuffer);
        encoder.queueInputBuffer(encoderInBuffIndex,
                0, outBuffer.position() * BYTES_PER_SHORT,
                presentationTimeUs, 0);
        if (inBuffer != null) {
            decoder.releaseOutputBuffer(inBuffer.bufferIndex, false);
            emptyBuffers.add(inBuffer);
        }

        return true;
    }

    @Override
    protected long sampleCountToDurationUs(long sampleCount, int sampleRate, int channelCount) {
        return (sampleCount / (sampleRate * MICROSECS_PER_SEC)) / channelCount;
    }

    private long drainOverflow(final ShortBuffer outBuff) {
        final ShortBuffer overflowBuff = overflowBuffer.data;
        final int overflowLimit = overflowBuff.limit();
        final int overflowSize = overflowBuff.remaining();

        final long beginPresentationTimeUs = overflowBuffer.presentationTimeUs +
                sampleCountToDurationUs(overflowBuff.position(), inputSampleRate, outputChannelCount);

        outBuff.clear();
        // Limit overflowBuff to outBuff's capacity
        overflowBuff.limit(outBuff.capacity());
        // Load overflowBuff onto outBuff
        outBuff.put(overflowBuff);

        if (overflowSize >= outBuff.capacity()) {
            // Overflow fully consumed - Reset
            overflowBuff.clear().limit(0);
        } else {
            // Only partially consumed - Keep position & restore previous limit
            overflowBuff.limit(overflowLimit);
        }

        return beginPresentationTimeUs;
    }


    private long remixAndMaybeFillOverflow(final AudioBuffer input,
                                           final ShortBuffer outBuff) {
        final ShortBuffer inBuff = input.data;
        final ShortBuffer overflowBuff = overflowBuffer.data;

        outBuff.clear();

        // Reset position to 0, and set limit to capacity (Since MediaCodec doesn't do that for us)
        inBuff.clear();

        if (inBuff.remaining() > outBuff.remaining()) {
            // Overflow
            // Limit inBuff to outBuff's capacity
            inBuff.limit(outBuff.capacity());
            outBuff.put(inBuff);

            // Reset limit to its own capacity & Keep position
            inBuff.limit(inBuff.capacity());

            // Remix the rest onto overflowBuffer
            // NOTE: We should only reach this point when overflow buffer is empty
            final long consumedDurationUs =
                    sampleCountToDurationUs(inBuff.position(), inputSampleRate, inputChannelCount);
            overflowBuff.put(inBuff);

            // Seal off overflowBuff & mark limit
            overflowBuff.flip();
            overflowBuffer.presentationTimeUs = input.presentationTimeUs + consumedDurationUs;
        } else {
            // No overflow
            outBuff.put(inBuff);
        }
        return input.presentationTimeUs;
    }

}

