package net.ib.mn.utils.ext

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.internal.managers.ViewComponentManager.FragmentContextWrapper

/**
 * Hilt 사용시 FragmentContextWrapper인 경우 대비하여 안전하게 activity를 가져온다
 * */
val Context.safeActivity: Activity?
    get() = when {
        this is Activity -> this
        this is ContextWrapper -> {
            val baseContext = this.baseContext
            if (baseContext is Activity) baseContext
            else if (baseContext is ContextWrapper) baseContext.safeActivity
            else null
        }
        else -> null
    }

// 필잇 임시 처리 용
fun Context.openAppOrStore(
    targetPackage: String = "com.exodus.fillit",
    fallbackStoreUrl: String = "https://play.google.com/store/apps/details?id=com.exodus.fillit"
) {
    val pm = packageManager
    val launch = pm.getLaunchIntentForPackage(targetPackage)
    if (launch != null) {
        // 필요하면 딥링크/파라미터 추가
        // launch.data = Uri.parse("myapp://artist?ref=web")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)
    } else {
        // Play 스토어로 폴백
        runCatching {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$targetPackage")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(fallbackStoreUrl)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
