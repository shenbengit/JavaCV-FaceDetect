package com.shencoder.javacv_facedetect.util;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 使用虹软官方提供的图片剪裁、转换框架
 * 偷个懒
 *
 * @author ShenBen
 * @date 2021/7/9 9:58
 * @email 714081644@qq.com
 */
public class Nv21ToBitmapUtil {
    private static final String TAG = "Nv21ToBitmapUtil";

    public static String bitmapToBase64(@Nullable Bitmap bitmap, int quality) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        try {
            return Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
        } finally {
            try {
                bos.close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    /**
     * Nv21数据转Bitmap
     *
     * @param nv21
     * @param width
     * @param height
     * @return
     */
    @Nullable
    public static Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        Bitmap headBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        int result = ArcSoftImageUtil.imageDataToBitmap(nv21, headBmp, ArcSoftImageFormat.NV21);
        if (result != ArcSoftImageUtilError.CODE_SUCCESS) {
            Log.e(TAG, "nv21ToBitmap imageDataToBitmap error : " + result);
            return null;
        }
        return headBmp;
    }

    /**
     * 会进行二次转换，剪裁出的图片要比提供位置矩阵的略大
     *
     * @param nv21   nv21
     * @param width  width
     * @param height height
     * @param rect   需要剪裁数据的位置矩阵
     * @return
     */
    @Nullable
    public static Bitmap cropNv21ToBitmap(byte[] nv21, int width, int height, Rect rect) {
        Rect cropRect = getBestRect(width, height, rect);
        cropRect.left &= ~3;
        cropRect.top &= ~3;
        cropRect.right &= ~3;
        cropRect.bottom &= ~3;
        //创建数据
        byte[] imageData = ArcSoftImageUtil.createImageData(cropRect.width(), cropRect.height(), ArcSoftImageFormat.NV21);
        //剪裁数据
        int cropCode = ArcSoftImageUtil.cropImage(nv21, imageData, width, height, cropRect, ArcSoftImageFormat.NV21);
        if (cropCode != ArcSoftImageUtilError.CODE_SUCCESS) {
            Log.e(TAG, "cropNv21ToBitmap cropImage error : " + cropCode);
            return null;
        }
        return nv21ToBitmap(imageData, cropRect.width(), cropRect.height());
    }

    /**
     * 将图像中需要截取的Rect向外扩张一倍，若扩张一倍会溢出，则扩张到边界，若Rect已溢出，则收缩到边界
     *
     * @param width   图像宽度
     * @param height  图像高度
     * @param srcRect 原Rect
     * @return 调整后的Rect
     */
    private static Rect getBestRect(int width, int height, @NonNull Rect srcRect) {
        Rect rect = new Rect(srcRect);

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
