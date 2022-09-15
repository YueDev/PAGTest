package com.example.pagtest.xcollage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.pagtest.R;
import com.hack.turbo_collage.TCBitmap;
import com.hack.turbo_collage.TCCollage;
import com.hack.turbo_collage.TCRectF;
import com.hack.turbo_collage.TCResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yue on 2022/9/15.
 */
public class XCollageView extends View {


    private final TCCollage mCollage = new TCCollage();

    private final CollageType mCollageType = CollageType.CENTER_CROP;

    private final List<XBitmap> mXBitmaps = new ArrayList<>();
    private final Paint mBitmapPaint = new Paint();

    private boolean mCanDraw = false;

    public XCollageView(Context context) {
        super(context);
        init();
    }

    public XCollageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();

    }

    public XCollageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setFilterBitmap(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!mCanDraw) return;
        for (XBitmap xBitmap : mXBitmaps) {
            canvas.save();
            canvas.clipRect(
                    xBitmap.getRect().left,
                    xBitmap.getRect().top,
                    xBitmap.getRect().right,
                    xBitmap.getRect().bottom
            );
            canvas.drawBitmap(xBitmap.getBitmap(), xBitmap.getMatrix(), mBitmapPaint);
        }
    }

    public void setBitmaps(List<Bitmap> bitmaps) {
        post(() -> {
            mXBitmaps.clear();
            for (Bitmap bitmap : bitmaps) {
                mXBitmaps.add(new XBitmap(bitmap));
            }

            List<TCBitmap> tcBitmaps = new ArrayList<>();
            for (XBitmap xBitmap : mXBitmaps) {
                tcBitmaps.add(new TCBitmap(xBitmap.getId(), xBitmap.getBitmap().getWidth(), xBitmap.getBitmap().getHeight()));
            }

            mCollage.init(tcBitmaps);
            mCanDraw = true;
        });
    }


    //拼图 无动画
    public void collage() {
        new Thread(() -> {
            TCResult result = mCollage.collage(getMeasuredWidth(), getMeasuredHeight(), 0.0);
            if (result != null) {
                for (XBitmap xBitmap : mXBitmaps) {
                    TCRectF tcRect = result.get(xBitmap.getId());
                    xBitmap.getRect().set(
                            tcRect.left,
                            tcRect.top,
                            tcRect.right,
                            tcRect.bottom
                    );

                    switch (mCollageType) {
                        case FIT_CENTER:
                            fitCenter(xBitmap.getBitmap(), xBitmap.getRect(), xBitmap.getMatrix());
                            break;
                        case CENTER_CROP:
                            centerCrop(xBitmap.getBitmap(), xBitmap.getRect(), xBitmap.getMatrix());
                            break;
                    }
                }
                postInvalidate();
            }
        }).start();
    }


    private void fitCenter(Bitmap bitmap, RectF rect, Matrix matrix) {

        float scaleW = rect.width() / bitmap.getWidth();
        float scaleH = rect.height() / bitmap.getHeight();

        float scale = Math.min(scaleW, scaleH);
        float dx = rect.centerX() - bitmap.getWidth() / 2f;
        float dy = rect.centerY() - bitmap.getHeight() / 2f;

        matrix.reset();
        matrix.postTranslate(dx, dy);
        matrix.postScale(scale, scale, rect.centerX(), rect.centerY());
    }

    private void centerCrop(Bitmap bitmap, RectF rect, Matrix matrix) {

        float scaleW = rect.width() / bitmap.getWidth();
        float scaleH = rect.height() / bitmap.getHeight();

        float scale = Math.max(scaleW, scaleH);
        float dx = rect.centerX() - bitmap.getWidth() / 2f;
        float dy = rect.centerY() - bitmap.getHeight() / 2f;

        matrix.reset();
        matrix.postTranslate(dx, dy);
        matrix.postScale(scale, scale, rect.centerX(), rect.centerY());
    }

}
