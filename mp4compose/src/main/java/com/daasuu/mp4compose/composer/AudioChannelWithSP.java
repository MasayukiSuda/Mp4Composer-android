package com.daasuu.mp4compose.composer;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Created by TAPOS DATTA on 22,May,2020
 */

public class AudioChannelWithSP extends BaseAudioChannel{

    private static final String TAG = "AUDIO_CHANNEL_WITH_SONIC";

    private SonicAudioProcessor stream = null;  // SonicAudioProcessor can deal with stereo Audio
    private float timeScale = 1f;
    boolean isEOF = false;
    private int BUFFER_CAPACITY = 2048; // in ShortBuffer size
    private long totalDataAdded = 0;
    private int pendingDecoderOutputBuffIndx = -1;
    private ByteBuffer tempInputBuffer = null;
    private boolean isPendingFeeding = true;
    private boolean isAffectInPitch; // if true the scale will impact in speed with pitch

    AudioChannelWithSP(MediaCodec decoder, MediaCodec encoder, MediaFormat encodeFormat,float timeScale, boolean isPitchChanged) {
        super(decoder, encoder, encodeFormat);
        this.isAffectInPitch = isPitchChanged;
        this.timeScale = timeScale;
    }

    @Override
    public void setActualDecodedFormat(MediaFormat decodedFormat) {
        super.setActualDecodedFormat(decodedFormat);

        if (inputChannelCount > 2) {
            throw new UnsupportedOperationException("Input channel count (" + inputChannelCount + ") not supported.");
        }
        stream = new SonicAudioProcessor(inputSampleRate, outputChannelCount);
        isEOF = false;
        totalDataAdded = 0;
        isPendingFeeding = true;
        tempInputBuffer = ByteBuffer.allocateDirect(BUFFER_CAPACITY * 16).order(ByteOrder.nativeOrder());

        if(isAffectInPitch){
            stream.setRate(timeScale);
        }else {
            stream.setSpeed(timeScale);
        }
    }

    @Override
    protected long sampleCountToDurationUs(long sampleCount, int sampleRate, int channelCount) {
        //considered short buffer as data
        return (long) ((MICROSECS_PER_SEC * (sampleCount * 1f)/(sampleRate * 1f * channelCount)));
    }

    @Override
    public void drainDecoderBufferAndQueue(int bufferIndex, long presentationTimeUs) {

        if (actualDecodedFormat == null) {
            throw new RuntimeException("Buffer received before format!");
        }

        final ByteBuffer data =
                bufferIndex == BUFFER_INDEX_END_OF_STREAM ?
                        null : decoder.getOutputBuffer(bufferIndex);

        if (data != null) {
            writeToSonicSteam(data.asShortBuffer());
            pendingDecoderOutputBuffIndx = bufferIndex;
            isEOF = false;
            decoder.releaseOutputBuffer(bufferIndex, false);
        } else {
            stream.flushStream();
            isEOF = true;
        }
    }

    @Override
    public boolean feedEncoder(long timeoutUs) {

        if (stream == null || !isPendingFeeding || (!isEOF && stream.samplesAvailable() == 0)) {
            //no data available

            updatePendingDecoderStatus();

            return false;
        } else if (!isEOF && timeScale < 1f && stream.samplesAvailable() > 0 && (stream.samplesAvailable() * outputChannelCount) < BUFFER_CAPACITY) {
            //few data remaining in stream wait for next stream data
            updatePendingDecoderStatus();

            return false;
        }

        final int encoderInBuffIndex = encoder.dequeueInputBuffer(timeoutUs);

        if (encoderInBuffIndex < 0) {
            // Encoder is full - Bail out
            return false;
        }

        boolean status = false;
        if (timeScale < 1f) {
            status = slowTimeBufferProcess(encoderInBuffIndex);
        } else {
            status = FastOrNormalTimeBufferProcess(encoderInBuffIndex);
        }

        return status;
    }

    private void updatePendingDecoderStatus() {

        if (pendingDecoderOutputBuffIndx != -1) {
            pendingDecoderOutputBuffIndx = -1;
        }
    }

    private boolean FastOrNormalTimeBufferProcess(int encoderInBuffIndex) {

        int samplesNum = stream.samplesAvailable();

        boolean status = false;

        int rawDataLen = samplesNum * outputChannelCount;

        if(rawDataLen >= BUFFER_CAPACITY){

            return readStreamDataAndQueueToEncoder(BUFFER_CAPACITY, encoderInBuffIndex);
        }

        else if (rawDataLen > 0 && rawDataLen < BUFFER_CAPACITY) {

            return readStreamDataAndQueueToEncoder(rawDataLen, encoderInBuffIndex);

        } else if (isEOF && samplesNum == 0) {

            return finalizeEncoderQueue(encoderInBuffIndex);

        } else {

            return status;
        }
    }

    private boolean slowTimeBufferProcess(final int encoderInBuffIndex) {

        int samplesNum = stream.samplesAvailable();

        boolean status = false;

        int rawDataLen = samplesNum * outputChannelCount;

        if (rawDataLen >= BUFFER_CAPACITY) {

            return readStreamDataAndQueueToEncoder(BUFFER_CAPACITY, encoderInBuffIndex);

        } else if (isEOF && (rawDataLen > 0 && rawDataLen < BUFFER_CAPACITY)) {

            return readStreamDataAndQueueToEncoder(rawDataLen, encoderInBuffIndex);

        } else if (isEOF && rawDataLen == 0) {

            return finalizeEncoderQueue(encoderInBuffIndex);

        } else {

            return status;
        }
    }

    private boolean finalizeEncoderQueue(final int encoderInBuffIndex) {

        isPendingFeeding = false;
        return queueInputBufferInEncoder(null, encoderInBuffIndex);
    }

    private boolean readStreamDataAndQueueToEncoder(final int capacity, final int encoderInBuffIndex) {

        short[] rawData = new short[capacity];
        stream.readShortFromStream(rawData, (capacity / outputChannelCount));
        return queueInputBufferInEncoder(rawData, encoderInBuffIndex);
    }

    private boolean queueInputBufferInEncoder(final short[] rawData, final int encoderInBuffIndex) {

        final ShortBuffer outBuffer = encoder.getInputBuffer(encoderInBuffIndex).asShortBuffer();

        outBuffer.clear();
        if (rawData != null) {

            outBuffer.put(rawData);
            totalDataAdded += rawData.length;

            long presentationTimeUs = sampleCountToDurationUs(totalDataAdded, inputSampleRate, outputChannelCount);

            encoder.queueInputBuffer(encoderInBuffIndex, 0, rawData.length * BYTES_PER_SHORT,
                    presentationTimeUs, 0);
            return false;
        } else {
            encoder.queueInputBuffer(encoderInBuffIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return false;
        }
    }

    private void writeToSonicSteam(final ShortBuffer data) {

        short[] temBuff = new short[data.capacity()];
        data.get(temBuff);
        data.rewind();
        stream.writeShortToStream(temBuff, temBuff.length / outputChannelCount);
    }

    public boolean isAnyPendingBuffIndex() {
        // allow to decoder to send data into stream (e.i. sonicprocessor)
        if (pendingDecoderOutputBuffIndx != -1) {
            return true;
        } else {
            return false;
        }
    }
}
