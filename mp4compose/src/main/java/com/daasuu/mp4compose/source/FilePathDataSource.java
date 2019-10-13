package com.daasuu.mp4compose.source;

import androidx.annotation.NonNull;

import com.daasuu.mp4compose.logger.Logger;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FilePathDataSource implements DataSource {

    private final static String TAG = FilePathDataSource.class.getSimpleName();

    private FileDescriptor fileDescriptor;

    public FilePathDataSource(@NonNull String filePath, @NonNull Logger logger, @NonNull Listener listener) {

        final File srcFile = new File(filePath);
        final FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(srcFile);
        } catch (FileNotFoundException e) {
            logger.error(TAG, "Unable to find file", e);
            listener.onError(e);
            return;
        }

        try {
            fileDescriptor = fileInputStream.getFD();
        } catch (IOException e) {
            logger.error(TAG, "Unable to read input file", e);
            listener.onError(e);
        }
    }

    @NonNull
    @Override
    public FileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }
}
