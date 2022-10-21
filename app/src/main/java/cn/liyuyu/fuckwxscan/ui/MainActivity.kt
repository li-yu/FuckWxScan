package cn.liyuyu.fuckwxscan.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cn.liyuyu.fuckwxscan.App
import cn.liyuyu.fuckwxscan.R
import cn.liyuyu.fuckwxscan.data.BarcodeResult
import cn.liyuyu.fuckwxscan.service.CaptureService
import cn.liyuyu.fuckwxscan.ui.theme.FuckWxScanTheme
import cn.liyuyu.fuckwxscan.ui.theme.HintMask
import cn.liyuyu.fuckwxscan.utils.ScreenUtil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url")
        val results = intent.getParcelableArrayExtra("results")
        if (!url.isNullOrEmpty()) {
            showHint(results)
            return
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "没有找到可处理的应用哎！", Toast.LENGTH_LONG).show()
            }
            finish()
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
                    Toast.makeText(this, "取消识别~", Toast.LENGTH_SHORT).show()
                }
                this.finish()
            }.launch(mediaProjectionManager.createScreenCaptureIntent())
        } else {
            startService(Intent(this, CaptureService::class.java))
            finish()
        }
    }

    private fun showHint(results: Array<Parcelable>?) {
        setContent {
            FuckWxScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = HintMask
                ) {
                    results?.let {
                        for (item in results) {
                            val result = item as BarcodeResult
                            Text(text = ".", modifier = Modifier
                                .absoluteOffset(
                                    with(LocalDensity.current) { result.centerX.toDp() },
                                    with(LocalDensity.current) {
                                        result.centerY.toDp()
                                    }
                                ))
                            Box(modifier = Modifier
                                .absoluteOffset(
                                    with(LocalDensity.current) { result.centerX.toDp() - 16.dp },
                                    with(LocalDensity.current) {
                                        (result.centerY - ScreenUtil.getStatusHeight(this@MainActivity)).toDp() - 16.dp
                                    }
                                )
                                .size(32.dp)) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_wait_click),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}