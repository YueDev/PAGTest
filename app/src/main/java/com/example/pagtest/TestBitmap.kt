package com.example.pagtest

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import java.util.*

/**
 * Created by Yue on 2022/5/28.
 */
data class TestBitmap(
    val uri: Uri,
    val bitmap: Bitmap,
    val matrix: Matrix = Matrix(),
    val uuid: String = UUID.randomUUID().toString(),
    val rect: RectF = RectF(),
    val tempRect: RectF = RectF()
)