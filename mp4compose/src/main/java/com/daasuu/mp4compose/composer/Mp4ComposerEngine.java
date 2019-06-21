package com.daasuu.mp4compose.composer;

import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.util.Log;

import com.daasuu.mp4compose.FillMode;
import com.daasuu.mp4compose.FillModeCustomItem;
import com.daasuu.mp4compose.Rotation;
import com.daasuu.mp4compose.compat.MediaCodecListCompat;
import com.daasuu.mp4compose.compat.MediaFormatCompat;
import com.daasuu.mp4compose.compat.SizeCompat;
import com.daasuu.mp4compose.filter.GlFilter;

import java.io.FileDescriptor;
import java.io.IOException;

// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/MediaTranscoderEngine.java

/**
 * Internal engine, do not use this directly.
 */
class Mp4ComposerEngine {
    private static final String TAG = "Mp4ComposerEngine";
    private static final double PROGRESS_UNKNOWN = -1.0;
    private static final long SLEEP_TO_WAIT_TRACK_TRANSCODERS = 10;
    private static final long PROGRESS_INTERVAL_STEPS = 10;
    private FileDescriptor inputFileDescriptor;
    private VideoComposer videoComposer;
    private IAudioComposer audioComposer;
    private MediaExtractor mediaExtractor;
    private MediaMuxer mediaMuxer;
    private ProgressCallback progressCallback;
    private long durationUs;
    private MediaMetadataRetriever mediaMetadataRetriever;


    void setDataSource(FileDescriptor fileDescriptor) {
        inputFileDescriptor = fileDescriptor;
    }

    void setProgressCallback(ProgressCallback progressCallback) {
        this.progressCallback = progressCallback;
    }


    void compose(
            final String destPath,
            final SizeCompat outputResolution,
            final GlFilter filter,
            final int bitrate,
            final boolean mute,
            final Rotation rotation,
            final SizeCompat inputResolution,
            final FillMode fillMode,
            final FillModeCustomItem fillModeCustomItem,
            final int timeScale,
            final boolean flipVertical,
            final boolean flipHorizontal,
            final long trimStartMs,
            final long trimEndMs
    ) throws IOException {


        try {
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(inputFileDescriptor);
            mediaMuxer = new MediaMuxer(destPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(inputFileDescriptor);
            try {
                durationUs = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
            } catch (NumberFormatException e) {
                durationUs = -1;
            }
            Log.d(TAG, "Duration (us): " + durationUs);

            MuxRender muxRender = new MuxRender(mediaMuxer);

            // identify track indices
            MediaFormat format = mediaExtractor.getTrackFormat(0);
            String mime = format.getString(MediaFormat.KEY_MIME);

            final int videoTrackIndex;
            final int audioTrackIndex;

            if (mime.startsWith(MediaFormatCompat.VIDEO_PREFIX)) {
                videoTrackIndex = 0;
                audioTrackIndex = 1;
            } else {
                videoTrackIndex = 1;
                audioTrackIndex = 0;
            }

            final MediaFormat desiredVideoOutputFormat = mediaExtractor.getTrackFormat(videoTrackIndex);
            final MediaFormat actualVideoOutputFormat = correctOutputVideoFormatForAvailableEncoders(desiredVideoOutputFormat, bitrate, outputResolution);

            // setup video composer
            videoComposer = new VideoComposer(mediaExtractor, videoTrackIndex, actualVideoOutputFormat, muxRender, timeScale, trimStartMs, trimEndMs);
            videoComposer.setUp(filter, rotation, outputResolution, inputResolution, fillMode, fillModeCustomItem, flipVertical, flipHorizontal);
            mediaExtractor.selectTrack(videoTrackIndex);

            // setup audio if present and not muted
            if (mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null && !mute) {
                // has Audio video
                final MediaFormat desiredOutputFormat = mediaExtractor.getTrackFormat(audioTrackIndex);
                final MediaFormat actualOutputFormat = correctOutputAudioFormatForAvailableEncoders(desiredOutputFormat);

                if (timeScale < 2 && actualOutputFormat.equals(desiredOutputFormat)) {
                    audioComposer = new AudioComposer(mediaExtractor, audioTrackIndex, muxRender, trimStartMs, trimEndMs);
                } else {
                    audioComposer = new RemixAudioComposer(mediaExtractor, audioTrackIndex, actualOutputFormat, muxRender, timeScale, trimStartMs, trimEndMs);
                }

                audioComposer.setup();

                mediaExtractor.selectTrack(audioTrackIndex);

                runPipelines();
            } else {
                // no audio video
                runPipelinesNoAudio();
            }


            mediaMuxer.stop();
        } finally {
            try {
                if (videoComposer != null) {
                    videoComposer.release();
                    videoComposer = null;
                }
                if (audioComposer != null) {
                    audioComposer.release();
                    audioComposer = null;
                }
                if (mediaExtractor != null) {
                    mediaExtractor.release();
                    mediaExtractor = null;
                }
            } catch (RuntimeException e) {
                // Too fatal to make alive the app, because it may leak native resources.
                //noinspection ThrowFromFinallyBlock
                throw new Error("Could not shutdown mediaExtractor, codecs and mediaMuxer pipeline.", e);
            }
            try {
                if (mediaMuxer != null) {
                    mediaMuxer.release();
                    mediaMuxer = null;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to release mediaMuxer.", e);
            }
            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever.release();
                    mediaMetadataRetriever = null;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to release mediaMetadataRetriever.", e);
            }
        }


    }

    private static MediaFormat correctOutputVideoFormatForAvailableEncoders(final MediaFormat desiredOutputFormat, final int bitrate, final SizeCompat outputResolution) {
        final MediaCodecListCompat mediaCodecList = new MediaCodecListCompat();
        final String encoderForOutputFormat = mediaCodecList.findEncoderForFormat(desiredOutputFormat);
        final MediaFormat outputFormat;

        Log.d(TAG, "Desired video format: " + desiredOutputFormat);

        if (encoderForOutputFormat != null && isSupportedByMpeg4(desiredOutputFormat)) {
            // If we found an encoder, then we can encode to this format.
            outputFormat = MediaFormat.createVideoFormat(desiredOutputFormat.getString(MediaFormat.KEY_MIME), outputResolution.getWidth(), outputResolution.getHeight());
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            // Required but ignored by the encoder
            outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        } else {
            // Otherwise, fall back to a format that should be supported, AVC.
            outputFormat = MediaFormat.createVideoFormat(MediaFormatCompat.MIMETYPE_VIDEO_AVC, outputResolution.getWidth(), outputResolution.getHeight());

            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            // Required but ignored by the encoder
            outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        }

        Log.d(TAG, "Actual video format: " + outputFormat);
        return outputFormat;
    }

    private static MediaFormat correctOutputAudioFormatForAvailableEncoders(final MediaFormat desiredOutputFormat) {
        final MediaCodecListCompat mediaCodecList = new MediaCodecListCompat();
        final String encoderForOutputFormat = mediaCodecList.findEncoderForFormat(desiredOutputFormat);
        final MediaFormat outputFormat;

        Log.d(TAG, "Desired audio format: " + desiredOutputFormat);

        if (encoderForOutputFormat != null && isSupportedByMpeg4(desiredOutputFormat)) {
            // If we found an encoder, then we can encode to this format.
            outputFormat = desiredOutputFormat;
        } else {
            // Otherwise, fall back to a format that should be supported, AAC.
            outputFormat = new MediaFormat();
            outputFormat.setString(MediaFormat.KEY_MIME, MediaFormatCompat.MIMETYPE_AUDIO_AAC);
            outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectELD);
            outputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, desiredOutputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
            outputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, desiredOutputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
        }

        Log.d(TAG, "Actual audio format: " + outputFormat);

        return outputFormat;
    }

    private static boolean isSupportedByMpeg4(final MediaFormat mediaFormat) {
        switch (mediaFormat.getString(MediaFormat.KEY_MIME)) {
            case MediaFormatCompat.MIMETYPE_VIDEO_AVC:
            case MediaFormatCompat.MIMETYPE_VIDEO_HEVC:
            case MediaFormatCompat.MIMETYPE_VIDEO_MPEG4:
            case MediaFormatCompat.MIMETYPE_VIDEO_MPEG2:
            // Supported, but worse than MPEG4 so we'll fall back.
            // case MediaFormatCompat.MIMETYPE_VIDEO_H263:
                return true;
            case MediaFormatCompat.MIMETYPE_AUDIO_AAC:
            case MediaFormatCompat.MIMETYPE_AUDIO_VORBIS:
            case MediaFormatCompat.MIMETYPE_AUDIO_MPEG:
            case MediaFormatCompat.MIMETYPE_AUDIO_AC3:
                return true;
            default:
                return false;
        }
    }

    private void runPipelines() {
        long loopCount = 0;
        if (durationUs <= 0) {
            if (progressCallback != null) {
                progressCallback.onProgress(PROGRESS_UNKNOWN);
            }// unknown
        }
        while (!(videoComposer.isFinished() && audioComposer.isFinished())) {
            boolean stepped = videoComposer.stepPipeline()
                    || audioComposer.stepPipeline();
            loopCount++;
            if (durationUs > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0) {
                double videoProgress = videoComposer.isFinished() ? 1.0 : Math.min(1.0, (double) videoComposer.getWrittenPresentationTimeUs() / durationUs);
                double audioProgress = audioComposer.isFinished() ? 1.0 : Math.min(1.0, (double) audioComposer.getWrittenPresentationTimeUs() / durationUs);
                double progress = (videoProgress + audioProgress) / 2.0;
                if (progressCallback != null) {
                    progressCallback.onProgress(progress);
                }
            }
            if (!stepped) {
                try {
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS);
                } catch (InterruptedException e) {
                    // nothing to do
                }
            }
        }
    }

    private void runPipelinesNoAudio() {
        long loopCount = 0;
        if (durationUs <= 0) {
            if (progressCallback != null) {
                progressCallback.onProgress(PROGRESS_UNKNOWN);
            } // unknown
        }
        while (!videoComposer.isFinished()) {
            boolean stepped = videoComposer.stepPipeline();
            loopCount++;
            if (durationUs > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0) {
                double videoProgress = videoComposer.isFinished() ? 1.0 : Math.min(1.0, (double) videoComposer.getWrittenPresentationTimeUs() / durationUs);
                if (progressCallback != null) {
                    progressCallback.onProgress(videoProgress);
                }
            }
            if (!stepped) {
                try {
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS);
                } catch (InterruptedException e) {
                    // nothing to do
                }
            }
        }


    }


    interface ProgressCallback {
        /**
         * Called to notify progress. Same thread which initiated transcode is used.
         *
         * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
         */
        void onProgress(double progress);
    }
}
