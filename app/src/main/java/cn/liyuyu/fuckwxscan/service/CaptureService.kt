package cn.liyuyu.fuckwxscan.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import cn.liyuyu.fuckwxscan.App
import cn.liyuyu.fuckwxscan.R
import cn.liyuyu.fuckwxscan.ui.MainActivity
import cn.liyuyu.fuckwxscan.utils.BarcodeUtil
import cn.liyuyu.fuckwxscan.utils.BarcodeUtil.toBarcodeResult
import cn.liyuyu.fuckwxscan.utils.ScreenUtil
import kotlinx.coroutines.*


/**
 * Created by frank on 2022/10/17.
 */
@SuppressLint("WrongConstant")
class CaptureService : Service(), CoroutineScope by MainScope() {

    private val mediaProjectionManager: MediaProjectionManager by lazy {
        getSystemService(
            MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
    }
    private val imageReader by lazy {
        val (width, height) = ScreenUtil.getScreenSize(this)
        ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            1
        )
    }
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val foregroundService: ForegroundNotification by lazy {
        ForegroundNotification(this)
    }

    private fun startCapture() {
        if (App.screenCaptureIntentResult == null) {
            return
        }
        if (mediaProjection == null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(
                Activity.RESULT_OK,
                App.screenCaptureIntentResult!!
            )
        }
        if (mediaProjection == null) {
            return
        }
        val (screenWidth, screenHeight) = ScreenUtil.getScreenSize(this)
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, ScreenUtil.getScreenDensityDpi(),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )
        launch(Dispatchers.IO) {
            val image = withTimeoutOrNull(1000) {
                var latestImage = imageReader.acquireLatestImage()
                while (latestImage == null) {
                    latestImage = imageReader.acquireLatestImage()
                }
                return@withTimeoutOrNull latestImage
            } ?: return@launch
            val bitmap = BarcodeUtil.imageToBitmap(image)
            val result = withTimeoutOrNull(2000) { BarcodeUtil.decodeQRCode(bitmap) }
            if (result != null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CaptureService, result.text, Toast.LENGTH_LONG).show()
                    val intent = Intent(this@CaptureService, MainActivity::class.java)
                    intent.putExtra("url", result.text)
                    intent.putExtra("results", arrayOf(result.toBarcodeResult()))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CaptureService, "未识别到二维码", Toast.LENGTH_LONG).show()
                }
            }
            // stop
            virtualDisplay?.release()
            virtualDisplay = null
            this@CaptureService.stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        foregroundService.startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (null == intent) {
            return START_NOT_STICKY
        }
        foregroundService.startForegroundNotification()
        startCapture()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        foregroundService.stopForegroundNotification()
        super.onDestroy()
    }
}

class ForegroundNotification(private val service: CaptureService) :
    ContextWrapper(service) {

    companion object {
        private const val START_ID = 2141
        private const val CHANNEL_ID = "fuck_wx_foreground_service"
        private const val CHANNEL_NAME = "扫你码服务"
    }

    private var mNotificationManager: NotificationManager? = null

    private var mCompatBuilder: NotificationCompat.Builder? = null

    private val compatBuilder: NotificationCompat.Builder?
        get() {
            if (mCompatBuilder == null) {
                val notificationIntent = Intent(this, MainActivity::class.java)
                notificationIntent.action = Intent.ACTION_MAIN
                notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                notificationIntent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getActivity(
                        this,
                        2140,
                        notificationIntent,
                        PendingIntent.FLAG_MUTABLE
                    )
                } else {
                    PendingIntent.getActivity(
                        this,
                        2140,
                        notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

                val notificationBuilder: NotificationCompat.Builder =
                    NotificationCompat.Builder(this, CHANNEL_ID)
                notificationBuilder.setContentTitle(getString(R.string.notification_title))
                notificationBuilder.setContentText(getString(R.string.notification_sub_title))
                notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
                notificationBuilder.setContentIntent(pendingIntent)
                mCompatBuilder = notificationBuilder
            }
            return mCompatBuilder
        }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.setShowBadge(false)
            mNotificationManager?.createNotificationChannel(channel)
        }
    }

    fun startForegroundNotification() {
        service.startForeground(START_ID, compatBuilder?.build())
    }

    fun stopForegroundNotification() {
        mNotificationManager?.cancelAll()
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }
}