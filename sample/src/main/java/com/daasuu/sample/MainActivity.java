package com.daasuu.sample;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.daasuu.mp4compose.FillMode;
import com.daasuu.mp4compose.composer.Mp4Composer;
import com.daasuu.mp4compose.filter.GlLutFilter;
import com.daasuu.sample.video.VideoItem;
import com.daasuu.sample.video.VideoListAdapter;
import com.daasuu.sample.video.VideoLoadListener;
import com.daasuu.sample.video.VideoLoader;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private VideoLoader videoLoader;

    private VideoItem videoItem = null;

    private static final String TAG = "SAMPLE";

    private static final int PERMISSION_REQUEST_CODE = 88888;

    private Mp4Composer mp4Composer;
    private Bitmap bitmap;

    private CheckBox muteCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        muteCheckBox = findViewById(R.id.mute_check_box);

        findViewById(R.id.start_codec_button).setOnClickListener(v -> {
            v.setEnabled(false);
            startCodec();
        });

        findViewById(R.id.cancel_button).setOnClickListener(v -> {
            if (mp4Composer != null) {
                mp4Composer.cancel();
            }
        });

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.lookup_sample);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermission()) {
            videoLoader = new VideoLoader(getApplicationContext());
            videoLoader.loadDeviceVideos(new VideoLoadListener() {
                @Override
                public void onVideoLoaded(final List<VideoItem> items) {

                    ListView lv = findViewById(R.id.video_list);
                    VideoListAdapter adapter = new VideoListAdapter(getApplicationContext(), R.layout.row_video_list, items);
                    lv.setAdapter(adapter);

                    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            videoItem = null;
                            videoItem = items.get(position);
                            findViewById(R.id.start_codec_button).setEnabled(true);
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

    private void startCodec() {
        final String videoPath = getVideoFilePath();

        final ProgressBar progressBar = findViewById(R.id.progress_bar);
        progressBar.setMax(100);

        boolean mute = muteCheckBox.isChecked();

        mp4Composer = null;
        mp4Composer = new Mp4Composer(videoItem.getPath(), videoPath)
                // .rotation(Rotation.ROTATION_270)
                //.size(720, 1280)
                .fillMode(FillMode.PRESERVE_ASPECT_FIT)
                .filter(new GlLutFilter(bitmap))
                .mute(mute)
                .listener(new Mp4Composer.Listener() {
                    @Override
                    public void onProgress(double progress) {
                        Log.d(TAG, "onProgress = " + progress);
                        runOnUiThread(() -> progressBar.setProgress((int) (progress * 100)));
                    }

                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "onCompleted()");
                        exportMp4ToGallery(getApplicationContext(), videoPath);
                        runOnUiThread(() -> {
                            progressBar.setProgress(100);
                            findViewById(R.id.start_codec_button).setEnabled(true);
                            Toast.makeText(MainActivity.this, "codec complete path =" + videoPath, Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onCanceled() {

                    }

                    @Override
                    public void onFailed(Exception exception) {
                        Log.d(TAG, "onFailed()");
                    }
                })
                .start();


    }


    public File getAndroidMoviesFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
    }

    public String getVideoFilePath() {
        return getAndroidMoviesFolder().getAbsolutePath() + "/" + new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date()) + "filter_apply.mp4";
    }

    /**
     * ギャラリーにエクスポート
     *
     * @param filePath
     * @return The video MediaStore URI
     */
    public static void exportMp4ToGallery(Context context, String filePath) {
        // ビデオのメタデータを作成する
        final ContentValues values = new ContentValues(2);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, filePath);
        // MediaStoreに登録
        context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://" + filePath)));
    }


    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        // request permission if it has not been grunted.
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "permission has been grunted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "[WARN] permission is not grunted.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

}
