package com.hack.turbo_collage;

/**
 * Created by Yue on 2022/5/27.
 */
public class TCRectF {

    public float left;
    public float top;
    public float right;
    public float bottom;

    public TCRectF(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    @Override
    public String toString() {
        return "RectF{" +
                "left=" + left +
                ", top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                '}';
    }
}
