package com.daasuu.mp4compose.source;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;

public class FileDescriptorDataSource implements DataSource {

    private final FileDescriptor fileDescriptor;

    public FileDescriptorDataSource(FileDescriptor fileDescriptor) {
        this.fileDescriptor = fileDescriptor;
    }

    @NonNull
    @Override
    public FileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }
}
