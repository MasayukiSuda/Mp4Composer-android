# Mp4Composer-android
[![Platform](https://img.shields.io/badge/platform-android-green.svg)](http://developer.android.com/index.html)
<img src="https://img.shields.io/badge/license-MIT-green.svg?style=flat">
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)

This library generate an Mp4 movie using Android MediaCodec API and apply filter, scale, and rotate Mp4.<br>
Idea from: [android-transcoder](https://github.com/ypresto/android-transcoder)

<table>
    <td><img src="art/sample.gif"><br>Sample Video<br>No filter</td>
    <td><img src="art/grayscale.gif" ><br><a href="mp4compose/src/main/java/com/daasuu/mp4compose/filter/GlGrayScaleFilter.java">GlGlayScaleFilter</a><br> apply</td>
    <td><img src="art/monochrome.gif" ><br><a href="mp4compose/src/main/java/com/daasuu/mp4compose/filter/GlMonochromeFilter.java">GlMonochromeFilter</a><br> apply</td>
    <td><img src="art/watermark.gif" ><br><a href="mp4compose/src/main/java/com/daasuu/mp4compose/filter/GlWatermarkFilter.java">GlWatermarkFilter</a><br> apply</td>
</table>






## Gradle
Step 1. Add the JitPack repository to your build file
```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
Step 2. Add the dependency
```groovy
dependencies {
        implementation 'com.github.MasayukiSuda:Mp4Composer-android:v0.2.0'
}
```

## Usage
```
    new Mp4Composer(srcMp4Path, destMp4Path)
            .rotation(Rotation.ROTATION_90)
            .size((width) 540, (height) 960)
            .fillMode(FillMode.PRESERVE_ASPECT_FIT)
            .filter(new GlFilterGroup(new GlMonochromeFilter(), new GlVignetteFilter()))
            .listener(new Mp4Composer.Listener() {
                @Override
                public void onProgress(double progress) {
                    Log.d(TAG, "onProgress = " + progress);
                }

                @Override
                public void onCompleted() {
                    Log.d(TAG, "onCompleted()");
                    runOnUiThread(() -> {
                        Toast.makeText(context, "codec complete path =" + destPath, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onCanceled() {
                    Log.d(TAG, "onCanceled");
                }

                @Override
                public void onFailed(Exception exception) {
                    Log.e(TAG, "onFailed()", exception);
                }
            })
            .start();
```
## Builder Method
| method | description |
|:---|:---|
| rotation | Rotation of the movie, default Rotation.NORMAL |
| size | Resolution of the movie, default same resolution of src movie |
| fillMode | Options for scaling the bounds of an movie. PRESERVE_ASPECT_FIT is fit center. PRESERVE_ASPECT_CROP is center crop , default PRESERVE_ASPECT_FIT |
| filter | This filter is OpenGL Shaders to apply effects on video. Custom filters can be created by inheriting <a href="https://github.com/MasayukiSuda/Mp4Composer-android/blob/master/mp4compose/src/main/java/com/daasuu/mp4compose/filter/GlFilter.java">GlFilter.java</a>. , default GlFilter(No filter). Filters is <a href="https://github.com/MasayukiSuda/Mp4Composer-android/tree/master/mp4compose/src/main/java/com/daasuu/mp4compose/filter">here</a>. |
| videoBitrate | Set Video Bitrate, default video bitrate is 0.25 * 30 * outputWidth * outputHeight |
| mute | Mute audio track on exported video. Default `mute = false`. |
| flipVertical | Flip Vertical on exported video. Default `flipVertical = false`. |
| flipHorizontal | Flip Horizontal on exported video. Default `flipHorizontal = false`. |



## References And Special Thanks to
* [android-transcoder](https://github.com/ypresto/android-transcoder)
* [android-transcoder Japanese blog](http://qiita.com/yuya_presto/items/d48e29c89109b746d000)
* [android-gpuimage](https://github.com/CyberAgent/android-gpuimage)
* [Android MediaCodec stuff](http://bigflake.com/mediacodec/)
* [grafika](https://github.com/google/grafika)
* [libstagefright](https://android.googlesource.com/platform/frameworks/av/+/lollipop-release/media/libstagefright)


## Sample Dependencies
* [glide](https://github.com/bumptech/glide)


## License

[MIT License](https://github.com/MasayukiSuda/Mp4Composer-android/blob/master/LICENSE)
