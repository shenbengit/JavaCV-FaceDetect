package com.shencoder.javacv_facedetect;


import android.graphics.Rect;

import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import java.util.List;

/**
 * 人脸检测相关回调接口
 *
 * @author ShenBen
 * @date 2021/7/8 8:54
 * @email 714081644@qq.com
 */
public interface OnFaceDetectListener {
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
    default void somebodyFrame(byte[] data, int width, int height, List<Rect> faceRectList) {

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
    default void somebodyFirstFrame(byte[] data, int width, int height, List<Rect> faceRectList) {

    }


    /**
     * 首次检测到有人时调用一次
     */
    @MainThread
    default void somebody() {

    }

    /**
     * 首次检测到无人时调用一次
     */
    @MainThread
    default void nobody() {

    }
}
