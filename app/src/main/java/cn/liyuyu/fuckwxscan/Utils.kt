package cn.liyuyu.fuckwxscan

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Point
import android.media.Image
import android.view.WindowManager
import com.google.zxing.Result
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import java.nio.ByteBuffer

/**
 * Created by frank on 2022/10/17.
 */
object Utils {

    fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)
        return point.x
    }

    fun getScreenHeight(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)
        return point.y
    }

    fun getScreenDensityDpi(): Int {
        return Resources.getSystem().displayMetrics.densityDpi
    }

    fun imageToBitmap(image: Image): Bitmap {
        val width = image.width
        val height = image.height
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width
        var bitmap =
            Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()
        return bitmap
    }

    fun decodeQRCode(bitmap: Bitmap): Array<out Result>? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
        val binaryBitmap =
            com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
        val hint = java.util.HashMap<com.google.zxing.DecodeHintType, Any>()
        hint[com.google.zxing.DecodeHintType.TRY_HARDER] = true
        return try {
            val reader = QRCodeMultiReader()
            reader.decodeMultiple(binaryBitmap, hint)
        } catch (e: Exception) {
            null
        }
    }
}