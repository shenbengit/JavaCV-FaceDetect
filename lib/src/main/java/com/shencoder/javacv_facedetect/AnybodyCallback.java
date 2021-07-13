package com.shencoder.javacv_facedetect;

import androidx.annotation.MainThread;

/**
 * 人员状态回调
 *
 * @author ShenBen
 * @date 2021/7/13 16:26
 * @email 714081644@qq.com
 */
public interface AnybodyCallback {

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
