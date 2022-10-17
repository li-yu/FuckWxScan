package cn.liyuyu.fuckwxscan

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra("url")
        if (!url.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "没有找到可处理的应用", Toast.LENGTH_LONG).show()
            }
            finish()
            return
        }

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
                Toast.makeText(this, "没有权限，将无法识别二维码哦~", Toast.LENGTH_SHORT).show()
            }
            this.finish()
        }.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}