package net.ib.mn.base

import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.TTAdSdk
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.base.BaseApplication.Companion.appContext
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

fun BaseApplication.configureBuildSpecificSettings() {
    initAcra {
        buildConfigClass = BuildConfig::class.java
        reportFormat = StringFormat.JSON
        mailSender {
            mailTo = "exodus.acra@gmail.com"
            body = "Sorry, CHOEAEDOL has crashed. Please send this crash information to us."
        }
    }

    //pangle application에서 초기화 하는게 좋다고함.
    TTAdSdk.init(this,
        TTAdConfig.Builder()
            .appId("5136091")
            .useTextureView(true) //默认使用SurfaceView播放视频广告,当有SurfaceView冲突的场景，可以使用TextureView
            .appName(appContext.getString(R.string.choeaedol))
            .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK) //落地页主题(페이지 테마)
            .allowShowNotify(true) //是否允许sdk展示通知栏提示
            //.debug(true) //测试阶段打开，可以通过日志排查问题，上线时去除该调用(테스트할땐 주석제거)
            .supportMultiProcess(false) //是否支持多进程，true支持(다중프로세스지원)
            // .asyncInit(true) //是否异步初始化sdk,设置为true可以减少SDK初始化耗时(sdk 비동기 초기화) // 3450 버전부터 삭제됨
            //.httpStack(new MyOkStack3())//自定义网络库，demo中给出了okhttp3版本的样例，其余请自行开发或者咨询工作人员。
            .build())
}