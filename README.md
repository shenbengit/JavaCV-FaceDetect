# JavaCV-FaceDetect
Android端基于JavaCV实现人脸检测功能
## 实现功能
- 人脸检测功能：[FaceDetectCameraView](https://github.com/shenbengit/JavaCV-FaceDetect/blob/master/lib/src/main/java/com/shencoder/javacv_facedetect/FaceDetectCameraView.java)
- 人脸检测后自动网络请求功能：[FaceDetectRequestDialog](https://github.com/shenbengit/JavaCV-FaceDetect/blob/master/lib/src/main/java/com/shencoder/javacv_facedetect/FaceDetectRequestDialog.java)
## 项目引入框架
- [javacv](https://github.com/bytedeco/javacv)
- [CameraView](https://github.com/natario1/CameraView)
- [okhttp](https://github.com/square/okhttp)
- [LoadingDialog](https://github.com/shenbengit/LoadingDialog)

## 引入
### 将JitPack存储库添加到您的项目中(项目根目录下build.gradle文件)
```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
### 添加依赖
[![](https://jitpack.io/v/shenbengit/JavaCV-FaceDetect.svg)](https://jitpack.io/#shenbengit/JavaCV-FaceDetect)
> 在您引入项目的build.gradle中添加
```gradle
android {
    ...
    defaultConfig {
        ...
        ndk {
            // 设置支持的SO库架构，仅支持armeabi-v7a、arm64-v8a，若想减小APK体积，可只引用对应的SO库架构
            abiFilters 'armeabi-v7a', 'arm64-v8a'
        }
    }
    
    //解决JavaCV中文件重复问题
    packagingOptions {
        pickFirst 'META-INF/native-image/android-arm/jnijavacpp/jni-config.json'
        pickFirst 'META-INF/native-image/android-arm64/jnijavacpp/jni-config.json'
        pickFirst 'META-INF/native-image/android-arm/jnijavacpp/reflect-config.json'
        pickFirst 'META-INF/native-image/android-arm64/jnijavacpp/reflect-config.json'
    }
}

dependencies {
    implementation 'com.github.shenbengit:JavaCV-FaceDetect:Tag'
}
```
## 使用事例
- FaceDetectCameraView
> 布局事例
```Xml
    <com.shencoder.javacv_facedetect.FaceDetectCameraView
        android:id="@+id/fdv"
        android:layout_width="match_parent"
        android:layout_height="500dp"
        app:fdv_cameraFacing="back"
        app:fdv_classifierFileRaw="@raw/haarcascade_frontalface_alt"
        app:fdv_detectAreaLimited="true"
        app:fdv_drawFaceRect="true"
        app:fdv_faceRectStrokeColor="@color/design_default_color_error"
        app:fdv_faceRectStrokeWidth="3dp"
        app:fdv_keepMaxFace="true"
        app:fdv_previewMirror="true" />
```


