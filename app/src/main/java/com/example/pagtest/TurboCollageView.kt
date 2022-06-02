package com.example.pagtest

import android.animation.ObjectAnimator
import android.animation.TimeAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import com.hack.turbo_collage.TCBitmap
import com.hack.turbo_collage.TCCollage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log


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

    private val animator = ObjectAnimator.ofFloat(0f, 100f).apply {

        duration = 2500
        addUpdateListener { ani ->
            val progress = ani.animatedFraction
            bitmaps.forEach {
                val l = it.rect.left + (it.tempRect.left - it.rect.left) * progress
                val t = it.rect.top + (it.tempRect.top - it.rect.top) * progress
                val r = it.rect.right + (it.tempRect.right - it.rect.right) * progress
                val b = it.rect.bottom + (it.tempRect.bottom - it.rect.bottom) * progress
                it.rect.set(l, t, r, b)
                when (type) {
                    Type.FIT_CENTER -> fitCenter(it.bitmap, it.rect, it.matrix)
                    Type.CENTER_CROP -> centerCrop(it.bitmap, it.rect, it.matrix)
                }
            }
            postInvalidateOnAnimation()
        }
    }


    fun setBitmaps(testBitmaps: List<TestBitmap>, mainScope: CoroutineScope) {
        post {
            bitmaps.clear()
            bitmaps.addAll(testBitmaps)
            val tcBitmaps = bitmaps.map {
                TCBitmap(it.uuid, it.bitmap.width, it.bitmap.height)
            }
            collage.init(tcBitmaps)
            canDraw = true
            mainScope.launch {
                collage()
            }
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

    @UiThread
    suspend fun collage() {
        val result = withContext(Dispatchers.Default) {
            collage.collage(measuredWidth.toDouble(), measuredHeight.toDouble(), 0.0)
        }

        result?.let {
            bitmaps.forEach { tcBitmap ->
                val tcRect = it.get(tcBitmap.uuid)
                tcBitmap.rect.apply {
                    left = tcRect.left
                    top = tcRect.top
                    right = tcRect.right
                    bottom = tcRect.bottom
                }
                when (type) {
                    Type.FIT_CENTER -> fitCenter(tcBitmap.bitmap, tcBitmap.rect, tcBitmap.matrix)
                    Type.CENTER_CROP -> centerCrop(tcBitmap.bitmap, tcBitmap.rect, tcBitmap.matrix)
                }
            }
            invalidate()
        }
    }


    @UiThread
    suspend fun changeType() {
        type = if (type == Type.FIT_CENTER) Type.CENTER_CROP else Type.FIT_CENTER
        collage()
    }

    private fun centerCrop(bitmap: Bitmap, rect: RectF, matrix:Matrix)  {

        val scale = (rect.width() / bitmap.width).coerceAtLeast(rect.height() / bitmap.height)
        val dx = rect.centerX() - bitmap.width / 2f
        val dy = rect.centerY() - bitmap.height / 2f

        matrix.reset()
        matrix.postTranslate(dx, dy)
        matrix.postScale(scale, scale, rect.centerX(), rect.centerY())
    }

    private fun fitCenter(bitmap: Bitmap, rect: RectF, matrix:Matrix) {

        val scale = (rect.width() / bitmap.width).coerceAtMost(rect.height() / bitmap.height)
        val dx = rect.centerX() - bitmap.width / 2f
        val dy = rect.centerY() - bitmap.height / 2f

        matrix.reset()
        matrix.postTranslate(dx, dy)
        matrix.postScale(scale, scale, rect.centerX(), rect.centerY())
    }


    @UiThread
    suspend fun animateCollage() {
        val result = withContext(Dispatchers.Default) {
            collage.collage(measuredWidth.toDouble(), measuredHeight.toDouble(), 0.0)
        }

        result?.let {

            bitmaps.forEach { tcBitmap ->
                val tcRect = it.get(tcBitmap.uuid)
                tcBitmap.tempRect.apply {
                    left = tcRect.left
                    top = tcRect.top
                    right = tcRect.right
                    bottom = tcRect.bottom
                }
            }

            if (animator.isStarted) animator.cancel()
            animator.start()

        }
    }

    @UiThread
    suspend fun getHighResBitmap(scale: Float) = withContext(Dispatchers.Default) {
        if (measuredWidth == 0 || measuredHeight == 0) return@withContext null
        val result = Bitmap.createBitmap((measuredWidth * scale).toInt(), (measuredHeight * scale).toInt(), Bitmap.Config.RGB_565)

        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        canvas.save()

        bitmaps.forEach {

            it.tempRect.left = it.rect.left * scale
            it.tempRect.top = it.rect.top * scale
            it.tempRect.right = it.rect.right * scale
            it.tempRect.bottom = it.rect.bottom * scale


            val w = it.rect.width() * scale
            val h = it.rect.height() * scale
            val bitmap = context.getBitmapWithSize(it.uri, w.toInt(), h.toInt(), true)

            val matrix = Matrix()

            when (type) {
                Type.FIT_CENTER -> fitCenter(bitmap, it.tempRect, matrix)
                Type.CENTER_CROP -> centerCrop(bitmap, it.tempRect, matrix)
            }

            canvas.drawBitmap(bitmap, matrix, paint)
        }
        result
    }


}