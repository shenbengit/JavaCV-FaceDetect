package com.shencoder.javacv_facedetect;

import androidx.annotation.WorkerThread;

/**
 * @author ShenBen
 * @date 2021/7/9 17:16
 * @email 714081644@qq.com
 */
public interface LoadClassifierErrorCallback {

    @WorkerThread
    void onError(Exception e);
}
