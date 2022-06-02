package com.hack.turbo_collage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Yue on 2022/5/28.
 */
public class TCCollage {

    private final List<TCCollageItem> collageItems = new ArrayList<>();

    private double width = 1000;
    private double height = 1500;

    private final Map<String, TCBitmap> bitmapMap = new HashMap<>();


    //初始化，设置TCBitmaps数据，传入的每张图片的尺寸，之后调用collage()
    //图片尺寸变化时，重新调用一下这个方法即可
    public void init(List<TCBitmap> bitmaps) {

        bitmapMap.clear();
        collageItems.clear();

        collageItems.add(new TCCollageItem(null, new TCRect(0.0d, 0.0d, 1.0d, 1.0d)));

        if (bitmaps != null && bitmaps.size() > 0) {
            for (TCBitmap bitmap : bitmaps) {
                String uuid = bitmap.getUUID();
                TCCollageItem emptyCollageItem = getEmptyItemOrNull();
                if (emptyCollageItem != null) {
                    emptyCollageItem.uuid = uuid;
                } else {
                    int d = d();
                    TCRect tcRect = d < 0 ? new TCRect(0.0d, 0.0d, 1.0d, 1.0d) : a(d);
                    collageItems.add(new TCCollageItem(uuid, tcRect));
                }
                bitmapMap.put(uuid, bitmap);
            }
        }
    }

    //拼图，并且返回结果  回调版本 java用这个
    public void collage(double width, double height, double padding, OnResultListener mListener) {
        try {
            this.width = width;
            this.height = height;
            reCollage();
            TCResult result = drawCollage(padding);
            mListener.onSuccess(result);
        } catch (Exception e) {
            e.printStackTrace();
            mListener.onError(e);
        }
    }

    //同步版本 kotlin协程用这个
    public TCResult collage(double width, double height, double padding) {
        try {
            this.width = width;
            this.height = height;
            reCollage();
            return drawCollage(padding);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    private TCResult drawCollage(double frame) {
        TCResult result = new TCResult();
        if (this.collageItems.size() > 0) {
            for (int i = 0; i < this.collageItems.size(); i++) {
                TCCollageItem collageItem = (TCCollageItem) this.collageItems.get(i);
                TCRect tcRecf = getItemInCanvasRect(collageItem, frame, width, height);
                if (!collageItem.emptyUUID()) {
                    result.add(collageItem.uuid, tcRecf.getRectF());
                }
            }
        }
        return result;
    }


    TCRect getItemInCanvasRect(TCCollageItem collageItem, double frame, double width, double height) {
        double c = collageItem.ratioRect.left < 1.0E-4d ? 2.0d * frame : frame;
        double c2 = collageItem.ratioRect.left + collageItem.ratioRect.right > 0.9999d ? 2.0d * frame : frame;
        double c3 = collageItem.ratioRect.top < 1.0E-4d ? 2.0d * frame : frame;
        double c4 = frame;
        if (collageItem.ratioRect.top + collageItem.ratioRect.bottom > 0.9999d) {
            c4 = frame * 2.0d;
        }
        return new TCRect((collageItem.ratioRect.left * width) + c, (collageItem.ratioRect.top * height) + c3, (collageItem.ratioRect.right * width) - (c + c2), (collageItem.ratioRect.bottom * height) - (c3 + c4));
    }


    private void reCollage() {
        this.collageItems.clear();
        if (bitmapMap.size() <= 0) {
            collageItems.add(new TCCollageItem(null, new TCRect(0.0d, 0.0d, 1.0d, 1.0d)));
        } else {
            HashMap<String, Double> ratioMap = new HashMap<>();
            for (Map.Entry<String, TCBitmap> entry : bitmapMap.entrySet()) {
                String uuid = entry.getKey();
                TCBitmap bitmap = entry.getValue();
                ratioMap.put(uuid, bitmap.getWidth() * 1.0 / bitmap.getHeight());
            }

            TCShuffle totalShuffle = TCShuffle.getTotalShuffle(ratioMap, this.width, this.height);
            //这个返回这个结果，就是计算的质量Math.abs(totalShuffle.a() - ((this.width) / this.height)) < 0.01d;
            //一般来说 几张图计算出来 留白比较多，这个值就是false
            totalShuffle.a(this.collageItems, new TCRect(0.0d, 0.0d, 1.0d, 1.0d));
        }
    }


    private TCCollageItem getEmptyItemOrNull() {
        List<TCCollageItem> list = getEmptyUUIDCollageItems();
        return list.size() > 0 ? list.get(0) : null;
    }


    private List<TCCollageItem> getEmptyUUIDCollageItems() {
        List<TCCollageItem> list = new ArrayList<>();
        for (TCCollageItem collageItem : this.collageItems) {
            if (collageItem.emptyUUID()) {
                list.add(collageItem);
            }
        }
        return list;
    }

    private int d() {
        int i;
        if (collageItems.size() > 0) {
            int i2 = 0;
            int i3 = 1;
            double z = collageItems.get(0).getRatioMaxBound(this.width, this.height);
            while (true) {
                i = i2;
                if (i3 >= collageItems.size()) {
                    break;
                }
                double a = collageItems.get(i3).getRatioMaxBound(this.width, this.height);
                double z2 = z;
                if (a > z) {
                    z2 = a;
                    i2 = i3;
                }
                i3++;
                z = z2;
            }
        } else {
            i = -1;
        }
        return i;
    }

    private TCRect a(int i) {
        TCRect tcRect;
        TCCollageItem item = this.collageItems.get(i);
        double nextInt = 0.4d + (new Random().nextInt(20) / 100.0d);
        TCRect iVar = item.ratioRect;
        if (item.ratioRect.right * this.width < item.ratioRect.bottom * this.height) {
            item.ratioRect = new TCRect(iVar.left, iVar.top, iVar.right, iVar.bottom * nextInt);
            tcRect = new TCRect(iVar.left, iVar.top + (iVar.bottom * nextInt), iVar.right, (1.0 - nextInt) * iVar.bottom);
        } else {
            item.ratioRect = new TCRect(iVar.left, iVar.top, iVar.right * nextInt, iVar.bottom);
            tcRect = new TCRect(iVar.left + (iVar.right * nextInt), iVar.top, (1.00 - nextInt) * iVar.right, iVar.bottom);
        }
        return tcRect;
    }


    public interface OnResultListener {
        void onSuccess(TCResult mResult);
        void onError(Exception e);
    }

}
