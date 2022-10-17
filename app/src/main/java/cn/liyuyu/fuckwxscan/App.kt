package cn.liyuyu.fuckwxscan

import android.app.Application
import android.content.Intent

/**
 * Created by frank on 2022/10/17.
 */
class App : Application() {

    companion object {
        var screenCaptureIntentResult: Intent? = null
    }
}