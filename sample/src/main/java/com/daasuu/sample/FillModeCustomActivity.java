package com.daasuu.sample;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.daasuu.mp4compose.FillMode;
import com.daasuu.mp4compose.FillModeCustomItem;
import com.daasuu.mp4compose.composer.Mp4Composer;
import com.daasuu.mp4compose.filter.GlFilter;
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
    private Button btnRotate;
    private Button btnCodec;
    private Button btnClose;
    private Button btnStartPlayMovie;
    private Button btnColorChange;
    private RelativeLayout layoutCodec;
    private FrameLayout layoutCropChange;
    private AlertDialog clearColorDialog;
    private SceneCropColor sceneCropColor = SceneCropColor.BLACK;

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

        btnRotate = findViewById(R.id.btn_rotate);
        btnRotate.setOnClickListener((v) -> {
            playerTextureView.updateRotate();
        });
        btnCodec = findViewById(R.id.btn_codec);
        btnCodec.setOnClickListener((v) -> {
            codec();
        });
        layoutCodec = findViewById(R.id.layout_codec);
        btnClose = findViewById(R.id.close);
        btnStartPlayMovie = findViewById(R.id.start_play_movie);

        btnColorChange = findViewById(R.id.btn_color_change);
        layoutCropChange = findViewById(R.id.layout_crop_change);
        btnColorChange.setOnClickListener((v) -> {
            if (clearColorDialog == null) {

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Choose a background color");
                builder.setOnDismissListener(dialog -> {
                    clearColorDialog = null;
                });

                final SceneCropColor[] items = SceneCropColor.values();
                CharSequence[] charList = new CharSequence[items.length];
                for (int i = 0, n = items.length; i < n; i++) {
                    charList[i] = items[i].name();
                }
                builder.setItems(charList, (dialog, item) -> {
                    sceneCropColor = items[item];
                    layoutCropChange.setBackgroundColor(ContextCompat.getColor(FillModeCustomActivity.this, sceneCropColor.getColorRes()));
                });
                clearColorDialog = builder.show();
            } else {
                clearColorDialog.dismiss();
            }
        });

        initPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playerTextureView != null) {
            playerTextureView.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (playerTextureView != null) {
            playerTextureView.pause();
        }
    }

    private void initPlayer() {
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

        layoutCodec.setVisibility(View.VISIBLE);
        btnCodec.setEnabled(false);
        btnRotate.setEnabled(false);
        btnColorChange.setEnabled(false);
        final ProgressBar progressBar = findViewById(R.id.progress_bar);
        progressBar.setMax(100);

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
        btnStartPlayMovie.setEnabled(false);
        btnStartPlayMovie.setOnClickListener((v) -> {
            Uri uri = Uri.parse(videoPath);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setDataAndType(uri, "video/mp4");
            startActivity(intent);
        });
        GlFilter glFilter = new GlFilter();
        ClearColorItem clearColorItem = sceneCropColor.getClearColorItem();
        glFilter.setClearColor(clearColorItem.getRed(), clearColorItem.getGreen(), clearColorItem.getBlue(), clearColorItem.getAlpha());

        new Mp4Composer(srcPath, videoPath)
                .size(720, 1280)
                .filter(glFilter)
                .fillMode(FillMode.CUSTOM)
                .customFillMode(fillModeCustomItem)
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

                            btnStartPlayMovie.setEnabled(true);
                            btnClose.setVisibility(View.VISIBLE);
                            btnClose.setOnClickListener((v) -> {
                                layoutCodec.setVisibility(View.GONE);
                                btnCodec.setEnabled(true);
                                btnRotate.setEnabled(true);
                                btnColorChange.setEnabled(true);
                            });
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
        return getAndroidMoviesFolder().getAbsolutePath() + "/" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".mp4";
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
