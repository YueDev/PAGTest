package com.example.pagtest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.hack.turbo_collage.TCBitmap
import com.hack.turbo_collage.TCCollage


/**
 * Created by Yue on 2022/5/28.
 */
class TurboCollageView : View {

    enum class Type {
        FIT_CENTER, CENTER_CROP
    }

    private var type = Type.CENTER_CROP

    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    private val collage = TCCollage()

    private val bitmaps = mutableListOf<TestBitmap>()

    private var canDraw = false

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setBitmaps(testBitmaps: List<TestBitmap>) {
        post {
            bitmaps.clear()
            bitmaps.addAll(testBitmaps)
            val tcBitmaps = bitmaps.map {
                TCBitmap(it.uuid, it.bitmap.width, it.bitmap.height)
            }
            collage.init(tcBitmaps)
            canDraw = true
            collage()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (!canDraw) return
        canvas?.let {
            bitmaps.forEach {
                canvas.save()
                canvas.clipRect(it.rect.left, it.rect.top, it.rect.right, it.rect.bottom)
                canvas.drawBitmap(it.bitmap, it.matrix, paint)
                canvas.restore()
            }
        }
    }

    fun collage() {
        val result = collage.collage(measuredWidth.toDouble(), measuredHeight.toDouble(), 0.0)

        bitmaps.forEach {
            val tcRect = result.get(it.uuid)
            it.rect.apply {
                left = tcRect.left
                top = tcRect.top
                right = tcRect.right
                bottom = tcRect.bottom
            }
            when (type) {
                Type.FIT_CENTER -> fitCenter(it)
                Type.CENTER_CROP -> centerCrop(it)
            }
        }

        invalidate()
    }


    fun changeType() {
        type = if (type == Type.FIT_CENTER) Type.CENTER_CROP else Type.FIT_CENTER
        collage()
    }

    private fun centerCrop(testBitmap: TestBitmap) {
        val matrix = testBitmap.matrix
        val bitmap = testBitmap.bitmap
        val rect = testBitmap.rect

        val scale = (rect.width() / bitmap.width).coerceAtLeast(rect.height() / bitmap.height)
        val dx = rect.centerX() - bitmap.width / 2f
        val dy = rect.centerY() - bitmap.height / 2f

        matrix.reset()
        matrix.postTranslate(dx, dy)
        matrix.postScale(scale, scale, rect.centerX(), rect.centerY())
    }

    private fun fitCenter(testBitmap: TestBitmap) {
        val matrix = testBitmap.matrix
        val bitmap = testBitmap.bitmap
        val rect = testBitmap.rect

        val scale = (rect.width() / bitmap.width).coerceAtMost(rect.height() / bitmap.height)
        val dx = rect.centerX() - bitmap.width / 2f
        val dy = rect.centerY() - bitmap.height / 2f

        matrix.reset()
        matrix.postTranslate(dx, dy)
        matrix.postScale(scale, scale, rect.centerX(), rect.centerY())
    }

}