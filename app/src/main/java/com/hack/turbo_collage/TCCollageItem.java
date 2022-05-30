package com.hack.turbo_collage;

public class TCCollageItem extends TCCollageItemBase {

    TCRect ratioRect;

    public TCCollageItem(String uuid, TCRect ratioRect) {
        super(uuid);
        this.ratioRect = ratioRect;
    }

    public double getRatioMaxBound(double canvasWidth, double canvasHeight) {
        double w = canvasWidth * this.ratioRect.right;
        double h = this.ratioRect.bottom * canvasHeight;
        int i = (Double.compare(w, h));
        double max = w;
        if (i < 0) {
            max = h;
        }
        return max;
    }

    @Override
    public String toString() {
        return "CollageItem{" +
                "ratioRect=" + ratioRect +
                ", uuid='" + uuid + '\'' +
                '}';
    }
}