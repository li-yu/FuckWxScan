package cn.liyuyu.fuckwxscan

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
        ImageReader.newInstance(
            Utils.getScreenWidth(this),
            Utils.getScreenHeight(this),
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
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "ScreenCapture",
            Utils.getScreenWidth(this), Utils.getScreenHeight(this), Utils.getScreenDensityDpi(),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )
        launch(Dispatchers.IO) {
            var image = imageReader.acquireLatestImage()
            while (image == null) {
                image = imageReader.acquireLatestImage()
            }
            val bitmap = Utils.imageToBitmap(image)
            val result = Utils.decodeQRCode(bitmap)
            if (result != null && result.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CaptureService, result[0].text, Toast.LENGTH_LONG).show()
                    val intent = Intent(this@CaptureService, MainActivity::class.java)
                    intent.putExtra("url", result[0].text)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
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
                var pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getActivity(
                        this,
                        2140,
                        notificationIntent,
                        PendingIntent.FLAG_MUTABLE
                    )
                } else {
                    PendingIntent.getActivity(
                        this, 2140,
                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
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