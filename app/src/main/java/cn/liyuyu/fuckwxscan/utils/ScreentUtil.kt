package cn.liyuyu.fuckwxscan.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.os.Build.VERSION
import android.view.WindowManager

/**
 * Created by frank on 2022/10/21.
 */
object ScreenUtil {

    fun getScreenSize(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        if (VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.currentWindowMetrics.bounds.let {
                point.x = it.width()
                point.y = it.height()
            }
        } else {
            @Suppress("DEPRECATION") wm.defaultDisplay.getRealSize(point)
        }
        return Pair(point.x, point.y)
    }

    @SuppressLint("DiscouragedApi")
    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun getScreenDensityDpi() = Resources.getSystem().displayMetrics.densityDpi
}