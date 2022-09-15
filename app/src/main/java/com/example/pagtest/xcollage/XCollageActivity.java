package com.example.pagtest.xcollage;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.lights.LightState;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.pagtest.databinding.ActivityCollageBinding;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class XCollageActivity extends AppCompatActivity {

    private ActivityCollageBinding mBinding;

    private static final String KEY_X_COLLAGE_ACTIVITY_IMAGES = "key_x_collage_activity_images";

    private List<Uri> mUriList;

    private int mSize = 1920;

    private List<Bitmap> mBitmaps = new ArrayList<>();

    public static void startNewInstance(Context context, ArrayList<Uri> imageUris) {
        Intent intent = new Intent(context, XCollageActivity.class);
        intent.putParcelableArrayListExtra(KEY_X_COLLAGE_ACTIVITY_IMAGES, imageUris);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("XCollage");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0f);
        }

        mUriList = getIntent().getParcelableArrayListExtra(KEY_X_COLLAGE_ACTIVITY_IMAGES);
        if (mUriList == null || mUriList.size() == 0) return;

        mBinding = ActivityCollageBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mBinding.progressBar2.setVisibility(View.VISIBLE);

        mSize = (int) Math.min(Math.sqrt(2048.0 * 2048.0 / mUriList.size()), 1920.0);

        loadImage(mUriList, 0, mUriList.size());

    }

    private void initView() {

        mBinding.collageView.setBitmaps(mBitmaps);

        //刷新
        mBinding.buttonRefresh.setOnClickListener((v) -> mBinding.collageView.collage());

        //16:9
        mBinding.button2.setOnClickListener((v) -> {});
        //4:3
        mBinding.button3.setOnClickListener((v) -> {});
        //3:4
        mBinding.button4.setOnClickListener ((v) -> {});
        //1:1
        mBinding.button5.setOnClickListener((v) -> {});
        //9:16
        mBinding.button6.setOnClickListener((v) -> {});

        //无边框
        mBinding.buttonNone.setOnClickListener((v) -> {});
        //小
        mBinding.buttonNone.setOnClickListener((v) -> {});
        //中
        mBinding.buttonNone.setOnClickListener((v) -> {});
        //大
        mBinding.buttonNone.setOnClickListener((v) -> {});


        mBinding.progressBar2.setVisibility(View.GONE);
    }

    private void loadImage(List<Uri> uris, int index, int sum) {
        if (index >= sum) {
            initView();
            return;
        }

        ImageUtil.loadBitmapFromGlide(this, uris.get(index), mSize, mSize, new ImageUtil.SimpleGlideListener() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                mBitmaps.add(bitmap);
                loadImage(uris, index + 1, sum);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(XCollageActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }


}