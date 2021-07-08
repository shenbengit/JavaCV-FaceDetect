package com.shencoder.javacv_facedetect;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
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

import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.size.SizeSelector;

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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;


/**
 * 基于JavaCV实现人脸检测功能
 * 基于{@link CameraView}
 * 人脸检测View
 *
 * @author ShenBen
 * @date 2021/7/7 10:37
 * @email 714081644@qq.com
 */
public class FaceDetectView extends FrameLayout implements LifecycleObserver {
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
    protected Mat mGrayImage;
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
    private volatile boolean keepMaxFace = false;
    /**
     * 是否镜像预览
     * 左右镜像
     *
     * @see android.view.View#setScaleX(float)
     */
    private volatile boolean previewMirror = false;
    /**
     * 是否绘制人脸框
     */
    private volatile boolean drawFaceRect = true;
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
    private final List<android.graphics.Rect> drawFaceRectList = new ArrayList<>();
    protected final Handler mHandler = new Handler(Looper.getMainLooper());

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();


    public FaceDetectView(@NonNull Context context) {
        this(context, null);
    }

    public FaceDetectView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceDetectView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.layout_face_detect, this);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FaceDetectView);
        int cameraFacing = typedArray.getInteger(R.styleable.FaceDetectView_fdv_cameraFacing, CAMERA_BACK);
        boolean keepMaxFace = typedArray.getBoolean(R.styleable.FaceDetectView_fdv_keepMaxFace, false);
        boolean previewMirror = typedArray.getBoolean(R.styleable.FaceDetectView_fdv_previewMirror, false);
        boolean drawFaceRect = typedArray.getBoolean(R.styleable.FaceDetectView_fdv_drawFaceRect, true);
        int strokeColor = typedArray.getColor(R.styleable.FaceDetectView_fdv_faceRectStrokeColor, Color.GREEN);
        float strokeWidth = typedArray.getDimension(R.styleable.FaceDetectView_fdv_faceRectStrokeWidth, 2f);
        int classifierFileRes = typedArray.getResourceId(R.styleable.FaceDetectView_fdv_classifierFileRaw, R.raw.haarcascade_frontalface_alt);
        typedArray.recycle();

        mCameraView = findViewById(R.id.cameraView);
        mFaceRectView = findViewById(R.id.faceRectView);

        setCameraFacing(getCameraFacing(cameraFacing));
        setKeepMaxFace(keepMaxFace);
        setPreviewMirror(previewMirror);
        setDrawFaceRect(drawFaceRect);
        setStrokeColor(strokeColor);
        setStrokeWidth(strokeWidth);

        mCameraView.addCameraListener(new CameraListener() {
            @Override
            public void onCameraOpened(@NonNull CameraOptions options) {
                dispatchOnCameraOpened();
            }

            @Override
            public void onCameraClosed() {
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
                Mat mat = mGrayImage;
                if (needInitGrayMat(mat, width, height)) {
                    mat = initGrayImage(width, height);
                    mGrayImage = mat;
                }
                processImage(mClassifier, mat, mFaceRectVector, frame.getData(), width, height);
            }
        });
        executorService.submit(() -> {
            try {
                loadClassifierCascade(classifierFileRes);
            } catch (Exception exception) {
                Log.e(TAG, "loadClassifierCascade exception: " + exception.getMessage());

            }
        });
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
    public void start() {
        mCameraView.open();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void stop() {
        mCameraView.close();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void destroy() {
        mCameraView.destroy();
        if (mGrayImage != null) {
            mGrayImage.release();
        }
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

    public void setDrawFaceRect(boolean isDraw) {
        drawFaceRect = isDraw;
    }

    public void setStrokeWidth(@Px float width) {
        mFaceRectView.setStrokeWidth(width);
    }

    public void setStrokeColor(@ColorInt int color) {
        mFaceRectView.setStrokeColor(color);
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
        if (keepMaxFace) {
            keepMapFace(faceRectList);
        }


        //人脸位置数据
        boolean anybody = size != 0;
        boolean drawRect = drawFaceRect;
        if (anybody) {
            for (Rect rect : faceRectList) {
                //转换的人脸位置
                android.graphics.Rect faceRect = FaceRectTransformerUtil.convertFaceRect(width, height, grayMat.cols(), grayMat.rows(), rect);
                faceCameraRectList.add(faceRect);
                if (drawRect) {
                    drawFaceRectList.add(FaceRectTransformerUtil.adjustRect(width, height,
                            getOutputSurfaceWidth(), getOutputSurfaceHeight(),
                            previewMirror,
                            faceRect)
                    );
                }
            }
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
     * @param faceRectList
     */
    private void judgeAnybody(boolean anybody, byte[] data, int width, int height, List<android.graphics.Rect> faceRectList) {
        if (isAnybody != anybody) {
            if (anybody) {
                dispatchSomebodyFirstFrame(data, width, height, faceRectList);
                dispatchOnSomebody();
            } else {
                dispatchOnNobody();
            }
            isAnybody = anybody;
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

    @Nullable
    public static Bitmap cropNv21ToBitmap(byte[] nv21, int width, int height, android.graphics.Rect rect) {
        android.graphics.Rect cropRect = getBestRect(width, height, rect);
        cropRect.left &= ~3;
        cropRect.top &= ~3;
        cropRect.right &= ~3;
        cropRect.bottom &= ~3;
        byte[] imageData = ArcSoftImageUtil.createImageData(cropRect.width(), cropRect.height(), ArcSoftImageFormat.NV21);
        int cropCode = ArcSoftImageUtil.cropImage(nv21, imageData, width, height, cropRect, ArcSoftImageFormat.NV21);
        if (cropCode != ArcSoftImageUtilError.CODE_SUCCESS) {
            return null;
        }
        Bitmap headBmp = Bitmap.createBitmap(cropRect.width(), cropRect.height(), Bitmap.Config.RGB_565);
        int imageDataToBitmapCode = ArcSoftImageUtil.imageDataToBitmap(imageData, headBmp, ArcSoftImageFormat.NV21);
        if (imageDataToBitmapCode != ArcSoftImageUtilError.CODE_SUCCESS) {
            return null;
        }
        return headBmp;
    }

    @Nullable
    public static Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        Bitmap headBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        int imageDataToBitmapCode = ArcSoftImageUtil.imageDataToBitmap(nv21, headBmp, ArcSoftImageFormat.NV21);
        if (imageDataToBitmapCode != ArcSoftImageUtilError.CODE_SUCCESS) {
            return null;
        }
        return headBmp;
    }

    /**
     * 将图像中需要截取的Rect向外扩张一倍，若扩张一倍会溢出，则扩张到边界，若Rect已溢出，则收缩到边界
     *
     * @param width   图像宽度
     * @param height  图像高度
     * @param srcRect 原Rect
     * @return 调整后的Rect
     */
    private static android.graphics.Rect getBestRect(int width, int height, @NonNull android.graphics.Rect srcRect) {
        android.graphics.Rect rect = new android.graphics.Rect(srcRect);

        // 原rect边界已溢出宽高的情况
        int maxOverFlow = Math.max(-rect.left, Math.max(-rect.top, Math.max(rect.right - width, rect.bottom - height)));
        if (maxOverFlow >= 0) {
            rect.inset(maxOverFlow, maxOverFlow);
            return rect;
        }

        // 原rect边界未溢出宽高的情况
        int padding = rect.height() / 2;

        // 若以此padding扩张rect会溢出，取最大padding为四个边距的最小值
        if (!(rect.left - padding > 0 && rect.right + padding < width && rect.top - padding > 0 && rect.bottom + padding < height)) {
            padding = Math.min(Math.min(Math.min(rect.left, width - rect.right), height - rect.bottom), rect.top);
        }
        rect.inset(-padding, -padding);
        return rect;
    }


}
