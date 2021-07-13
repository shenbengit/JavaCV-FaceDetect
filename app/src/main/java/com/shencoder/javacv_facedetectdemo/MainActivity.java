package com.shencoder.javacv_facedetectdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.size.Size;
import com.shencoder.javacv_facedetect.AnybodyCallback;
import com.shencoder.javacv_facedetect.FaceDetectCameraView;
import com.shencoder.javacv_facedetect.FaceDetectRequestDialog;
import com.shencoder.javacv_facedetect.OnCameraListener;
import com.shencoder.javacv_facedetect.OnFaceDetectListener;
import com.shencoder.javacv_facedetect.RequestCallback;
import com.shencoder.javacv_facedetect.RequestDialogLayoutCallback;
import com.shencoder.javacv_facedetect.util.Nv21ToBitmapUtil;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class MainActivity extends AppCompatActivity {
    private FaceDetectRequestDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FaceDetectCameraView fdv = findViewById(R.id.fdv);
        ImageView iv = findViewById(R.id.iv);
        fdv.setOnCameraListener(new OnCameraListener() {
            @Override
            public void onCameraOpened() {

            }

            @Override
            public void onCameraClosed() {

            }

            @Override
            public void onCameraError(@NonNull @NotNull CameraException exception) {

            }
        });
        fdv.setPreviewStreamSize(source -> Collections.singletonList(new Size(1280, 720)));
        fdv.setOnFaceDetectListener(new OnFaceDetectListener() {
            @Override
            public void somebodyFrame(byte[] data, int width, int height, List<Rect> faceRectList) {

            }

            @Override
            public void somebodyFirstFrame(byte[] data, int width, int height, List<Rect> faceRectList) {
                if (!faceRectList.isEmpty()) {
                    Rect rect = faceRectList.get(0);
                    Bitmap bitmap = Nv21ToBitmapUtil.cropNv21ToBitmap(data, width, height, rect);
//                    Bitmap bitmap = Nv21ToBitmapUtil.nv21ToBitmap(data, width, height);
                    runOnUiThread(() -> iv.setImageBitmap(bitmap));
                }
            }

            @Override
            public void somebody() {
                System.out.println("当前有人--->");
            }

            @Override
            public void nobody() {
                System.out.println("当前无人--->");
            }
        });
//        fdv.setCameraFacing(Facing.BACK);
//        fdv.setKeepMaxFace(true);
//        fdv.setPreviewMirror(false);
//        fdv.setDetectAreaLimited(true);
//        fdv.setDrawFaceRect(true);
//        fdv.setFaceRectStrokeColor(Color.GREEN);
//        fdv.setFaceRectStrokeWidth(2f);
//        fdv.needRetry();
//        fdv.needRetryDelay(1000L);
        dialog = FaceDetectRequestDialog.builder(this,
                new RequestDialogLayoutCallback() {
                    @Override
                    public int getLayoutId() {
                        return R.layout.dialog_face_detect_request;
                    }

                    @Override
                    public int getFaceDetectCameraViewId() {
                        return R.id.detectCameraView;
                    }

                    @Override
                    public void initView(FaceDetectRequestDialog dialog) {
//                        Button btnClose = dialog.findViewById(R.id.btnClose);

                    }

                    @Override
                    public void onStart(FaceDetectRequestDialog dialog) {

                    }

                    @Override
                    public void onStop(FaceDetectRequestDialog dialog) {

                    }

                    @Override
                    public void onDestroy(FaceDetectRequestDialog dialog) {

                    }
                },
                new RequestCallback() {
                    @Override
                    @NonNull
                    public OkHttpClient.Builder generateOkhttpClient(OkHttpClient.Builder builder) {
                        return builder;
                    }

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
                })
                .build();
        Button button = findViewById(R.id.btnShowDialog);
        button.setOnClickListener(v -> dialog.show());

//        fdv.setLifecycleOwner(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dialog.destroy();
    }
}