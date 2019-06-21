package com.daasuu.mp4compose.logger;

import android.util.Log;

/**
 * The default implementation of the {@link Logger} for Android.
 */
public class AndroidLogger implements Logger{

    @Override
    public void debug(String tag, String message) {
        Log.d(tag, message);
    }

    @Override
    public void error(String tag, String message, Throwable error) {
        Log.e(tag, message, error);
    }

    @Override
    public void warning(String tag, String message) {
        Log.w(tag, message);
    }

}
