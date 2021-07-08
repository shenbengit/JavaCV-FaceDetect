package com.shencoder.javacv_facedetect;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.AnyThread;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 绘制人脸框
 *
 * @author ShenBen
 * @date 2021/7/7 15:00
 * @email 714081644@qq.com
 */
public class FaceRectView extends View {

    private final Paint mPaint;
    private final List<Rect> mFaceRectList = new CopyOnWriteArrayList<>();

    public FaceRectView(Context context) {
        this(context, null);
    }

    public FaceRectView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceRectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FaceRectView);
        int strokeColor = typedArray.getColor(R.styleable.FaceRectView_frv_strokeColor, Color.GREEN);
        float strokeWidth = typedArray.getDimension(R.styleable.FaceRectView_frv_strokeWidth, 2f);
        typedArray.recycle();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        setStrokeColor(strokeColor);
        setStrokeWidth(strokeWidth);
    }

    public void setStrokeWidth(float width) {
        mPaint.setStrokeWidth(width);
    }

    public void setStrokeColor(int color) {
        mPaint.setColor(color);
    }

    @AnyThread
    public void drawFaceRect(@Nullable List<Rect> list) {
        mFaceRectList.clear();
        if (list != null) {
            mFaceRectList.addAll(list);
        }
        postInvalidate();
    }

    @AnyThread
    public void clearFaceRect() {
        if (!mFaceRectList.isEmpty()) {
            mFaceRectList.clear();
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas == null) {
            return;
        }
        for (Rect rect : mFaceRectList) {
            canvas.drawRect(rect, mPaint);
        }
    }
}
