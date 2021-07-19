package com.shencoder.javacv_facedetect.util;

import android.graphics.Bitmap;
import android.util.Base64;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author ShenBen
 * @date 2021/7/19 16:29
 * @email 714081644@qq.com
 */
public class BitmapUtil {
    @Nullable
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
}
