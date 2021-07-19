package com.shencoder.javacv_facedetect;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RawRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDialog;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.size.SizeSelector;
import com.shencoder.loadingdialog.LoadingDialog;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 检测到人脸然后执行网络请求操作Dialog
 * 仅会在每次检测到有人是执行网络请求操作
 *
 * @author ShenBen
 * @date 2021/7/9 10:31
 * @email 714081644@qq.com
 */
public class FaceDetectRequestDialog extends AppCompatDialog {
    protected final Builder builder;
    protected FaceDetectCameraView detectCameraView;
    private OkHttpClient mOkHttpClient;
    /**
     * 当次网络请求
     */
    private volatile Call mCall;
    /**
     * 网络请求的次数
     */
    private int requestTimes = 0;
    /**
     * 是否正在网络请求中
     */
    private volatile boolean isRequesting = false;

    private boolean isShowLoadingDialog = true;

    private Dialog loadingDialog;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private FaceDetectRequestDialog(@NonNull Builder builder) {
        super(builder.mContext, builder.mTheme);
        this.builder = builder;
    }

    @Override
    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(builder.mLayoutCallback.getLayoutId());
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        mOkHttpClient = builder.mRequestCallback.generateOkhttpClient(new OkHttpClient.Builder()).build();
        detectCameraView = findViewById(builder.mLayoutCallback.getFaceDetectCameraViewId());
        if (detectCameraView == null) {
            throw new NullPointerException("FaceDetectCameraView is null");
        }
        builder.mLayoutCallback.initView(this);
        setPreviewStreamSize(builder.previewSizeSelector);
        setShowLoadingDialog(builder.isShowLoadingDialog);

        detectCameraView.setOnCameraListener(new OnCameraListener() {
            @Override
            public void onCameraOpened() {
                OnCameraListener cameraListener = builder.cameraListener;
                if (cameraListener != null) {
                    cameraListener.onCameraOpened();
                }
            }

            @Override
            public void onCameraClosed() {
                OnCameraListener cameraListener = builder.cameraListener;
                if (cameraListener != null) {
                    cameraListener.onCameraClosed();
                }
            }

            @Override
            public void onCameraError(@NonNull CameraException exception) {
                OnCameraListener cameraListener = builder.cameraListener;
                if (cameraListener != null) {
                    cameraListener.onCameraError(exception);
                }
            }
        });
        detectCameraView.setOnFaceDetectListener(new OnFaceDetectListener() {
            @Override
            public void somebodyFirstFrame(byte[] data, int width, int height, List<Rect> faceRectList) {
                if (isRequesting) {
                    return;
                }
                httpRequest(data, width, height, faceRectList);
            }

            @Override
            public void somebody() {
                AnybodyCallback anybodyCallback = builder.anybodyCallback;
                if (anybodyCallback != null) {
                    anybodyCallback.somebody();
                }
            }

            @Override
            public void nobody() {
                AnybodyCallback anybodyCallback = builder.anybodyCallback;
                if (anybodyCallback != null) {
                    anybodyCallback.nobody();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        builder.mLayoutCallback.onStart(this);

        if (detectCameraView != null) {
            detectCameraView.open();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        builder.mLayoutCallback.onStop(this);

        if (detectCameraView != null) {
            detectCameraView.close();
        }
        cancelCallTask();
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        builder.mLayoutCallback.onDestroy(this);

        cancelCallTask();
        if (detectCameraView != null) {
            detectCameraView.destroy();
        }
        mHandler.removeCallbacksAndMessages(null);
        requestTimes = 0;
    }

    /**
     * 设置摄像头
     * 需在{@link AppCompatDialog#onCreate(Bundle)} 之后调用方可有效
     *
     * @param facing
     */
    public void setCameraFacing(Facing facing) {
        if (detectCameraView != null) {
            detectCameraView.setCameraFacing(facing);
        }
    }

    /**
     * 设置预览分辨率
     * 需在{@link AppCompatDialog#onCreate(Bundle)} 之后调用方可有效
     *
     * @param selector
     */
    public void setPreviewStreamSize(@Nullable SizeSelector selector) {
        if (detectCameraView != null && selector != null) {
            detectCameraView.setPreviewStreamSize(selector);
        }
    }

    /**
     * 设置仅保留最大人脸
     * 需在{@link AppCompatDialog#onCreate(Bundle)} 之后调用方可有效
     *
     * @param keepMaxFace
     */
    public void setKeepMaxFace(boolean keepMaxFace) {
        if (detectCameraView != null) {
            detectCameraView.setKeepMaxFace(keepMaxFace);
        }
    }

    /**
     * 设置预览画面是否镜像
     * 左右镜像
     * 需在{@link AppCompatDialog#onCreate(Bundle)} 之后调用方可有效
     *
     * @param isMirror 是否镜像显示
     */
    public void setPreviewMirror(boolean isMirror) {
        if (detectCameraView != null) {
            detectCameraView.setPreviewMirror(isMirror);
        }
    }

    /**
     * 设置是否限制检测区域
     * 注意：目前限制的区域是人脸是否完整在预览View显示的画面里
     * 需在{@link AppCompatDialog#onCreate(Bundle)} 之后调用方可有效
     *
     * @param limited 是否限制
     */
    public void setDetectAreaLimited(boolean limited) {
        if (detectCameraView != null) {
            detectCameraView.setDetectAreaLimited(limited);
        }
    }

    /**
     * 设置是否绘制人脸框
     * 需在{@link AppCompatDialog#onCreate(Bundle)} 之后调用方可有效
     *
     * @param isDraw
     */
    public void setDrawFaceRect(boolean isDraw) {
        if (detectCameraView != null) {
            detectCameraView.setDrawFaceRect(isDraw);
        }
    }

    /**
     * 设置人脸框宽度
     * 需在{@link AppCompatDialog#onCreate(Bundle)} 之后调用方可有效
     *
     * @param width
     */
    public void setFaceRectStrokeWidth(@Px float width) {
        if (detectCameraView != null) {
            detectCameraView.setFaceRectStrokeWidth(width);
        }
    }

    /**
     * 设置人脸框颜色
     * 需在{@link AppCompatDialog#onCreate(Bundle)} 之后调用方可有效
     *
     * @param color
     */
    public void setFaceRectStrokeColor(@ColorInt int color) {
        if (detectCameraView != null) {
            detectCameraView.setFaceRectStrokeColor(color);
        }
    }

    public void setShowLoadingDialog(boolean showLoadingDialog) {
        isShowLoadingDialog = showLoadingDialog;
    }

    /**
     * 设置目标检测的级联分类器
     * 需在{@link AppCompatDialog#onCreate(Bundle)} 之后调用方可有效
     *
     * @param resId    级联分类器
     * @param callback 加载结果回调
     */
    public void loadClassifierCascade(@RawRes final int resId, @Nullable LoadClassifierErrorCallback callback) {
        if (detectCameraView != null) {
            detectCameraView.loadClassifierCascade(resId, callback);
        }
    }

    /**
     * 重试操作
     */
    public void needRetry() {
        if (detectCameraView != null) {
            detectCameraView.needRetry();
        }
    }

    /**
     * 延迟重试操作
     *
     * @param delayMillis
     */
    public void needRetryDelay(long delayMillis) {
        if (detectCameraView != null) {
            detectCameraView.needRetryDelay(delayMillis);
        }
    }

    /**
     * @return 获取网络请求次数
     */
    public int getRequestTimes() {
        return requestTimes;
    }

    /**
     * @return 是否正在网络请求中
     */
    public boolean isRequesting() {
        return isRequesting;
    }

    /**
     * 取消任务
     */
    private void cancelCallTask() {
        if (mCall != null) {
            if (!mCall.isCanceled()) {
                mCall.cancel();
            }
        }
        mCall = null;
    }

    /**
     * 开始网络请求
     */
    private void httpRequest(byte[] data, int width, int height, List<Rect> faceRectList) {
        ++requestTimes;
        isRequesting = true;
        Request request = builder.mRequestCallback
                .generateRequest(new Request.Builder(),
                        data, width, height, faceRectList
                ).
                        build();
        mCall = mOkHttpClient.newCall(request);
        requestStart();
        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                isRequesting = false;
                requestFailure(e);
                mCall = null;
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                isRequesting = false;
                ResponseBody body = response.body();
                if (body == null) {
                    requestFailure(new IllegalArgumentException("ResponseBody is null"));
                    mCall = null;
                    return;
                }
                if (!response.isSuccessful()) {
                    requestFailure(new IllegalArgumentException("request failed, response's code is: " + response.code() + ", response's message is: " + response.message()));
                    mCall = null;
                    return;
                }
                String bodyStr = body.string();
                requestSuccess(bodyStr);
                mCall = null;
            }
        });
    }

    /**
     * @return 初始化加载中dialog
     */
    protected Dialog initLoadingDialog() {
        return LoadingDialog.builder(getContext()).setHintText("请稍后...").create();
    }

    private void requestStart() {
        mHandler.post(() -> {
            if (isShowLoadingDialog) {
                if (loadingDialog == null) {
                    loadingDialog = initLoadingDialog();
                }
                loadingDialog.show();
            }
            builder.mRequestCallback.onRequestStart(this);
        });
    }

    private void requestFailure(Exception e) {
        mHandler.post(() -> {
            if (isShowLoadingDialog) {
                if (loadingDialog != null) {
                    loadingDialog.cancel();
                }
            }
            builder.mRequestCallback.onRequestFailure(e, this);
        });
    }

    private void requestSuccess(String bodyStr) {
        mHandler.post(() -> {
            if (isShowLoadingDialog) {
                if (loadingDialog != null) {
                    loadingDialog.cancel();
                }
            }
            builder.mRequestCallback.onRequestSuccess(bodyStr, this);
        });
    }

    public static Builder builder(@NonNull Context context, @NonNull RequestDialogLayoutCallback layoutCallback, @NonNull RequestCallback requestCallback) {
        return builder(context, layoutCallback, requestCallback, R.style.FaceDetectRequestDialog);
    }

    public static Builder builder(@NonNull Context context, @NonNull RequestDialogLayoutCallback layoutCallback, @NonNull RequestCallback requestCallback, @StyleRes int theme) {
        return new Builder(context, layoutCallback, requestCallback, theme);
    }

    public static class Builder {
        final Context mContext;
        /**
         * Dialog布局相关回调
         */
        final RequestDialogLayoutCallback mLayoutCallback;
        /**
         * 网络请求相关回调
         */
        final RequestCallback mRequestCallback;
        /**
         * dialog's theme
         */
        final int mTheme;
        /**
         * 摄像头预览分辨率
         */
        @Nullable
        SizeSelector previewSizeSelector = null;

        boolean isShowLoadingDialog = true;

        @Nullable
        OnCameraListener cameraListener;
        @Nullable
        AnybodyCallback anybodyCallback;

        private Builder(@NonNull Context context, @NonNull RequestDialogLayoutCallback layoutCallback, @NonNull RequestCallback requestCallback, @StyleRes int theme) {
            this.mContext = context;
            this.mLayoutCallback = layoutCallback;
            this.mRequestCallback = requestCallback;
            this.mTheme = theme;
        }

        public Builder setPreviewSizeSelector(@Nullable SizeSelector previewSizeSelector) {
            this.previewSizeSelector = previewSizeSelector;
            return this;
        }

        /**
         * 是否显示加载中dialog
         *
         * @param showLoadingDialog true:显示，false:不显示
         * @return
         */
        public Builder setShowLoadingDialog(boolean showLoadingDialog) {
            isShowLoadingDialog = showLoadingDialog;
            return this;
        }

        public Builder setCameraListener(@Nullable OnCameraListener cameraListener) {
            this.cameraListener = cameraListener;
            return this;
        }

        public Builder setAnybodyCallback(@Nullable AnybodyCallback anybodyCallback) {
            this.anybodyCallback = anybodyCallback;
            return this;
        }

        public FaceDetectRequestDialog build() {
            return new FaceDetectRequestDialog(this);
        }
    }
}
