package com.example.pagtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class PagSeekBar extends View {
    //==========================自定义区域===========================

    //游标外部的圆环
    private final int mThumbOutWidth = UtilsKt.dp2px(17f);
    private final int mThumbOutColor = Color.parseColor("#262626");

    //游标
    private final int mThumbWidth = UtilsKt.dp2px(14.0f);
    private final int mThumbColor = Color.parseColor("#F9CFAE");
    //进度条
    private final int mProgressBarHeight = UtilsKt.dp2px(2.0f);
    private final int mProgressBarColor = Color.parseColor("#F9CFAE");
    private final int mProgressBarSecondaryColor = Color.GRAY;
    private final int mProgressBarRadius = UtilsKt.dp2px(2.0f);
    //额外的触摸区域，竖直向
    private final int mTouchArea = UtilsKt.dp2px(8.0f);
    //===============================================================

    //画笔
    private final Paint mThumbOutPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mProgressBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mProgressSecondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    //progressBar的坐标
    private float mProgressLeft;
    private float mProgressTop;
    private float mProgressRight;
    private float mProgressBottom;
    //进度，绘制游标的基准
    private float mProgress = 0.5f;

    //最大值，计算value用
    private long mMax = 100;

    private boolean mIsTouch = false;

    private OnTouchListener mOnTouchListener;

    private boolean mIsRtl = false;

    public PagSeekBar(Context context) {
        super(context);
        init();
    }

    public PagSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PagSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        mThumbOutPaint.setColor(mThumbOutColor);
        mThumbOutPaint.setStrokeWidth(mThumbOutWidth);
        mThumbOutPaint.setStrokeCap(Paint.Cap.ROUND);

        mThumbPaint.setColor(mThumbColor);
        mThumbPaint.setStrokeWidth(mThumbWidth);
        mThumbPaint.setStrokeCap(Paint.Cap.ROUND);

        mProgressBarPaint.setColor(mProgressBarColor);
        mProgressSecondaryPaint.setColor(mProgressBarSecondaryColor);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //wrap content的尺寸，一般为view最小尺寸
        //最小宽度是两端的游标距离 + 两端的触摸区域 + padding + 一个48dp
        int minWidth = mThumbOutWidth * 2 + mTouchArea * 2 + getPaddingStart() + getPaddingEnd() + UtilsKt.dp2px(48);
        //最小高度是游标的高度 + 上下的触摸区域 + padding
        int minHeight = mThumbOutWidth + mTouchArea * 2 + getPaddingTop() + getPaddingBottom();

        int w = resolveSize(minWidth, widthMeasureSpec);
        int h = resolveSize(minHeight, heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w == 0 || h == 0) return;

        //计算progressBar的矩形坐标
        //左右两端是view的两端。去掉 padding  触摸区域 和 一半的游标
        //上下是在view减去padding的高度后，矩形居中
        mProgressLeft = getPaddingStart() + mThumbOutWidth / 2f + mTouchArea;
        mProgressTop = (h - getPaddingTop() - getPaddingBottom()) / 2f + getPaddingTop() - mProgressBarHeight / 2f;
        mProgressRight = w - getPaddingEnd() - mThumbOutWidth / 2f - mTouchArea;
        mProgressBottom = mProgressTop + mProgressBarHeight;

        mIsRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mIsRtl) {
            rtlDraw(canvas);
            return;
        }
        //绘制底层矩形
        canvas.drawRoundRect(
                mProgressLeft,
                mProgressTop,
                mProgressRight,
                mProgressBottom,
                mProgressBarRadius,
                mProgressBarRadius,
                mProgressSecondaryPaint
        );

        float thumbX = getThumbX();
        float thumbY = getThumbY();
        //绘制顶层矩形
        canvas.drawRoundRect(
                mProgressLeft,
                mProgressTop,
                thumbX,
                mProgressBottom,
                mProgressBarRadius,
                mProgressBarRadius,
                mProgressBarPaint
        );
        //绘制游标外圆环
        canvas.drawPoint(thumbX, thumbY, mThumbOutPaint);

        //绘制游标
        canvas.drawPoint(thumbX, thumbY, mThumbPaint);
    }

    private void rtlDraw(Canvas canvas) {
        //绘制底层矩形
        canvas.drawRoundRect(
                mProgressLeft,
                mProgressTop,
                mProgressRight,
                mProgressBottom,
                mProgressBarRadius,
                mProgressBarRadius,
                mProgressSecondaryPaint
        );

        float thumbX = getThumbX();
        float thumbY = getThumbY();

        //绘制顶层矩形
        canvas.drawRoundRect(
                thumbX,
                mProgressTop,
                mProgressRight,
                mProgressBottom,
                mProgressBarRadius,
                mProgressBarRadius,
                mProgressBarPaint
        );
        //绘制游标外圆环
        canvas.drawPoint(thumbX, thumbY, mThumbOutPaint);

        //绘制游标
        canvas.drawPoint(thumbX, thumbY, mThumbPaint);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //判断落点
                if (pointInArea(x, y)) {
                    mProgress = getProgress(x);
                    invalidate();
                    mIsTouch = true;
                    if (mOnTouchListener != null) mOnTouchListener.onStartTouch(getValue());
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsTouch) {
                    mProgress = getProgress(x);
                    invalidate();
                    if (mOnTouchListener != null) mOnTouchListener.onTouch(getValue());
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mIsTouch) {
                    mProgress = getProgress(x);
                    invalidate();
                    mIsTouch = false;
                    if (mOnTouchListener != null) mOnTouchListener.onEndTouch(getValue());
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    //根据进度获取当前游标的x坐标
    private float getThumbX() {
        if (mIsRtl) {
            return mProgressRight - (mProgressRight - mProgressLeft) * mProgress;
        }
        return mProgressLeft + (mProgressRight - mProgressLeft) * mProgress;
    }

    //获取游标的y坐标
    private float getThumbY() {
        return mProgressTop + (mProgressBottom - mProgressTop) * 0.5f;
    }

    //根据游标的x坐标 计算progress
    private float getProgress(float x) {
        if (mIsRtl) {
            float progress = (mProgressRight - x) / (mProgressRight - mProgressLeft);
            if (progress > 1.0f) progress = 1.0f;
            if (progress < 0.0f) progress = 0.0f;
            return progress;
        }
        float progress = (x - mProgressLeft) / (mProgressRight - mProgressLeft);
        if (progress > 1.0f) progress = 1.0f;
        if (progress < 0.0f) progress = 0.0f;
        return progress;
    }

    //判断是否可以拖动游标，有 拖动 和 既能点击又能拖动两种判断
    private boolean pointInArea(float x, float y) {

        //        //判断落点是否在游标里，这种无法点击，只能拖动游标
//        float thumbX = getThumbX();
//        float thumbY = getThumbY();
//        return x > (thumbX - mThumbWidth / 2f - mTouchArea)
//                && x < (thumbX + mThumbWidth / 2f + mTouchArea)
//                && y > (thumbY - mThumbWidth / 2f - mTouchArea)
//                && y < (thumbY + mThumbWidth / 2f + mTouchArea);

        //判断落点是否在矩形内，这种可以点击和拖动
        return x > (mProgressLeft - mTouchArea - mThumbOutWidth / 2f)
                && x < (mProgressRight + mTouchArea + mThumbOutWidth / 2f)
                && y > (getThumbY() - mTouchArea - mThumbOutWidth / 2f)
                && y < (getThumbY() + mTouchArea + mThumbOutWidth / 2f);

    }

    //Max must be > 0
    public void setMax(long max) {
        if (max > 0) {
            mMax = max;
        }
    }

    public long getMax() {
        return mMax;
    }

    //获取当前游标值
    public long getValue() {
        return (long) (mProgress * mMax);
    }

    public void setValue(long value) {
        mProgress = value * 1.0f / mMax;
        if (mProgress < 0f) mProgress = 0f;
        if (mProgress > 1.0f) mProgress = 1.0f;
        invalidate();
    }

    public float getProgress() {
        return mProgress;
    }

    // 触摸监听
    public void setOnSeekBarTouchListener(OnTouchListener onTouchListener) {
        mOnTouchListener = onTouchListener;
    }

    public interface OnTouchListener {
        void onStartTouch(long value);

        void onTouch(long value);

        void onEndTouch(long value);
    }

}
