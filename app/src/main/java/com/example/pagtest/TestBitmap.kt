package com.example.pagtest

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import java.util.UUID

/**
 * Created by Yue on 2022/5/28.
 */
data class TestBitmap(
    val bitmap:Bitmap,
    val matrix: Matrix = Matrix(),
    val uuid:String = UUID.randomUUID().toString(),
    val rect:RectF = RectF()
)