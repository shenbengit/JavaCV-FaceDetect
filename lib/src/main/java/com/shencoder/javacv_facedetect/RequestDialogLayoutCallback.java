package com.shencoder.javacv_facedetect;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;

/**
 * 设置{@link FaceDetectRequestDialog}中布局相关操作，及相关生命周期回调
 * 布局文件中必须要包括一个{@link FaceDetectCameraView}
 *
 * @author ShenBen
 * @date 2021/7/12 10:22
 * @email 714081644@qq.com
 */
public interface RequestDialogLayoutCallback {
    /**
     * 设置{@link FaceDetectRequestDialog#setContentView(int)}
     *
     * @return
     */
    @LayoutRes
    int getLayoutId();

    /**
     * 设置{@link RequestDialogLayoutCallback#getLayoutId()} 中 {@link FaceDetectCameraView}的id
     *
     * @return
     */
    @IdRes
    int getFaceDetectCameraViewId();

    /**
     * init view
     * (e.g. {@code dialog.findViewById(id)}).
     *
     * @param dialog FaceDetectRequestDialog
     */
    void initView(FaceDetectRequestDialog dialog);

    /**
     * Called when the dialog is starting.
     *
     * @param dialog FaceDetectRequestDialog
     */
    default void onStart(FaceDetectRequestDialog dialog) {

    }

    /**
     * Called when the dialog is starting.
     *
     * @param dialog FaceDetectRequestDialog
     */
    default void onStop(FaceDetectRequestDialog dialog) {

    }

    /**
     * @param dialog FaceDetectRequestDialog
     * @see FaceDetectRequestDialog#destroy()
     */
    default void onDestroy(FaceDetectRequestDialog dialog) {

    }
}
