package com.shencoder.javacv_facedetect;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.CameraException;

/**
 * @author ShenBen
 * @date 2021/7/7 15:36
 * @email 714081644@qq.com
 */
public interface OnCameraListener {
    /**
     * 摄像头打开
     */
    @MainThread
    default void onCameraOpened() {

    }

    /**
     * 摄像头关闭
     */
    @MainThread
    default void onCameraClosed() {

    }

    /**
     * 摄像头异常
     *
     * @param exception
     */
    @MainThread
    void onCameraError(@NonNull CameraException exception);
}
