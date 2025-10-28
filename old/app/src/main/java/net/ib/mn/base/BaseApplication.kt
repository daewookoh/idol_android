package net.ib.mn.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import androidx.multidex.MultiDex
import com.appsflyer.AppsFlyerLib
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.facebook.stetho.Stetho
import dagger.hilt.android.HiltAndroidApp
import net.ib.mn.BuildConfig
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util


@HiltAndroidApp
class BaseApplication : Application(), Application.ActivityLifecycleCallbacks {
    private var activityCount = 0
    private val activityInventory = arrayListOf<Activity>()

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var appContext: Context
        val TAG = if(BuildConfig.CELEB) "com.exodus.myloveactor" else "net.ib.mn"
        //debug 가능 여부 -> default 값은  false
        var DEBUG_AVAILABLE:Boolean=false
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)

        //debug 가능 여부 넣어줌.
        DEBUG_AVAILABLE=isDebuggable(this)
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }
        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }

        var host = Util.getPreference(appContext, Const.PREF_SERVER_URL)
        if (!host.isNullOrEmpty()) {
            // retrofit
            ServerUrl.HOST = host
            Logger.w("ServerUrl.HOST", ServerUrl.HOST)
        }

        AppsFlyerLib.getInstance().init(Const.APPS_FLYER_KEY, null, this)
        if (BuildConfig.DEBUG) {
            AppsFlyerLib.getInstance().setDebugLog(true)
        }
        AppsFlyerLib.getInstance().start(this, Const.APPS_FLYER_KEY, object : AppsFlyerRequestListener {

            override fun onSuccess() {
                Util.log("AppsFlyer init success: ")
            }

            override fun onError(p0: Int, p1: String) {
                Util.log("AppsFlyer init fail: $p0 $p1")
            }
        });

        registerActivityLifecycleCallbacks(this)

        configureBuildSpecificSettings()
    }

    //debug 가능 여부를  체크해준다. (logger 안보이게 할려고)
    //release 버전에서는  false 로 체크된다.
    fun isDebuggable(context: Context): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityCreated(activity: Activity, p1: Bundle?) {
        if (isExternalSdkActivity(activity)) {
            return
        }
        activityCount++
        activityInventory.add(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (isExternalSdkActivity(activity)) {
            return
        }
        activityCount--
        activityInventory.remove(activity)
    }

    private fun isExternalSdkActivity(obj: Any): Boolean {
        return try {
            val className = obj.javaClass.name
            // 앱의 패키지 네임으로 설정하게되면 셀럽은 com.exodus.myloveactor 이런식으로 계속 바뀌므로 고정해줌.
            val appPackageName = "net.ib.mn"
            !className.startsWith(appPackageName)
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }

    fun getActivityCount(): Int = activityCount

    fun getTopActivity(): Activity? = activityInventory.lastOrNull()
}
