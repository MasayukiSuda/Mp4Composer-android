package com.daasuu.mp4compose.composer;

import android.media.*;
import android.opengl.EGLContext;
import android.os.Build;
import android.util.Size;
import androidx.annotation.NonNull;
import com.daasuu.mp4compose.FillMode;
import com.daasuu.mp4compose.FillModeCustomItem;
import com.daasuu.mp4compose.Rotation;
import com.daasuu.mp4compose.VideoFormatMimeType;
import com.daasuu.mp4compose.filter.GlFilter;
import com.daasuu.mp4compose.logger.Logger;
import com.daasuu.mp4compose.source.DataSource;

import java.io.FileDescriptor;
import java.io.IOException;


// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/MediaTranscoderEngine.java

/**
 * Internal engine, do not use this directly.
 */
class Mp4ComposerEngine {

    private static final String TAG = "Mp4ComposerEngine";
    private static final String AUDIO_PREFIX = "audio/";
    private static final String VIDEO_PREFIX = "video/";
    private static final double PROGRESS_UNKNOWN = -1.0;
    private static final long SLEEP_TO_WAIT_TRACK_TRANSCODERS = 10;
    private static final long PROGRESS_INTERVAL_STEPS = 10;
    private VideoComposer videoComposer;
    private IAudioComposer audioComposer;
    private MediaExtractor mediaExtractor;
    private MediaMuxer mediaMuxer;
    private ProgressCallback progressCallback;
    private long durationUs;
    private MediaMetadataRetriever mediaMetadataRetriever;
    private volatile boolean canceled;
    private final Logger logger;
    private long trimStartMs;
    private long trimEndMs;

    Mp4ComposerEngine(@NonNull final Logger logger) {
        this.logger = logger;
    }

    void setProgressCallback(ProgressCallback progressCallback) {
        this.progressCallback = progressCallback;
    }

    void compose(
            final DataSource srcDataSource,
            final String destSrc,
            final FileDescriptor destFileDescriptor,
            final Size outputResolution,
            final GlFilter filter,
            final int bitrate,
            final boolean mute,
            final Rotation rotation,
            final Size inputResolution,
            final FillMode fillMode,
            final FillModeCustomItem fillModeCustomItem,
            final float timeScale,
            final boolean isPitchChanged,
            final boolean flipVertical,
            final boolean flipHorizontal,
            final long trimStartMs,
            final long trimEndMs,
            final VideoFormatMimeType videoFormatMimeType,
            final EGLContext shareContext
    ) throws IOException {

        try {
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(srcDataSource.getFileDescriptor());
            if (Build.VERSION.SDK_INT >= 26 && destSrc == null) {
                mediaMuxer = new MediaMuxer(destFileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } else {
                mediaMuxer = new MediaMuxer(destSrc, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            }
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(srcDataSource.getFileDescriptor());

            this.trimStartMs = trimStartMs;
            this.trimEndMs = trimEndMs;
            if (trimEndMs != -1) {
                durationUs = (trimEndMs - trimStartMs) * 1000;
            } else {
                try {
                    durationUs = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
                } catch (NumberFormatException e) {
                    durationUs = -1;
                }
            }

            logger.debug(TAG, "Duration (us): " + durationUs);

            MuxRender muxRender = new MuxRender(mediaMuxer, logger);

            // identify track indices
            int videoTrackIndex = -1;
            int audioTrackIndex = -1;
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType == null) continue;
                if (mimeType.startsWith(VIDEO_PREFIX)) {
                    videoTrackIndex = i;
                } else if (mimeType.startsWith(AUDIO_PREFIX)) {
                    audioTrackIndex = i;
                }
            }

            final MediaFormat actualVideoOutputFormat = createVideoOutputFormatWithAvailableEncoders(videoFormatMimeType, bitrate, outputResolution);
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                // Only LOLLIPOP sets KEY_FRAME_RATE here.
                actualVideoOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            }

            // setup video composer
            videoComposer = new VideoComposer(mediaExtractor, videoTrackIndex, actualVideoOutputFormat, muxRender, timeScale, trimStartMs, trimEndMs, logger);
            videoComposer.setUp(filter, rotation, outputResolution, inputResolution, fillMode, fillModeCustomItem, flipVertical, flipHorizontal, shareContext);
            mediaExtractor.selectTrack(videoTrackIndex);

            // setup audio if present and not muted
            if (audioTrackIndex >= 0 && mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null && !mute) {
                // has Audio video
                final MediaFormat inputMediaFormat = mediaExtractor.getTrackFormat(audioTrackIndex);
                final MediaFormat outputMediaFormat = createAudioOutputFormat(inputMediaFormat);

                if( timeScale >= 0.99 && timeScale <= 1.01  && outputMediaFormat.equals(inputMediaFormat)) {
                    audioComposer = new AudioComposer(mediaExtractor, audioTrackIndex, muxRender, trimStartMs, trimEndMs, logger);
                } else {
                    audioComposer = new RemixAudioComposer(mediaExtractor, audioTrackIndex, outputMediaFormat, muxRender, timeScale, isPitchChanged , trimStartMs, trimEndMs);
                }

                audioComposer.setup();
                mediaExtractor.selectTrack(audioTrackIndex);
                mediaExtractor.seekTo(trimStartMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                runPipelines();
            } else {
                mediaExtractor.seekTo(trimStartMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
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
                logger.error(TAG, "Could not shutdown mediaExtractor, codecs and mediaMuxer pipeline.", e);
            }
            try {
                if (mediaMuxer != null) {
                    mediaMuxer.release();
                    mediaMuxer = null;
                }
            } catch (RuntimeException e) {
                logger.error(TAG, "Failed to release mediaMuxer.", e);
            }
            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever.release();
                    mediaMetadataRetriever = null;
                }
            } catch (RuntimeException e) {
                logger.error(TAG, "Failed to release mediaMetadataRetriever.", e);
            }
        }
    }

    void cancel() {
        canceled = true;
    }

    boolean isCanceled() {
        return canceled;
    }

    @NonNull
    private static MediaFormat createVideoOutputFormatWithAvailableEncoders(@NonNull final VideoFormatMimeType mimeType,
                                                                            final int bitrate,
                                                                            @NonNull final Size outputResolution) {
        final MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);

        if (mimeType != VideoFormatMimeType.AUTO) {
            final MediaFormat mediaFormat = createVideoFormat(mimeType.getFormat(), bitrate, outputResolution);
            if (mediaCodecList.findEncoderForFormat(mediaFormat) != null) {
                return mediaFormat;
            }
        }

        final MediaFormat hevcMediaFormat = createVideoFormat(VideoFormatMimeType.HEVC.getFormat(), bitrate, outputResolution);
        if (mediaCodecList.findEncoderForFormat(hevcMediaFormat) != null) {
            return hevcMediaFormat;
        }

        final MediaFormat avcMediaFormat = createVideoFormat(VideoFormatMimeType.AVC.getFormat(), bitrate, outputResolution);
        if (mediaCodecList.findEncoderForFormat(avcMediaFormat) != null) {
            return avcMediaFormat;
        }

        final MediaFormat mp4vesMediaFormat = createVideoFormat(VideoFormatMimeType.MPEG4.getFormat(), bitrate, outputResolution);
        if (mediaCodecList.findEncoderForFormat(mp4vesMediaFormat) != null) {
            return mp4vesMediaFormat;
        }

        return createVideoFormat(VideoFormatMimeType.H263.getFormat(), bitrate, outputResolution);
    }

    @NonNull
    private static MediaFormat createAudioOutputFormat(@NonNull final MediaFormat inputFormat) {
        if (MediaFormat.MIMETYPE_AUDIO_AAC.equals(inputFormat.getString(MediaFormat.KEY_MIME))) {
            return inputFormat;
        } else {
            final MediaFormat outputFormat = new MediaFormat();
            outputFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
            outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectELD);
            outputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE,
                    inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
            outputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT,
                    inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));

            return outputFormat;
        }
    }

    @NonNull
    private static MediaFormat createVideoFormat(@NonNull final String mimeType,
                                                 final int bitrate,
                                                 @NonNull final Size outputResolution) {
        final MediaFormat outputFormat =
                MediaFormat.createVideoFormat(mimeType,
                        outputResolution.getWidth(),
                        outputResolution.getHeight());

        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        // On Build.VERSION_CODES.LOLLIPOP, format must not contain a MediaFormat#KEY_FRAME_RATE.
        // https://developer.android.com/reference/android/media/MediaCodecInfo.CodecCapabilities.html#isFormatSupported(android.media.MediaFormat)
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.LOLLIPOP) {
            // Required but ignored by the encoder
            outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        }
        outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        return outputFormat;
    }


    private void runPipelines() {
        long loopCount = 0;
        if (durationUs <= 0) {
            if (progressCallback != null) {
                progressCallback.onProgress(PROGRESS_UNKNOWN);
            }// unknown
        }
        while (!canceled && !(videoComposer.isFinished() && audioComposer.isFinished())) {
            boolean stepped = videoComposer.stepPipeline()
                    || audioComposer.stepPipeline();
            loopCount++;
            if (durationUs > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0) {
                long writtenPresentationVideoTimeUs = videoComposer.getWrittenPresentationTimeUs();
                if (progressCallback != null) {
                    progressCallback.onCurrentWrittenVideoTime(writtenPresentationVideoTimeUs);
                }
                double videoProgress = videoComposer.isFinished() ? 1.0 : Math.min(1.0, (double) getWrittenPresentationTimeUs(writtenPresentationVideoTimeUs) / durationUs);
                double audioProgress = audioComposer.isFinished() ? 1.0 : Math.min(1.0, (double) getWrittenPresentationTimeUs(audioComposer.getWrittenPresentationTimeUs()) / durationUs);
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

    private long getWrittenPresentationTimeUs(long time) {
        return Math.max(0, time - trimStartMs * 1000);
    }

    private void runPipelinesNoAudio() {
        long loopCount = 0;
        if (durationUs <= 0) {
            if (progressCallback != null) {
                progressCallback.onProgress(PROGRESS_UNKNOWN);
            } // unknown
        }
        while (!canceled && !videoComposer.isFinished()) {
            boolean stepped = videoComposer.stepPipeline();
            loopCount++;
            if (durationUs > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0) {
                long writtenPresentationVideoTimeUs = videoComposer.getWrittenPresentationTimeUs();
                if (progressCallback != null) {
                    progressCallback.onCurrentWrittenVideoTime(writtenPresentationVideoTimeUs);
                }
                double videoProgress = videoComposer.isFinished() ? 1.0 : Math.min(1.0, (double) getWrittenPresentationTimeUs(writtenPresentationVideoTimeUs) / durationUs);
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

        /**
         * Called to propagate current time at video transcoding process
         *
         * @param timeUs Current time, in Us
         */
        void onCurrentWrittenVideoTime(long timeUs);
    }
}
