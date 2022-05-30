package com.example.pagtest

import android.graphics.Bitmap
import java.io.Serializable

//除以1000 化成毫秒
const val MAX_END_TIME = Long.MAX_VALUE / 1_000L

data class TemImageBean(
    val pagIndex: Int,
    val width: Int,
    val height: Int,
    val time: Long,
    val bitmap: Bitmap
)

data class ImageBean(
    val pagIndex: Int, //模版顺序
    val width: Int,
    val height: Int,
    val time: Long,  //毫秒
    val startTime: Long,  //显示时间，毫秒，在这个时间开始显示
    val endTime: Long// 显示时间，毫秒， 在这个时间结束显示  [startTime endTime)
) : Serializable


