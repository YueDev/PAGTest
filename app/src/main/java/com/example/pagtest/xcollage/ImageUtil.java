package com.example.pagtest.xcollage;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

/**
 * Created by Yue on 2021/8/16.
 */
public class ImageUtil {

    //加载bitmap
    //要跳过内存缓存：有些操作会调用bitmap.recycle()，如果开启内存缓存，会出现bitmap recycled error
    @UiThread
    public static void loadBitmapFromGlide(Context context, Uri uri, int width, int height, SimpleGlideListener listener) {

        Glide.with(context).asBitmap()
                .load(uri)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerInside()
                .addListener(new RequestListener<>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        Exception exception = e;
                        if (exception == null) exception = new Exception("unknown error!");
                        exception.printStackTrace();
                        listener.onError(exception.getMessage());
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        if (resource == null) {
                            listener.onError("error: bitmap is null!");
                            return true;
                        }
                        listener.onSuccess(resource);
                        return true;
                    }
                })
                .preload(width, height);
    }


    public interface SimpleGlideListener {
        void onSuccess(Bitmap bitmap);

        void onError(String errorMessage);
    }


}
