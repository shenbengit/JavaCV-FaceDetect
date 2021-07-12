package com.shencoder.javacv_facedetect;

import android.graphics.Rect;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * @author ShenBen
 * @date 2021/7/9 11:32
 * @email 714081644@qq.com
 */
public interface RequestCallback {

    /**
     * 生成用于网络请求的{@link OkHttpClient}
     * 自动调用{@link OkHttpClient.Builder#build()}
     *
     * @param builder
     * @return
     */
    @NonNull
    default OkHttpClient.Builder generateOkhttpClient(OkHttpClient.Builder builder) {
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
    @WorkerThread
    Request.Builder generateRequest(Request.Builder builder, byte[] data, int width, int height, List<Rect> faceRectList);

    /**
     * 网络请求开始
     */
    @MainThread
    default void onRequestStart(){

    }

    /**
     * 网络请求失败
     *
     * @param e error
     */
    @MainThread
    void onRequestFailure(Exception e);

    /**
     * 网络请求成功
     *
     * @param bodyStr 网络请求返回的String字符串
     */
    @MainThread
    void onRequestSuccess(String bodyStr);
}
