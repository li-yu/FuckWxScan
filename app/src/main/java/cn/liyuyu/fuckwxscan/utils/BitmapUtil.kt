package cn.liyuyu.fuckwxscan.utils

import android.graphics.*
import android.media.Image
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.nio.ByteBuffer


/**
 * Created by frank on 2022/10/17.
 */
object BitmapUtil {

    fun imageToBitmap(image: Image): Bitmap {
        val width = image.width
        val height = image.height
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width
        val bitmap =
            Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()
        return bitmap
    }

    fun decodeQRCode(bitmap: Bitmap, isRecursive: Boolean = true): Result? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        var binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val hint = HashMap<DecodeHintType, Any>()
        hint[DecodeHintType.TRY_HARDER] = true
        hint[DecodeHintType.CHARACTER_SET] = "UTF-8"
        hint[DecodeHintType.POSSIBLE_FORMATS] = listOf(
            BarcodeFormat.QR_CODE
        )
        val reader = QRCodeReader()
        var result: Result? = null
        try {
            result = reader.decode(binaryBitmap, hint)
        } catch (e: Exception) {
            //ignore
        }
        if (result == null) {
            try {
                binaryBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
                result = reader.decode(binaryBitmap, hint)
            } catch (e: Exception) {
                //ignore
            }
        }
        if (result == null && isRecursive) {
            val newBitmap = changeBitmapContrastBrightness(bitmap, 1f, -100f)
            return decodeQRCode(newBitmap, false)
        }
        return result
    }

    /**
     *
     * @param bmp input bitmap
     * @param contrast 0..10 1 is default
     * @param brightness -255..255 0 is default
     * @return new bitmap
     */
    fun changeBitmapContrastBrightness(
        bmp: Bitmap,
        contrast: Float,
        brightness: Float
    ): Bitmap {
        val cm = ColorMatrix(
            floatArrayOf(
                contrast,
                0f,
                0f,
                0f,
                brightness,
                0f,
                contrast,
                0f,
                0f,
                brightness,
                0f,
                0f,
                contrast,
                0f,
                brightness,
                0f,
                0f,
                0f,
                1f,
                0f
            )
        )
        val ret = Bitmap.createBitmap(bmp.width, bmp.height, bmp.config)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        Canvas(ret).drawBitmap(bmp, 0f, 0f, paint)
        return ret
    }
}