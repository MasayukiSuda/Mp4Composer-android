package com.daasuu.sample.video;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sudamasayuki on 2017/11/22.
 */

public class VideoLoader {

    private final static String TAG = "VideoLoader";

    private final Context context;
    private ExecutorService executorService;

    public VideoLoader(Context context) {
        this.context = context;
    }

    public void loadDeviceVideos(final VideoLoadListener listener) {
        getExecutorService().execute(new VideoLoadRunnable(listener, context));
    }

    public void abortLoadVideos() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
        return executorService;
    }


    private static class VideoLoadRunnable implements Runnable {

        private final VideoLoadListener listener;
        private final Context context;
        private final Handler handler = new Handler(Looper.getMainLooper());

        private final String[] projection = new String[]{
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
        };


        public VideoLoadRunnable(VideoLoadListener listener, Context context) {
            this.listener = listener;
            this.context = context;
        }

        @Override
        public void run() {
            Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
                    null, null, MediaStore.Video.Media.DATE_MODIFIED);

            if (cursor == null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onFailed(new NullPointerException());
                    }
                });
                return;
            }

            final List<VideoItem> temp = new ArrayList<>(cursor.getCount());

            if (cursor.moveToLast()) {
                do {
                    String path = cursor.getString(cursor.getColumnIndex(projection[0]));
                    if (path == null) continue;
                    if (!path.endsWith(".mp4") && !path.endsWith(".MOV") && !path.endsWith(".mov")) {
                        continue;
                    }
                    Log.d(TAG, "pick video from device path = " + path);

                    String duration = cursor.getString(cursor.getColumnIndex(projection[1]));
                    if (duration == null) duration = "0";
                    Log.d(TAG, "pick video from device duration = " + duration);

                    String width = cursor.getString(cursor.getColumnIndex(projection[2]));
                    if (width == null) width = "0";
                    Log.d(TAG, "pick video from device width = " + width);

                    String height = cursor.getString(cursor.getColumnIndex(projection[3]));
                    if (height == null) height = "0";
                    Log.d(TAG, "pick video from device height = " + height);

                    File file = new File(path);
                    if (file.exists()) {
                        temp.add(new VideoItem(
                                path,
                                Integer.valueOf(duration),
                                Integer.valueOf(width),
                                Integer.valueOf(height)
                        ));
                    }
                    file = null;

                } while (cursor.moveToPrevious());
            }
            cursor.close();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onVideoLoaded(temp);
                }
            });
        }
    }


}
