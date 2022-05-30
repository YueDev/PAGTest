package com.hack.turbo_collage;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TCUtils {

    private static final Random a = new Random();

    public static <T> List<T> randomList(List<T> list) {
        List<T> result = new ArrayList<>(list);
        Collections.shuffle(result);
        return result;
    }

    public static boolean randomBoolean() {
        return a.nextBoolean();
    }

}