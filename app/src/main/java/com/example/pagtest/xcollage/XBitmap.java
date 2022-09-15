package com.example.pagtest.xcollage;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;

import java.util.UUID;

/**
 * Created by Yue on 2022/9/15.
 */
public class XBitmap {

    private final Bitmap mBitmap;
    private final Matrix mMatrix = new Matrix();
    private final String mId = UUID.randomUUID().toString();
    private final RectF mRect = new RectF();
    private final RectF mTempRect = new RectF();

    public XBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public Matrix getMatrix() {
        return mMatrix;
    }

    public String getId() {
        return mId;
    }

    public RectF getRect() {
        return mRect;
    }

    public RectF getTempRect() {
        return mTempRect;
    }
}
