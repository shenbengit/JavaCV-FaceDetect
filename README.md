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
## 效果展示
>FaceDetectCameraView运行效果
<img src="https://github.com/shenbengit/JavaCV-FaceDetect/blob/master/screenshots/FaceDetectCameraView.gif" alt="FaceDetectCameraView动图演示效果" width="600px">

>FaceDetectRequestDialog运行效果
<img src="https://github.com/shenbengit/JavaCV-FaceDetect/blob/master/screenshots/FaceDetectRequestDialog.gif" alt="FaceDetectCameraView动图演示效果" width="600px">

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
> 布局示例
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
> 布局说明
```Xml
    <declare-styleable name="FaceDetectCameraView">
        <!--摄像头类型，default:back-->
        <attr name="fdv_cameraFacing" format="enum">
            <enum name="back" value="0" />
            <enum name="front" value="1" />
        </attr>
        <!--是否仅检测最大人脸，default:true-->
        <attr name="fdv_keepMaxFace" format="boolean" />
        <!--预览画面是否镜像，default:false-->
        <attr name="fdv_previewMirror" format="boolean" />
        <!--是否限制检测区域，default:true-->
        <attr name="fdv_detectAreaLimited" format="boolean" />

        <!--级联分类器，default:R.raw.haarcascade_frontalface_alt-->
        <attr name="fdv_classifierFileRaw" format="reference" />

        <!--是否绘制人脸框，default:true-->
        <attr name="fdv_drawFaceRect" format="boolean" />
        <!--绘制人脸框的颜色，default:Color.GREEN-->
        <attr name="fdv_faceRectStrokeColor" format="color" />
        <!--绘制人脸框的宽度-->
        <attr name="fdv_faceRectStrokeWidth" format="dimension" />

    </declare-styleable>
```
>代码示例
```java
FaceDetectCameraView fdv = findViewById(R.id.fdv);
//设置摄像头相关回调
fdv.setOnCameraListener(new OnCameraListener() {
    @Override
    public void onCameraOpened() {

    }

    @Override
    public void onCameraClosed() {

    }

    @Override
    public void onCameraError(@NonNull CameraException exception) {

    }
});
//设置人脸检测相关回调接口
fdv.setOnFaceDetectListener(new OnFaceDetectListener() {
    /**
     * 摄像头的预览帧画面里检测到人就会调用
     * 子线程调用
     *
     * @param data         nv21
     * @param width        camera frame width
     * @param height       camera frame height
     * @param faceRectList 人脸位置数据
     */
    @WorkerThread
    @Override
    public void somebodyFrame(byte[] data, int width, int height, List<Rect> faceRectList) {

    }
        
    /**
     * 检测到有人会调用一次，和{@link OnFaceDetectListener#somebody()}一起调用
     * 子线程调用
     *
     * @param data         nv21
     * @param width        camera frame width
     * @param height       camera frame height
     * @param faceRectList 人脸位置数据
     */
    @WorkerThread
    @Override
    public void somebodyFirstFrame(byte[] data, int width, int height, List<Rect> faceRectList) {
        if (!faceRectList.isEmpty()) {
            Rect rect = faceRectList.get(0);
            //剪裁人脸
            Bitmap cropBitmap = Nv21ToBitmapUtil.cropNv21ToBitmap(data, width, height, rect);
            Bitmap bitmap = Nv21ToBitmapUtil.nv21ToBitmap(data, width, height);
         }
    }
        
    /**
     * 首次检测到有人时调用一次
     */
    @MainThread
    @Override
    public void somebody() {

    }
        
    /**
     * 首次检测到无人时调用一次
     */
    @MainThread
    @Override
    public void nobody() {

    }
});

//设置相机预览分辨率
fdv.setPreviewStreamSize(source -> Collections.singletonList(new Size(1280, 720)));
//设置摄像头
fdv.setCameraFacing(Facing.BACK);
//设置是否只保留最大人脸
fdv.setKeepMaxFace(true);
//是否镜像预览
fdv.setPreviewMirror(false);
//设置是否检测区域限制，注意：目前限制的区域是人脸是否完整在预览View显示的画面里
fdv.setDetectAreaLimited(true);
//是否绘制人脸框
fdv.setDrawFaceRect(true);
//设置人脸框颜色
fdv.setFaceRectStrokeColor(Color.GREEN);
//设置人脸框的宽度
fdv.setFaceRectStrokeWidth(2f);
//重试操作
fdv.needRetry();
//延迟重试操作
fdv.needRetryDelay(1000L);

fdv.setLifecycleOwner(this);
//fdv.open();
//fdv.close();
//fdv.destroy();

```
- FaceDetectRequestDialog
> 代码示例
```java
FaceDetectRequestDialog dialog = FaceDetectRequestDialog.builder(this,
                new RequestDialogLayoutCallback() {
                    /**
                     * 设置{@link FaceDetectRequestDialog#setContentView(int)}
                     *
                     * @return
                     */
                    @Override
                    public int getLayoutId() {
                        return R.layout.dialog_face_detect_request;
                    }
                    
                    /**
                     * 设置{@link RequestDialogLayoutCallback#getLayoutId()} 中 {@link FaceDetectCameraView}的id
                     *
                     * @return
                     */
                    @Override
                    public int getFaceDetectCameraViewId() {
                        return R.id.detectCameraView;
                    }
                    
                    /**
                     * init view
                     * (e.g. {@code dialog.findViewById(id)}).
                     *
                     * @param dialog FaceDetectRequestDialog
                     */
                    @Override
                    public void initView(FaceDetectRequestDialog dialog) {
//                        Button btnClose = dialog.findViewById(R.id.btnClose);
                    }

                    /**
                     * Called when the dialog is starting.
                     *
                     * @param dialog FaceDetectRequestDialog
                     */
                    @Override
                    public void onStart(FaceDetectRequestDialog dialog) {

                    }

                    /**
                     * Called when the dialog is starting.
                     *
                     * @param dialog FaceDetectRequestDialog
                     */
                    @Override
                    public void onStop(FaceDetectRequestDialog dialog) {

                    }
                    
                    /**
                     * @param dialog FaceDetectRequestDialog
                     * @see FaceDetectRequestDialog#destroy()
                     */
                    @Override
                    public void onDestroy(FaceDetectRequestDialog dialog) {

                    }
                },
                new RequestCallback() {
                
                    /**
                     * 生成用于网络请求的{@link OkHttpClient}
                     * 自动调用{@link OkHttpClient.Builder#build()}
                     *
                     * @param builder
                     * @return
                     */
                    @Override
                    @NonNull
                    public OkHttpClient.Builder generateOkhttpClient(OkHttpClient.Builder builder) {
                        return builder;
                    }

                    /**
                     * 生成网络请求的{@link Request}
                     * 自行根据人脸照片数据进行二次封装
                     * <p>
                     * 自动调用{@link Request.Builder#build()}
                     *
                     * @param builder
                     * @return
                     */
                    @NonNull
                    @Override
                    public Request.Builder generateRequest(Request.Builder builder, byte[] data, int width, int height, List<Rect> faceRectList) {
                        Bitmap bitmap = Nv21ToBitmapUtil.nv21ToBitmap(data, width, height);
//                        Bitmap bitmap = Nv21ToBitmapUtil.cropNv21ToBitmap(data, width, height, faceRectList.get(0));
                        if (bitmap != null) {
                            String base64 = Nv21ToBitmapUtil.bitmapToBase64(bitmap, 100);
                            RequestFaceBean bean = new RequestFaceBean("imagecompare", base64);
                            RequestBody body = RequestBody.create(MediaType.parse("application/json"), GsonUtil.toJson(bean));
                            builder.url("http://192.168.2.186:25110")
                                    .post(body);
                        }
                        return builder;
                    }

                    @Override
                    public void onRequestStart() {

                    }

                    @Override
                    public void onRequestFailure(Exception e) {
                        Toast.makeText(MainActivity.this, "人脸识别Error：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.needRetryDelay(2000L);
                    }

                    @Override
                    public void onRequestSuccess(String bodyStr) {
                        ResultBean resultBean = GsonUtil.jsonToBean(bodyStr, ResultBean.class);
                        if (resultBean.getResCode() == 1) {
                            Toast.makeText(MainActivity.this, "人脸识别成功:" + resultBean.getData().getUserName(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "人脸识别失败", Toast.LENGTH_SHORT).show();
                            dialog.needRetryDelay(2000L);
                        }
                    }
                })
                .setPreviewSizeSelector(source -> Collections.singletonList(new Size(1280, 720)))
                .setShowLoadingDialog(true)
                .setCameraListener(exception -> Toast.makeText(MainActivity.this, "摄像头开启异常：" + exception.getMessage(), Toast.LENGTH_SHORT).show())
                .setAnybodyCallback(new AnybodyCallback() {
                    @Override
                    public void somebody() {
                        System.out.println("有人--->");
                    }

                    @Override
                    public void nobody() {
                        System.out.println("无人--->");
                    }
                }).build();
```
