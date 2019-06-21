package com.daasuu.mp4compose.composer;

import android.media.MediaMetadataRetriever;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.daasuu.mp4compose.FillMode;
import com.daasuu.mp4compose.FillModeCustomItem;
import com.daasuu.mp4compose.Rotation;
import com.daasuu.mp4compose.compat.SizeCompat;
import com.daasuu.mp4compose.filter.GlFilter;
import com.daasuu.mp4compose.logger.AndroidLogger;
import com.daasuu.mp4compose.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sudamasayuki on 2017/11/15.
 */

public class Mp4Composer {

    private final static String TAG = Mp4Composer.class.getSimpleName();

    private final String srcPath;
    private final String destPath;
    private GlFilter filter;
    private SizeCompat outputResolution;
    private int bitrate = -1;
    private boolean mute = false;
    private Rotation rotation = Rotation.NORMAL;
    private Listener listener;
    private FillMode fillMode = FillMode.PRESERVE_ASPECT_FIT;
    private FillModeCustomItem fillModeCustomItem;
    private int timeScale = 1;
    private boolean flipVertical = false;
    private boolean flipHorizontal = false;
    private long trimStartMs = 0;
    private long trimEndMs = -1;

    private ExecutorService executorService;

    private Logger logger = new AndroidLogger();


    public Mp4Composer(final String srcPath, final String destPath) {
        this.srcPath = srcPath;
        this.destPath = destPath;
    }

    public Mp4Composer filter(GlFilter filter) {
        this.filter = filter;
        return this;
    }

    public Mp4Composer size(int width, int height) {
        this.outputResolution = new SizeCompat(width, height);
        return this;
    }

    public Mp4Composer videoBitrate(int bitrate) {
        this.bitrate = bitrate;
        return this;
    }

    public Mp4Composer mute(boolean mute) {
        this.mute = mute;
        return this;
    }

    public Mp4Composer flipVertical(boolean flipVertical) {
        this.flipVertical = flipVertical;
        return this;
    }

    public Mp4Composer flipHorizontal(boolean flipHorizontal) {
        this.flipHorizontal = flipHorizontal;
        return this;
    }

    public Mp4Composer rotation(Rotation rotation) {
        this.rotation = rotation;
        return this;
    }

    public Mp4Composer fillMode(FillMode fillMode) {
        this.fillMode = fillMode;
        return this;
    }

    public Mp4Composer customFillMode(FillModeCustomItem fillModeCustomItem) {
        this.fillModeCustomItem = fillModeCustomItem;
        this.fillMode = FillMode.CUSTOM;
        return this;
    }


    public Mp4Composer listener(Listener listener) {
        this.listener = listener;
        return this;
    }

    public Mp4Composer timeScale(final int timeScale) {
        this.timeScale = timeScale;
        return this;
    }

    /**
     * Set the {@link Logger} that should be used. Defaults to {@link AndroidLogger} if none is set.
     *
     * @param logger The logger that should be used to log.
     * @return The composer instance.
     */
    public Mp4Composer logger(@NonNull final Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Trim the video to the provided times. By default the video will not be trimmed.
     *
     * @param trimStartMs The start time of the trim in milliseconds.
     * @param trimEndMs   The end time of the trim in milliseconds, -1 for no end.
     * @return The composer instance.
     */
    public Mp4Composer trim(final long trimStartMs, final long trimEndMs) {
        this.trimStartMs = trimStartMs;
        this.trimEndMs = trimEndMs;
        return this;
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
        return executorService;
    }


    public Mp4Composer start() {
        getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                Mp4ComposerEngine engine = new Mp4ComposerEngine(logger);

                engine.setProgressCallback(new Mp4ComposerEngine.ProgressCallback() {
                    @Override
                    public void onProgress(final double progress) {
                        if (listener != null) {
                            listener.onProgress(progress);
                        }
                    }
                });

                final File srcFile = new File(srcPath);
                final FileInputStream fileInputStream;
                try {
                    fileInputStream = new FileInputStream(srcFile);
                } catch (FileNotFoundException e) {
                    logger.error(TAG, "Unable to find input file", e);
                    if (listener != null) {
                        listener.onFailed(e);
                    }
                    executorService.shutdown();
                    return;
                }

                try {
                    engine.setDataSource(fileInputStream.getFD());
                } catch (IOException e) {
                    logger.error(TAG, "Unable to read input file", e);
                    if (listener != null) {
                        listener.onFailed(e);
                    }
                    executorService.shutdown();
                    return;
                }

                final Integer videoRotate = getVideoRotation(srcPath);
                final SizeCompat srcVideoResolution = getVideoResolution(srcPath);

                if (srcVideoResolution == null || videoRotate == null) {
                    if (listener != null) {
                        listener.onFailed(new UnsupportedOperationException("File type unsupported, path: " + srcPath));
                    }
                    executorService.shutdown();
                    return;
                }

                if (filter == null) {
                    filter = new GlFilter();
                }

                if (fillMode == null) {
                    fillMode = FillMode.PRESERVE_ASPECT_FIT;
                }

                if (fillModeCustomItem != null) {
                    fillMode = FillMode.CUSTOM;
                }

                if (outputResolution == null) {
                    if (fillMode == FillMode.CUSTOM) {
                        outputResolution = srcVideoResolution;
                    } else {
                        Rotation rotate = Rotation.fromInt(rotation.getRotation() + videoRotate);
                        if (rotate == Rotation.ROTATION_90 || rotate == Rotation.ROTATION_270) {
                            outputResolution = new SizeCompat(srcVideoResolution.getHeight(), srcVideoResolution.getWidth());
                        } else {
                            outputResolution = srcVideoResolution;
                        }
                    }
                }
//                if (filter instanceof IResolutionFilter) {
//                    ((IResolutionFilter) filter).setResolution(outputResolution);
//                }

                if (timeScale < 2) {
                    timeScale = 1;
                }

                logger.debug(TAG, "rotation = " + (rotation.getRotation() + videoRotate));
                logger.debug(TAG, "inputResolution width = " + srcVideoResolution.getWidth() + " height = " + srcVideoResolution.getHeight());
                logger.debug(TAG, "outputResolution width = " + outputResolution.getWidth() + " height = " + outputResolution.getHeight());
                logger.debug(TAG, "fillMode = " + fillMode);

                try {
                    if (bitrate < 0) {
                        bitrate = calcBitRate(outputResolution.getWidth(), outputResolution.getHeight());
                    }
                    engine.compose(
                            destPath,
                            outputResolution,
                            filter,
                            bitrate,
                            mute,
                            Rotation.fromInt(rotation.getRotation() + videoRotate),
                            srcVideoResolution,
                            fillMode,
                            fillModeCustomItem,
                            timeScale,
                            flipVertical,
                            flipHorizontal,
                            trimStartMs,
                            trimEndMs
                    );

                } catch (Exception e) {
                    logger.error(TAG, "Unable to compose the engine", e);
                    if (listener != null) {
                        listener.onFailed(e);
                    }
                    executorService.shutdown();
                    return;
                }

                if (listener != null) {
                    listener.onCompleted();
                }
                executorService.shutdown();
            }
        });

        return this;
    }

    public void cancel() {
        getExecutorService().shutdownNow();
    }


    public interface Listener {
        /**
         * Called to notify progress.
         *
         * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
         */
        void onProgress(double progress);

        /**
         * Called when transcode completed.
         */
        void onCompleted();

        /**
         * Called when transcode canceled.
         */
        void onCanceled();


        void onFailed(Exception exception);
    }

    @Nullable
    private Integer getVideoRotation(String videoFilePath) {
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(videoFilePath);
            final String orientation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if (orientation == null){
                return null;
            }
            return Integer.valueOf(orientation);
        } catch (IllegalArgumentException e) {
            logger.error("MediaMetadataRetriever", "getVideoRotation IllegalArgumentException", e);
            return 0;
        } catch (RuntimeException e) {
            logger.error("MediaMetadataRetriever", "getVideoRotation RuntimeException", e);
            return 0;
        } catch (Exception e) {
            logger.error("MediaMetadataRetriever", "getVideoRotation Exception", e);
            return 0;
        } finally {
            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever.release();
                }
            } catch (RuntimeException e) {
                logger.error(TAG, "Failed to release mediaMetadataRetriever.", e);
            }
        }
    }

    private int calcBitRate(int width, int height) {
        final int bitrate = (int) (0.25 * 30 * width * height);
        logger.debug(TAG, "bitrate=" + bitrate);
        return bitrate;
    }

    /**
     * Extract the resolution of the video at the provided path, or null if the format is
     * unsupported.
     *
     * @param path The path of the video.
     */
    @Nullable
    private SizeCompat getVideoResolution(final String path) {
        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path);
            final String rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            final String rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (rawWidth == null || rawHeight == null) {
                return null;
            }
            final int width = Integer.valueOf(rawWidth);
            final int height = Integer.valueOf(rawHeight);

            return new SizeCompat(width, height);
        } finally {
            try {
                if (retriever != null) {
                    retriever.release();
                }
            } catch (RuntimeException e) {
                logger.error(TAG, "Failed to release mediaMetadataRetriever.", e);
            }
        }
    }

}
