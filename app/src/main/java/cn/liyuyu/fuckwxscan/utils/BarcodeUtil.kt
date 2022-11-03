package cn.liyuyu.fuckwxscan.utils

import android.content.Context
import android.graphics.*
import android.media.Image
import android.net.Uri
import androidx.core.content.FileProvider
import cn.liyuyu.fuckwxscan.data.ResultType
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


/**
 * Created by frank on 2022/10/17.
 */
object BarcodeUtil {

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

    fun decodeQRCode(bitmap: Bitmap, isRecursive: Boolean = true): Array<Result>? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        var binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val hint = HashMap<DecodeHintType, Any>()
        hint[DecodeHintType.TRY_HARDER] = true
        hint[DecodeHintType.CHARACTER_SET] = "UTF-8"
        hint[DecodeHintType.POSSIBLE_FORMATS] = BarcodeFormat.QR_CODE

        val reader = QRCodeMultiReader()
        var result: Array<Result>? = null
        try {
            result = reader.decodeMultiple(binaryBitmap, hint)
        } catch (e: Exception) {
            //ignore
        }
        if (result == null) {
            try {
                binaryBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
                result = reader.decodeMultiple(binaryBitmap, hint)
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
    private fun changeBitmapContrastBrightness(
        bmp: Bitmap, contrast: Float, brightness: Float
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

    fun getBitmapUri(bitmap: Bitmap, context: Context): Uri? {
        val file = File(context.getExternalFilesDir(null), "screenShot.jpg")
        if (file.exists()) {
            file.delete()
        }
        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            return if (file.exists()) {
                FileProvider.getUriForFile(
                    context, context.packageName + ".provider", file
                )
            } else {
                null
            }
        }
    }

    fun getResultType(text: String): ResultType {
        val regex = "^(http(s?)|):\\/\\/(.+)\$".toRegex()
        val isUrl = regex.matches(text)
        return if (isUrl) {
            if (Regex("(?=(weixin.qq|wechat).com)").containsMatchIn(text)) {
                ResultType.WeChatUrl
            } else if (Regex("(?=(alipay|taobao|tb).(com|cn))").containsMatchIn(text)) {
                ResultType.AlipayUrl
            } else {
                ResultType.CommonUrl
            }
        } else {
            ResultType.PlainText
        }
    }
}