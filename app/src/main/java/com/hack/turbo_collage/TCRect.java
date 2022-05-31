package com.hack.turbo_collage;


public class TCRect {

    public double left;

    public double top;

    public double right;

    public double bottom;

    public TCRect(double d, double d2, double d3, double d4) {
        this.left = d;
        this.top = d2;
        this.right = d3;
        this.bottom = d4;
    }


    public TCRectF getRectF() {
        return new TCRectF((float) this.left, (float) this.top, (float) (this.left + this.right), (float) (this.top + this.bottom));
    }


    @Override
    public String toString() {
        return "TCRect{" +
                "left=" + left +
                ", top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                '}';
    }
}