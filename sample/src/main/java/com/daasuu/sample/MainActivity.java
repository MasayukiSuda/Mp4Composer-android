package com.daasuu.sample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 88888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        findViewById(R.id.button).setOnClickListener(v -> {
            if (checkPermission()) {
                BasicUsageActivity.startActivity(MainActivity.this);
            }
        });
        findViewById(R.id.button2).setOnClickListener(v -> {
            if (checkPermission()) {
                MovieListActivity.startActivity(MainActivity.this);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();

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
