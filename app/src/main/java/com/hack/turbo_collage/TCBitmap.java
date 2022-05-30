package com.hack.turbo_collage;

/**
 * Created by Yue on 2022/5/28.
 */
public class TCBitmap {

    private final String uuid;
    private final int width;
    private final int height;

    public TCBitmap(String uuid, int width, int height) {
        this.uuid = uuid;
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getUUID() {
        return this.uuid;
    }
}
