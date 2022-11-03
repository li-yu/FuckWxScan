package cn.liyuyu.fuckwxscan.utils

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import cn.liyuyu.fuckwxscan.data.BarcodeResult
import com.google.zxing.Result
import java.io.ByteArrayOutputStream

/**
 * Created by frank on 2022/11/3.
 */
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? = when {
    SDK_INT >= 33 -> getParcelableArrayList(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
}

inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? = when {
    SDK_INT >= 33 -> getParcelableArrayListExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
}

fun Result.toBarcodeResult(): BarcodeResult {
    val minX = resultPoints.minByOrNull { it.x }?.x ?: 0f
    val maxX = resultPoints.maxByOrNull { it.x }?.x ?: 0f
    val minY = resultPoints.minByOrNull { it.y }?.y ?: 0f
    val maxY = resultPoints.maxByOrNull { it.y }?.y ?: 0f
    return BarcodeResult(
        text = text, centerX = (minX + maxX) / 2, centerY = (minY + maxY) / 2
    )
}

fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 100, stream)
    return stream.toByteArray()
}

fun ByteArray.toBitmap(): Bitmap {
    return BitmapFactory.decodeByteArray(this, 0, this.size)
}