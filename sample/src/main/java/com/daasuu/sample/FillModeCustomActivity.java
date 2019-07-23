package com.daasuu.sample;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.daasuu.mp4compose.FillMode;
import com.daasuu.mp4compose.FillModeCustomItem;
import com.daasuu.mp4compose.composer.Mp4Composer;
import com.daasuu.sample.widget.GesturePlayerTextureView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FillModeCustomActivity extends AppCompatActivity {

    private final static String PATH_ARG = "PATH_ARG";
    private static final String TAG = "SAMPLE";

    private String srcPath;
    private float baseWidthSize;
    private GesturePlayerTextureView playerTextureView;

    public static void startActivity(Context context, String path) {
        Intent intent = new Intent(context, FillModeCustomActivity.class);
        intent.putExtra(PATH_ARG, path);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fill_mode_custom);

        if (getIntent() == null || getIntent().getStringExtra(PATH_ARG) == null) {
            finish();
            return;
        }
        srcPath = getIntent().getStringExtra(PATH_ARG);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        findViewById(R.id.btn_rotate).setOnClickListener((v) -> {
            playerTextureView.updateRotate();
        });
        findViewById(R.id.btn_codec).setOnClickListener((v) -> {
            codec();
        });


        FrameLayout frameLayout = findViewById(R.id.layout_crop_change);
        playerTextureView = new GesturePlayerTextureView(getApplicationContext(), srcPath);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;

        playerTextureView.setLayoutParams(layoutParams);
        baseWidthSize = (getWindowHeight(this) - dp2px(192, this)) / 16f * 9;
        playerTextureView.setBaseWidthSize(baseWidthSize);

        frameLayout.addView(playerTextureView, 1);
    }

    private void codec() {

        Size resolution = getVideoResolution(srcPath);

        FillModeCustomItem fillModeCustomItem = new FillModeCustomItem(
                playerTextureView.getScaleX(),
                playerTextureView.getRotation(),
                playerTextureView.getTranslationX() / baseWidthSize * 2f,
                playerTextureView.getTranslationY() / (baseWidthSize / 9f * 16) * 2f,
                resolution.getWidth(),
                resolution.getHeight()
        );

        final String videoPath = getVideoFilePath();
        new Mp4Composer(srcPath, videoPath)
                .size(720, 1280)
                .fillMode(FillMode.CUSTOM)
                .customFillMode(fillModeCustomItem)
                .listener(new Mp4Composer.Listener() {
                    @Override
                    public void onProgress(double progress) {
                        Log.d(TAG, "onProgress = " + progress);
                        //runOnUiThread(() -> progressBar.setProgress((int) (progress * 100)));
                    }

                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "onCompleted()");
                        exportMp4ToGallery(getApplicationContext(), videoPath);
                        runOnUiThread(() -> {
                            //progressBar.setProgress(100);
                            //findViewById(R.id.start_codec_button).setEnabled(true);
                            //findViewById(R.id.start_play_movie).setEnabled(true);
                            Toast.makeText(FillModeCustomActivity.this, "codec complete path =" + videoPath, Toast.LENGTH_SHORT).show();
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


    public static int getWindowHeight(Context context) {
        Display disp = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        disp.getSize(size);
        return size.y;
    }

    public static int dp2px(float dpValue, final Context context) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public File getAndroidMoviesFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
    }

    public String getVideoFilePath() {
        return getAndroidMoviesFolder().getAbsolutePath() + "/" + new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date()) + "filter_apply.mp4";
    }

    public Size getVideoResolution(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        int width = Integer.valueOf(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        );
        int height = Integer.valueOf(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        );
        retriever.release();
        int rotation = getVideoRotation(path);
        if (rotation == 90 || rotation == 270) {
            return new Size(height, width);
        }
        return new Size(width, height);
    }


    public int getVideoRotation(String videoFilePath) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(videoFilePath);
        String orientation = mediaMetadataRetriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
        );
        return Integer.valueOf(orientation);
    }

    public static void exportMp4ToGallery(Context context, String filePath) {
        final ContentValues values = new ContentValues(2);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, filePath);
        context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://" + filePath)));
    }
}
