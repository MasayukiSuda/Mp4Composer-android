package com.daasuu.mp4compose.composer;

/**
 * Created by sudamasayuki2 on 2018/02/24.
 */

interface IAudioComposer {

    void setup();

    boolean stepPipeline();

    long getWrittenPresentationTimeUs();

    boolean isFinished();

    void release();
}
