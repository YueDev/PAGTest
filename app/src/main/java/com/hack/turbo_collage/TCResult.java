package com.hack.turbo_collage;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yue on 2022/5/28.
 */
public class TCResult {

    private final Map<String, TCRectF> out = new HashMap<>();

    public void add(String uuid, TCRectF tcRectF) {
        out.put(uuid, tcRectF);
    }

    public TCRectF get(String uuid) {
        return out.get(uuid);
    }

}
