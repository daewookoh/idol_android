package net.ib.mn.base

import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.api.init.PAGConfig
import com.bytedance.sdk.openadsdk.api.init.PAGSdk
import com.bytedance.sdk.openadsdk.api.init.PAGSdk.PAGInitCallback
import net.ib.mn.R
import net.ib.mn.utils.Util

fun BaseApplication.configureBuildSpecificSettings() {
    val pAGInitConfig = PAGConfig.Builder()
        .appId("5161370")
        .appIcon(R.mipmap.ic_launcher)
        .useTextureView(true) //默认使用SurfaceView播放视频广告,当有SurfaceView冲突的场景，可以使用TextureView
        .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK) //落地页主题(페이지 테마)
        //    .debug(true) //测试阶段打开，可以通过日志排查问题，上线时去除该调用(테스트할땐 주석제거)
        .supportMultiProcess(false) //是否支持多进程，true支持(다중프로세스지원)
        //.httpStack(new MyOkStack3())//自定义网络库，demo中给出了okhttp3版本的样例，其余请自行开发或者咨询工作人员。
        .build()

    PAGSdk.init(this, pAGInitConfig, object : PAGInitCallback {
        override fun success() {
            //load pangle ads after this method is triggered.
            Util.log("pangle init success: ")
        }

        override fun fail(code: Int, msg: String) {
            Util.log("pangle init fail: $code")
        }
    })
}