package cn.liyuyu.fuckwxscan.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Created by frank on 2022/10/21.
 */
@Parcelize
data class BarcodeResult(val text: String, val centerX: Float, val centerY: Float) :
    Parcelable
