package com.daasuu.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.daasuu.sample.video.VideoItem;
import com.daasuu.sample.video.VideoListAdapter;
import com.daasuu.sample.video.VideoLoadListener;
import com.daasuu.sample.video.VideoLoader;

import java.util.List;

public class MovieListActivity extends AppCompatActivity {

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, MovieListActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoLoader videoLoader = new VideoLoader(getApplicationContext());
        videoLoader.loadDeviceVideos(new VideoLoadListener() {
            @Override
            public void onVideoLoaded(final List<VideoItem> items) {

                ListView lv = findViewById(R.id.video_list);
                VideoListAdapter adapter = new VideoListAdapter(getApplicationContext(), R.layout.row_video_list, items);
                lv.setAdapter(adapter);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        FillModeCustomActivity.startActivity(MovieListActivity.this, items.get(position).getPath());
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                e.printStackTrace();
            }
        });
    }

}
