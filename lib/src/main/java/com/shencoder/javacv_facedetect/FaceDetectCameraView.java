package com.shencoder.javacv_facedetect;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RawRes;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.size.SizeSelector;
import com.shencoder.javacv_facedetect.util.FaceRectTransformerUtil;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;


/**
 * 基于JavaCV实现人脸检测功能
 * 基于{@link CameraView}
 * 人脸检测View
 * <p>
 * 目前仅支持camera预览角度为0°
 *
 * @author ShenBen
 * @date 2021/7/7 10:37
 * @email 714081644@qq.com
 */
public class FaceDetectCameraView extends FrameLayout implements LifecycleObserver {
    private static final String TAG = "FaceDetectView";
    protected static final int CAMERA_BACK = 0;
    protected static final int CAMERA_FRONT = 1;
    protected static final int SUBSAMPLING_FACTOR = 4;

    private Lifecycle mLifecycle;
    private final CameraView mCameraView;
    private final FaceRectView mFaceRectView;

    protected OnCameraListener mOnCameraListener;

    protected OnFaceDetectListener mOnFaceDetectListener;
    private CascadeClassifier mClassifier;
    protected Mat mGrayMat;
    protected final RectVector mFaceRectVector = new RectVector();

    /**
     * 当前是否有人
     * true:有人
     * false:无人
     */
    private volatile boolean isAnybody = false;

    /**
     * 仅检测最大人脸
     */
    private volatile boolean keepMaxFace = true;
    /**
     * 是否镜像预览
     * 左右镜像
     *
     * @see android.view.View#setScaleX(float)
     */
    private volatile boolean previewMirror = false;
    /**
     * 是否限制限制检测区域
     * 注意：目前限制的区域是人脸是否完整在预览View显示的画面里
     */
    private volatile boolean detectAreaLimited;

    private final android.graphics.Rect mDetectAreaLimitedRect = new android.graphics.Rect(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    /**
     * 是否绘制人脸框
     */
    private volatile boolean drawFaceRect = true;

    /**
     * 需要重新推送人脸数据
     */
    private volatile boolean needRetry = false;

    /**
     * 人脸框集合
     */
    private final List<Rect> faceRectList = new ArrayList<>(10);
    /**
     * 转换到摄像头预览画面中的人脸位置集合
     */
    private final List<android.graphics.Rect> faceCameraRectList = new ArrayList<>(10);
    /**
     * 转换用于画人脸框的位置集合
     */
    private final List<android.graphics.Rect> drawFaceRectList = new ArrayList<>(10);
    protected final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable retryRunnable = () -> {
        if (isAnybody) {
            //仅在有人的情况下执行操作
            needRetry = true;
        }
    };
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public FaceDetectCameraView(@NonNull Context context) {
        this(context, null);
    }

    public FaceDetectCameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceDetectCameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FaceDetectCameraView);
        int cameraFacing = typedArray.getInteger(R.styleable.FaceDetectCameraView_fdv_cameraFacing, CAMERA_BACK);
        boolean keepMaxFace = typedArray.getBoolean(R.styleable.FaceDetectCameraView_fdv_keepMaxFace, true);
        boolean previewMirror = typedArray.getBoolean(R.styleable.FaceDetectCameraView_fdv_previewMirror, false);
        boolean detectAreaLimited = typedArray.getBoolean(R.styleable.FaceDetectCameraView_fdv_detectAreaLimited, true);
        int classifierFileRes = typedArray.getResourceId(R.styleable.FaceDetectCameraView_fdv_classifierFileRaw, R.raw.haarcascade_frontalface_alt);
        boolean drawFaceRect = typedArray.getBoolean(R.styleable.FaceDetectCameraView_fdv_drawFaceRect, true);
        int strokeColor = typedArray.getColor(R.styleable.FaceDetectCameraView_fdv_faceRectStrokeColor, Color.GREEN);
        float strokeWidth = typedArray.getDimension(R.styleable.FaceDetectCameraView_fdv_faceRectStrokeWidth, 2f);
        typedArray.recycle();

        LayoutInflater.from(context).inflate(R.layout.layout_face_detect, this);
        mCameraView = findViewById(R.id.cameraView);
        mFaceRectView = findViewById(R.id.faceRectView);

        setCameraFacing(getCameraFacing(cameraFacing));
        setKeepMaxFace(keepMaxFace);
        setPreviewMirror(previewMirror);
        setDetectAreaLimited(detectAreaLimited);
        setDrawFaceRect(drawFaceRect);
        setFaceRectStrokeColor(strokeColor);
        setFaceRectStrokeWidth(strokeWidth);

        mCameraView.addCameraListener(new CameraListener() {
            @Override
            public void onCameraOpened(@NonNull CameraOptions options) {
                dispatchOnCameraOpened();
                isAnybody = false;
            }

            @Override
            public void onCameraClosed() {
                mFaceRectView.clearFaceRect();
                removeRetry();
                dispatchOnCameraClosed();
            }

            @Override
            public void onCameraError(@NonNull CameraException exception) {
                dispatchOnCameraError(exception);
            }
        });
        mCameraView.addFrameProcessor(frame -> {
            if (mClassifier != null && frame.getDataClass() == byte[].class) {
                int width = frame.getSize().getWidth();
                int height = frame.getSize().getHeight();
                Mat mat = mGrayMat;
                if (needInitGrayMat(mat, width, height)) {
                    mat = initGrayImage(width, height);
                    mGrayMat = mat;
                }
                //目前仅支持摄像机角度为0°
                byte[] data = frame.getData();
//                byte[] newData = Nv21Util.rotateNv21(data, width, height, frame.getRotationToUser());
//                if (newData == null) {
//                    return;
//                }
                processImage(mClassifier, mat, mFaceRectVector, data, width, height);
            }
        });
        loadClassifierCascade(classifierFileRes, null);
    }

    public void setLifecycleOwner(@Nullable LifecycleOwner owner) {
        clearLifecycleObserver();
        if (owner != null) {
            mLifecycle = owner.getLifecycle();
            mLifecycle.addObserver(this);
        }
    }

    private void clearLifecycleObserver() {
        if (mLifecycle != null) {
            mLifecycle.removeObserver(this);
            mLifecycle = null;
        }
    }

    public void setOnCameraListener(OnCameraListener listener) {
        mOnCameraListener = listener;
    }

    public void setOnFaceDetectListener(OnFaceDetectListener listener) {
        mOnFaceDetectListener = listener;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void open() {
        mCameraView.open();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void close() {
        mCameraView.close();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void destroy() {
        mCameraView.destroy();
        if (mGrayMat != null) {
            mGrayMat.release();
        }
        mHandler.removeCallbacksAndMessages(null);
    }

    public void setPreviewStreamSize(@NonNull SizeSelector selector) {
        mCameraView.setPreviewStreamSize(selector);
    }

    public void setCameraFacing(Facing facing) {
        mCameraView.setFacing(facing);
    }

    public void setKeepMaxFace(boolean keepMaxFace) {
        this.keepMaxFace = keepMaxFace;
    }

    /**
     * 设置预览画面是否镜像
     * 左右镜像
     *
     * @param isMirror 是否镜像显示
     * @see android.view.View#setScaleX(float)
     */
    public void setPreviewMirror(boolean isMirror) {
        mCameraView.setScaleX(isMirror ? -1f : 1f);
        previewMirror = isMirror;
    }

    /**
     * 设置是否限制检测区域
     * 注意：目前限制的区域是人脸是否完整在预览View显示的画面里
     *
     * @param limited 是否限制
     */
    public void setDetectAreaLimited(boolean limited) {
        if (!limited) {
            mDetectAreaLimitedRect.set(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        detectAreaLimited = limited;
    }

    public void setDrawFaceRect(boolean isDraw) {
        drawFaceRect = isDraw;
    }

    public void setFaceRectStrokeWidth(@Px float width) {
        mFaceRectView.setStrokeWidth(width);
    }

    public void setFaceRectStrokeColor(@ColorInt int color) {
        mFaceRectView.setStrokeColor(color);
    }

    /**
     * 重试操作
     * 为了再一次执行{@link OnFaceDetectListener#somebodyFirstFrame(byte[], int, int, List)}
     * 因为只有首次识别到有人才会调用
     */
    public void needRetry() {
        needRetryDelay(0L);
    }

    /**
     * 重试操作
     * 为了再一次执行{@link OnFaceDetectListener#somebodyFirstFrame(byte[], int, int, List)}
     * 因为只有首次识别到有人才会调用
     *
     * @param delayMillis 延迟时间
     */
    public void needRetryDelay(long delayMillis) {
        removeRetry();
        mHandler.postDelayed(retryRunnable, delayMillis);
    }

    private Facing getCameraFacing(int cameraFacing) {
        if (cameraFacing == CAMERA_FRONT) {
            return Facing.FRONT;
        }
        return Facing.BACK;
    }

    private void dispatchOnCameraOpened() {
        if (mOnCameraListener != null) {
            mOnCameraListener.onCameraOpened();
        }
    }

    private void dispatchOnCameraClosed() {
        if (mOnCameraListener != null) {
            mOnCameraListener.onCameraClosed();
        }
    }

    private void dispatchOnCameraError(@NonNull CameraException exception) {
        if (mOnCameraListener != null) {
            mOnCameraListener.onCameraError(exception);
        }
    }

    private void dispatchSomebodyFrame(byte[] data, int width, int height, List<android.graphics.Rect> faceRectList) {
        if (mOnFaceDetectListener != null) {
            mOnFaceDetectListener.somebodyFrame(data, width, height, faceRectList);
        }
    }

    private void dispatchSomebodyFirstFrame(byte[] data, int width, int height, List<android.graphics.Rect> faceRectList) {
        if (mOnFaceDetectListener != null) {
            mOnFaceDetectListener.somebodyFirstFrame(data, width, height, faceRectList);
        }
    }

    private void dispatchOnSomebody() {
        mHandler.post(() -> {
            if (mOnFaceDetectListener != null) {
                mOnFaceDetectListener.somebody();
            }
        });
    }

    private void dispatchOnNobody() {
        mHandler.post(() -> {
            if (mOnFaceDetectListener != null) {
                mOnFaceDetectListener.nobody();
            }
        });
    }

    /**
     * 设置目标检测的级联分类器
     *
     * @param resId    级联分类器
     * @param callback 加载结果回调
     */
    public void loadClassifierCascade(@RawRes final int resId, @Nullable LoadClassifierErrorCallback callback) {
        executorService.submit(() -> {
            try {
                loadClassifierCascade(resId);
            } catch (Exception exception) {
                Log.e(TAG, "loadClassifierCascade exception: " + exception.getMessage());
                if (callback != null) {
                    callback.onError(exception);
                }
            }
        });
    }

    private void loadClassifierCascade(@RawRes final int resId) throws Exception {
        InputStream inputStream = getResources().openRawResource(resId);
        File file = new File(getContext().getCacheDir(), "classifier" + System.currentTimeMillis() + ".xml");
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            byte[] bytes = new byte[4096];
            int readLength;
            while ((readLength = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, readLength);
            }
        } finally {
            inputStream.close();
            if (outputStream != null) {
                outputStream.close();
            }
        }
        mClassifier = new CascadeClassifier(file.getAbsolutePath());
        file.delete();
        if (mClassifier.isNull()) {
            throw new IOException("Could not load the classifier file.");
        }
    }

    protected boolean needInitGrayMat(@Nullable Mat mat, int width, int height) {
        int f = SUBSAMPLING_FACTOR;
        return mat == null || mat.cols() != width / f || mat.rows() != height / f;
    }

    /**
     * 初始化Mat
     *
     * @param width  camera preview width
     * @param height camera preview height
     * @return
     */
    protected Mat initGrayImage(int width, int height) {
        int f = SUBSAMPLING_FACTOR;
        return new Mat(height / f, width / f, CV_8UC1);
    }

    /**
     * 可继承此类重写该方法，实现自己的逻辑
     *
     * @param classifier
     * @param data       nv21
     * @param width      width
     * @param height     height
     */
    protected void processImage(@NonNull CascadeClassifier classifier, @NonNull Mat grayMat, @NonNull RectVector vector, byte[] data, int width, int height) {
        int f = SUBSAMPLING_FACTOR;
        int imageWidth = grayMat.cols();
        int imageHeight = grayMat.rows();
        int dataStride = f * width;
        int imageStride = (int) grayMat.step(0);
        ByteBuffer imageBuffer = grayMat.createBuffer();
        for (int y = 0; y < imageHeight; y++) {
            int dataLine = y * dataStride;
            int imageLine = y * imageStride;
            for (int x = 0; x < imageWidth; x++) {
                imageBuffer.put(imageLine + x, data[dataLine + f * x]);
            }
        }
        classifier.detectMultiScale(grayMat, vector);

        //检测人脸数量
        detectFaceNumber(vector, grayMat, data, width, height);
    }

    /**
     * 检测人脸数量
     *
     * @param vector
     * @param data
     * @param width
     * @param height
     */
    protected void detectFaceNumber(@NonNull RectVector vector, @NonNull Mat grayMat, byte[] data, int width, int height) {
        faceRectList.clear();
        faceCameraRectList.clear();
        drawFaceRectList.clear();
        long size = vector.size();
        for (int i = 0; i < size; i++) {
            faceRectList.add(vector.get(i));
        }
        //保留最大人脸
        if (keepMaxFace) {
            keepMapFace(faceRectList);
        }

        boolean drawRect = drawFaceRect;
        //人脸位置数据
        for (Rect rect : faceRectList) {
            //人脸再Camera预览数据中的位置
            android.graphics.Rect faceRect = FaceRectTransformerUtil.convertFaceRect(width, height, grayMat.cols(), grayMat.rows(), rect);
            //人脸在预览布局中的位置
            android.graphics.Rect adjustRect = FaceRectTransformerUtil.adjustRect(width, height,
                    getOutputSurfaceWidth(), getOutputSurfaceHeight(),
                    previewMirror,
                    faceRect);

            //是否在有效区域内
            if (detectAreaValid(adjustRect)) {
                faceCameraRectList.add(faceRect);
                //绘制人脸框
                if (drawRect) {
                    drawFaceRectList.add(adjustRect);
                }
            }
        }

        boolean anybody = !faceCameraRectList.isEmpty();
        if (anybody) {
            dispatchSomebodyFrame(data, width, height, faceCameraRectList);
        }
        judgeAnybody(anybody, data, width, height, faceCameraRectList);
        if (drawRect) {
            mFaceRectView.drawFaceRect(drawFaceRectList);
        } else {
            mFaceRectView.clearFaceRect();
        }
    }

    /**
     * 判断是否有人
     *
     * @param anybody      是否有人
     * @param faceRectList 人脸位置数据
     */
    private void judgeAnybody(boolean anybody, byte[] data, int width, int height, List<android.graphics.Rect> faceRectList) {
        if (isAnybody != anybody) {
            removeRetry();
            isAnybody = anybody;
            if (anybody) {
                dispatchOnSomebody();
                dispatchSomebodyFirstFrame(data, width, height, faceRectList);
                return;
            } else {
                dispatchOnNobody();
            }
        }
        if (anybody) {
            if (needRetry) {
                //重新分发人脸数据
                dispatchSomebodyFirstFrame(data, width, height, faceRectList);
                needRetry = false;
            }
        }
    }

    private void keepMapFace(@NonNull List<Rect> list) {
        if (list.size() <= 1) {
            return;
        }
        Rect maxRect = list.get(0);
        for (Rect rect : list) {
            if (rect.width() * rect.height() > maxRect.width() * maxRect.height()) {
                maxRect = rect;
            }
        }
        list.clear();
        list.add(maxRect);
    }

    private int getOutputSurfaceWidth() {
        return mCameraView.getWidth() - mCameraView.getPaddingStart() - mCameraView.getPaddingEnd();
    }

    private int getOutputSurfaceHeight() {
        return mCameraView.getHeight() - mCameraView.getPaddingTop() - mCameraView.getPaddingBottom();
    }

    /**
     * 检测人脸是否在有效区域
     *
     * @param faceRect
     * @return
     */
    private boolean detectAreaValid(android.graphics.Rect faceRect) {
        boolean detectAreaValid = true;
        if (detectAreaLimited) {
            android.graphics.Rect limitedRect = this.mDetectAreaLimitedRect;
            limitedRect.set(0, 0, getOutputSurfaceWidth(), getOutputSurfaceHeight());
            detectAreaValid = limitedRect.contains(faceRect);
        }
        return detectAreaValid;
    }

    private void removeRetry() {
        needRetry = false;
        mHandler.removeCallbacks(retryRunnable);
    }

}
