package cn.liyuyu.fuckwxscan.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import cn.liyuyu.fuckwxscan.App
import cn.liyuyu.fuckwxscan.R
import cn.liyuyu.fuckwxscan.data.BarcodeResult
import cn.liyuyu.fuckwxscan.data.ResultType
import cn.liyuyu.fuckwxscan.service.CaptureService
import cn.liyuyu.fuckwxscan.ui.theme.FuckWxScanTheme
import cn.liyuyu.fuckwxscan.ui.theme.HintMask
import cn.liyuyu.fuckwxscan.utils.*

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BARCODE_RESULTS = "extra_barcode_results"
        const val EXTRA_BARCODE_BITMAP = "extra_barcode_bitmap"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        val results = intent.parcelableArrayList<BarcodeResult>(EXTRA_BARCODE_RESULTS)
        if (results != null && results.isNotEmpty()) {
            if (results.size == 1) {
                handleText(results[0].text)
                finish()
            } else {
                showHints(results)
            }
            return
        }
        if (App.screenCaptureIntentResult == null) {
            val mediaProjectionManager: MediaProjectionManager by lazy {
                getSystemService(
                    MEDIA_PROJECTION_SERVICE
                ) as MediaProjectionManager
            }
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    App.screenCaptureIntentResult = it.data
                    startService(Intent(this, CaptureService::class.java))
                } else {
                    App.screenCaptureIntentResult = null
                    showToast("取消识别~")
                }
                this.finish()
            }.launch(mediaProjectionManager.createScreenCaptureIntent())
        } else {
            startService(Intent(this, CaptureService::class.java))
            finish()
        }
    }

    private fun handleText(text: String) {
        showToast(text)
        val resultType = BarcodeUtil.getResultType(text)
        val bitmapUri = intent.parcelable<Uri>(EXTRA_BARCODE_BITMAP)
        if (resultType == ResultType.AlipayUrl) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, bitmapUri)
                setClassName(
                    "com.eg.android.AlipayGphone",
                    "com.alipay.mobile.quinox.splash.ShareScanQRDispenseActivity"
                )
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                showToast("没有找到[支付宝]哎！")
            }
        } else if (resultType == ResultType.WeChatUrl) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, bitmapUri)
                setClassName(
                    "com.tencent.mm",
                    "com.tencent.mm.ui.tools.ShareImgUI"
                )
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                showToast("没有找到[微信]哎！")
            }
        } else if (resultType == ResultType.CommonUrl) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(text))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                showToast("没有找到可处理的应用哎！")
            }
        }
    }

    private fun showHints(results: List<BarcodeResult>?) {
        setContent {
            FuckWxScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = HintMask
                ) {
                    Box {
                        val transition = rememberInfiniteTransition()
                        val currentSize by transition.animateValue(
                            28.dp, 40.dp, Dp.VectorConverter, infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 400, easing = LinearEasing
                                ), repeatMode = RepeatMode.Reverse
                            )
                        )
                        Text(text = "取消",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(
                                    x = (-16).dp,
                                    y = 16.dp + with(LocalDensity.current) {
                                        ScreenUtil
                                            .getStatusBarHeight(
                                                this@MainActivity
                                            )
                                            .toDp()
                                    }
                                )
                                .clickable {
                                    finish()
                                })
                        results?.let {
                            for (result in results) {
                                Box(
                                    modifier = Modifier
                                        .offset(with(LocalDensity.current) { result.centerX.toDp() - 18.dp },
                                            with(LocalDensity.current) {
                                                result.centerY.toDp() - 18.dp
                                            })
                                        .size(36.dp)
                                        .clickable {
                                            handleText(result.text)
                                            finish()
                                        }, contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_wait_click),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .size(currentSize)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}