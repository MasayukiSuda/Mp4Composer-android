package com.daasuu.sample.video;

import java.util.List;

/**
 * Created by sudamasayuki on 2017/11/22.
 */

public interface VideoLoadListener {

    void onVideoLoaded(List<VideoItem> videoItems);

    void onFailed(Exception e);
}
