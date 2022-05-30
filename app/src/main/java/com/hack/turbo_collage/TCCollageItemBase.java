package com.hack.turbo_collage;

public class TCCollageItemBase {

    String uuid;

    public TCCollageItemBase(String str) {
        this.uuid = str;
    }

    public boolean emptyUUID() {
        return this.uuid == null || this.uuid.trim().length() <= 0;
    }
}