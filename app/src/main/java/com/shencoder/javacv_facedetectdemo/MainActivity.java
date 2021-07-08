package com.shencoder.javacv_facedetectdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.widget.ImageView;

import com.otaliastudios.cameraview.size.Size;
import com.shencoder.javacv_facedetect.FaceDetectView;
import com.shencoder.javacv_facedetect.FaceRectView;
import com.shencoder.javacv_facedetect.OnFaceDetectListener;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FaceDetectView fdv = findViewById(R.id.fdv);
        ImageView iv = findViewById(R.id.iv);
        fdv.setPreviewStreamSize(source -> Collections.singletonList(new Size(1280, 720)));
        fdv.setOnFaceDetectListener(new OnFaceDetectListener() {
            @Override
            public void somebodyFirstFrame(byte[] data, int width, int height, List<Rect> faceRectList) {
                if (!faceRectList.isEmpty()) {
                    Rect rect = faceRectList.get(0);
                    Bitmap bitmap = FaceDetectView.cropNv21ToBitmap(data, width, height, rect);
//                    Bitmap bitmap = FaceDetectView.nv21ToBitmap(data, width, height);
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
        fdv.setLifecycleOwner(this);
    }

}