package com.shencoder.javacv_facedetect;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
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
    protected OkHttpClient mOkHttpClient;
    /**
     * 当次网络请求
     */
    protected volatile Call mCall;
    /**
     * 网络请求的次数
     */
    protected int requestTimes = 0;
    /**
     * 是否正在网络请求中
     */
    protected volatile boolean isRequesting = false;

    @Nullable
    protected SizeSelector previewSizeSelector;
    protected boolean keepMaxFace = true;
    protected boolean previewMirror = false;
    protected boolean drawFaceRect = true;
    @Px
    protected float strokeWidth = 2f;
    @ColorInt
    protected int strokeColor = Color.GREEN;

    protected boolean isShowLoadingDialog = true;

    protected Dialog loadingDialog;
    private final Handler mHandler = new Handler(Looper.getMainLooper());


    FaceDetectRequestDialog(@NonNull Builder builder) {
        super(builder.mContext, builder.mTheme);
        this.builder = builder;
    }

    @Override
    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(getLayoutId());
        mOkHttpClient = builder.mRequestCallback.generateOkhttpClient(new OkHttpClient.Builder()).build();
        detectCameraView = findViewById(getFaceDetectCameraViewId());
        if (detectCameraView == null) {
            throw new NullPointerException("FaceDetectCameraView is null");
        }
        setCameraFacing(builder.mCameraFacing);
        setPreviewStreamSize(previewSizeSelector);
        setKeepMaxFace(keepMaxFace);
        setPreviewMirror(previewMirror);
        setDrawFaceRect(drawFaceRect);
        setStrokeWidth(strokeWidth);
        setStrokeColor(strokeColor);
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
        });
    }

    /**
     * @return dialog's layout id
     */
    protected int getLayoutId() {
        return R.layout.dialog_face_detect_request;
    }

    protected int getFaceDetectCameraViewId() {
        return R.id.faceDetectCameraView;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (detectCameraView != null) {
            detectCameraView.open();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (detectCameraView != null) {
            detectCameraView.close();
        }
        cancelCallTask();
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        cancelCallTask();
        if (detectCameraView != null) {
            detectCameraView.destroy();
        }
        mHandler.removeCallbacksAndMessages(null);
        requestTimes = 0;
    }

    public void setPreviewStreamSize(@Nullable SizeSelector selector) {
        previewSizeSelector = selector;
        if (detectCameraView != null && selector != null) {
            detectCameraView.setPreviewStreamSize(selector);
        }
    }

    public void setCameraFacing(Facing facing) {
        if (detectCameraView != null) {
            detectCameraView.setCameraFacing(facing);
        }
    }

    public void setKeepMaxFace(boolean keepMaxFace) {
        this.keepMaxFace = keepMaxFace;
        if (detectCameraView != null) {
            detectCameraView.setKeepMaxFace(keepMaxFace);
        }
    }

    /**
     * 设置预览画面是否镜像
     * 左右镜像
     *
     * @param isMirror 是否镜像显示
     */
    public void setPreviewMirror(boolean isMirror) {
        previewMirror = isMirror;
        if (detectCameraView != null) {
            detectCameraView.setPreviewMirror(isMirror);
        }
    }

    public void setDrawFaceRect(boolean isDraw) {
        drawFaceRect = isDraw;
        if (detectCameraView != null) {
            detectCameraView.setDrawFaceRect(isDraw);
        }
    }

    public void setStrokeWidth(@Px float width) {
        strokeWidth = width;
        if (detectCameraView != null) {
            detectCameraView.setStrokeWidth(width);
        }
    }

    public void setStrokeColor(@ColorInt int color) {
        strokeColor = color;
        if (detectCameraView != null) {
            detectCameraView.setStrokeColor(color);
        }
    }

    public void setShowLoadingDialog(boolean showLoadingDialog) {
        isShowLoadingDialog = showLoadingDialog;
    }

    /**
     * 调用这个方法需要在{@link Dialog#show()}之后
     * <p>
     * must be called after {@link Dialog#show()}
     *
     * @param resId
     * @param callback
     */
    public void loadClassifierCascade(@RawRes final int resId, @Nullable LoadClassifierErrorCallback callback) {
        if (detectCameraView != null) {
            detectCameraView.loadClassifierCascade(resId, callback);
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

    public void needRetry() {
        if (detectCameraView != null) {
            detectCameraView.needRetry();
        }
    }

    public void needRetryDelay(long delayMillis) {
        if (detectCameraView != null) {
            detectCameraView.needRetryDelay(delayMillis);
        }
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
                    requestFailure(new IllegalArgumentException("request failed , response's code is : " + response.code()));
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
            builder.mRequestCallback.onRequestStart();
        });
    }

    private void requestFailure(Exception e) {
        mHandler.post(() -> {
            if (isShowLoadingDialog) {
                if (loadingDialog != null) {
                    loadingDialog.cancel();
                }
            }
            builder.mRequestCallback.onRequestFailure(e);
        });
    }

    private void requestSuccess(String bodyStr) {
        mHandler.post(() -> {
            if (isShowLoadingDialog) {
                if (loadingDialog != null) {
                    loadingDialog.cancel();
                }
            }
            builder.mRequestCallback.onRequestSuccess(bodyStr);
        });
    }

    public static class Builder {
        final Context mContext;
        /**
         * camera facing
         */
        final Facing mCameraFacing;
        final RequestCallback mRequestCallback;
        /**
         * dialog's theme
         */
        final int mTheme;

        boolean isShowLoadingDialog = true;

        OnCameraListener cameraListener;

        public Builder(@NonNull Context context, Facing facing, @NonNull RequestCallback callback) {
            this(context, facing, callback, R.style.MyDialog);
        }

        public Builder(@NonNull Context context, Facing facing, @NonNull RequestCallback callback, @StyleRes int theme) {
            this.mContext = context;
            this.mCameraFacing = facing;
            this.mRequestCallback = callback;
            this.mTheme = theme;
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

        public Builder setCameraListener(OnCameraListener cameraListener) {
            this.cameraListener = cameraListener;
            return this;
        }

        public FaceDetectRequestDialog build() {
            return new FaceDetectRequestDialog(this);
        }
    }
}
