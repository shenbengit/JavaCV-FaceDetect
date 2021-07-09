package com.shencoder.javacv_facedetect.util;

import android.graphics.Rect;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.size.AspectRatio;

/**
 * @author ShenBen
 * @date 2021/7/8 14:15
 * @email 714081644@qq.com
 */
public class FaceRectTransformerUtil {
    /**
     * 转换人脸数据在摄像头预览数据中的位置
     *
     * @param previewWidth
     * @param previewHeight
     * @param matCols
     * @param matRows
     * @param detectRect
     * @return
     */
    @NonNull
    public static Rect convertFaceRect(int previewWidth, int previewHeight,
                                       int matCols, int matRows,
                                       @NonNull org.bytedeco.opencv.opencv_core.Rect detectRect) {
        Rect rect = new Rect();
        float detectScaleX = (float) previewWidth / matCols;
        float detectScaleY = (float) previewHeight / matRows;
        int detectLeft = (int) (detectRect.x() * detectScaleX);
        int detectTop = (int) (detectRect.y() * detectScaleY);
        int detectRight = (int) ((detectRect.x() + detectRect.width()) * detectScaleX);
        int detectBottom = (int) ((detectRect.y() + detectRect.height()) * detectScaleY);
        rect.set(detectLeft, detectTop, detectRight, detectBottom);
        return rect;
    }

    /**
     * 转换人脸框位置
     *
     * @param previewWidth
     * @param previewHeight
     * @param canvasWidth
     * @param canvasHeight
     * @param isMirror
     * @param detectRect
     * @return
     */
    @NonNull
    public static Rect adjustRect(int previewWidth, int previewHeight,
                                  int canvasWidth, int canvasHeight,
                                  boolean isMirror,
                                  @NonNull Rect detectRect) {
        Rect rect = new Rect();
        //当前宽高比
        AspectRatio current = AspectRatio.of(canvasWidth, canvasHeight);
        //目标宽高比
        AspectRatio target = AspectRatio.of(previewWidth, previewHeight);

        int detectLeft = detectRect.left;
        int detectTop = detectRect.top;
        int detectRight = detectRect.right;
        int detectBottom = detectRect.bottom;

        int left;
        int top;
        int right;
        int bottom;
        int offset;
        float ratio;

        if (current.toFloat() >= target.toFloat()) {
            float scaleY = current.toFloat() / target.toFloat();
            int ratioHeight = (int) (canvasHeight * scaleY);
            offset = (ratioHeight >> 1) - (canvasHeight >> 1);
            ratio = (float) canvasWidth / previewWidth;
            left = (int) (detectLeft * ratio);
            top = (int) (detectTop * ratio);
            right = (int) (detectRight * ratio);
            bottom = (int) (detectBottom * ratio);

            rect.left = isMirror ? canvasWidth - right : left;
            rect.top = top - offset;
            rect.right = isMirror ? canvasWidth - left : right;
            rect.bottom = bottom - offset;
        } else {
            float scaleX = target.toFloat() / current.toFloat();
            int ratioWidth = (int) (canvasWidth * scaleX);
            offset = (ratioWidth >> 1) - (canvasWidth >> 1);
            ratio = (float) ratioWidth / previewWidth;
            left = (int) (detectLeft * ratio);
            top = (int) (detectTop * ratio);
            right = (int) (detectRight * ratio);
            bottom = (int) (detectBottom * ratio);

            rect.left = isMirror ? ratioWidth - right - offset : left - offset;
            rect.top = top;
            rect.right = isMirror ? ratioWidth - left - offset : right - offset;
            rect.bottom = bottom;
        }
        return rect;
    }

}
