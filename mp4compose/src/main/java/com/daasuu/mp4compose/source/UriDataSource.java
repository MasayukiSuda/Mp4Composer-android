package com.daasuu.mp4compose.source;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import com.daasuu.mp4compose.logger.Logger;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;

public class UriDataSource implements DataSource {

    private final static String TAG = UriDataSource.class.getSimpleName();

    private FileDescriptor fileDescriptor;

    public UriDataSource(@NonNull Uri uri, @NonNull Context context, @NonNull Logger logger, @NonNull Listener listener) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            logger.error(TAG, "Unable to find file", e);
            listener.onError(e);
            return;
        }
        fileDescriptor = parcelFileDescriptor.getFileDescriptor();
    }

    @NonNull
    @Override
    public FileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }
}
