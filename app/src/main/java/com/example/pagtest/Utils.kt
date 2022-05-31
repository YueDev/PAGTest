package com.example.pagtest

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.sangcomz.fishbun.FishBun
import com.sangcomz.fishbun.adapter.image.impl.GlideAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val emptyBitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

fun toGallery(activity: Activity, selectNum: Int, activityResultLauncher: ActivityResultLauncher<Intent>) {
    FishBun
        .with(activity)
        .setImageAdapter(GlideAdapter())
        .setMaxCount(selectNum)
        .setActionBarColor(
            ResourcesCompat.getColor(activity.resources, R.color.colorPrimary, null),
            ResourcesCompat.getColor(activity.resources, R.color.colorPrimaryDark, null)
        )
        .setActionBarTitle("选择图片")
        .startAlbumWithActivityResultCallback(activityResultLauncher)
}

fun toGallery(fragment: Fragment, selectNum: Int, activityResultLauncher: ActivityResultLauncher<Intent>) {
    FishBun
        .with(fragment)
        .setImageAdapter(GlideAdapter())
        .setMaxCount(selectNum)
        .setActionBarColor(
            ResourcesCompat.getColor(fragment.resources, R.color.colorPrimary, null),
            ResourcesCompat.getColor(fragment.resources, R.color.colorPrimaryDark, null)
        )
        .startAlbumWithActivityResultCallback(activityResultLauncher)
}


//rv的性能 glide好一些
//单纯加载bitmap coil glide差不多，都很快，coil有原生协程，glide需要自己开作用域
suspend fun getBitmapFromUri(context: Context, uri: Uri): Bitmap {
    //Coil
    val request = ImageRequest.Builder(context)
        .data(uri)
        .allowConversionToBitmap(true)
        .size(1200)
        .scale(Scale.FIT)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .diskCachePolicy(CachePolicy.DISABLED)
        .allowHardware(false)
        .build()
    val drawable = context.imageLoader.execute(request).drawable ?: throw Exception("drawable is null!")
    return (drawable as BitmapDrawable).bitmap
}


fun dp2px(dpValue: Float): Int {
    val scale: Float = Resources.getSystem().displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
}


const val IMAGE_MAX_SIZE = 1920

//rv的性能 glide好一些
//单纯加载bitmap coil glide差不多，都很快，coil有原生协程，glide需要自己开作用域
@JvmName("getBitmapFromUri1")
suspend fun Activity.getBitmapFromUri(uri: Uri): Bitmap {
    //Coil
    val request = ImageRequest.Builder(this)
        .data(uri)
        .allowConversionToBitmap(true)
        .size(IMAGE_MAX_SIZE)
        .scale(Scale.FIT)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .diskCachePolicy(CachePolicy.DISABLED)
        .allowHardware(false)
        .build()
    val drawable = imageLoader.execute(request).drawable ?: throw Exception("drawable is null!")
    return (drawable as BitmapDrawable).bitmap
}

@JvmName("getBitmapWithSize1")
suspend fun Activity.getBitmapWithSize(uri: Uri, max: Int): Bitmap {
    //Coil
    val request = ImageRequest.Builder(this)
        .data(uri)
        .allowConversionToBitmap(true)
        .size(max)
        .scale(Scale.FIT)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .diskCachePolicy(CachePolicy.DISABLED)
        .allowHardware(false)
        .build()
    val drawable = imageLoader.execute(request).drawable ?: throw Exception("drawable is null!")
    return (drawable as BitmapDrawable).bitmap
}

//根据uri获取文件名
@JvmName("getFileNameFromUri1")
suspend fun Context.getFileNameFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
    when (uri.scheme) {
        ContentResolver.SCHEME_FILE -> uri.toFile().name
        ContentResolver.SCHEME_CONTENT -> {
            val cursor = contentResolver.query(uri, null, null, null, null, null)
            cursor.use {
                it ?: return@use getDefaultName(uri)
                it.moveToFirst()
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index < 0) getDefaultName(uri) else it.getString(index)
            }
        }
        else -> getDefaultName(uri)
    }
}

fun Context.getDefaultName(uri: Uri) =
    "${System.currentTimeMillis()}.${MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri))}"

private val colors = listOf(
    Color.parseColor("#f8bbd0"),
    Color.parseColor("#f06292"),
    Color.parseColor("#d81b60"),
    Color.parseColor("#9575cd"),
    Color.parseColor("#512da8"),
    Color.parseColor("#64b5f6"),
    Color.parseColor("#0097a7"),
    Color.parseColor("#006064"),
    Color.parseColor("#cddc39"),
    Color.parseColor("#f9a825"),
    Color.parseColor("#d84315"),
    Color.parseColor("#9e9e9e"),
    Color.parseColor("#607d8b"),
    Color.parseColor("#651fff"),
)