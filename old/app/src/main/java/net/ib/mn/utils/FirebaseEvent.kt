package net.ib.mn.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase
import net.ib.mn.BuildConfig

fun setFirebaseUIAction(gaAction: GaAction) {
    if (BuildConfig.DEBUG) {
        Log.d("GaAction", "Action ${gaAction.label}")
        return
    }

    try {
        val firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(gaAction.label, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun setUiActionFirebaseGoogleAnalyticsActivity(context: Context, action: String?, label: String) {
    if (BuildConfig.DEBUG) {
        Log.d("GaAction", "Action $action - $label")
        return
    }

    if (BuildConfig.CHINA) {
        return
    }
    try {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        val params = Bundle()
        params.putString(Const.ANALYTICS_GA_DEFAULT_ACTION_KEY, action)
        firebaseAnalytics.logEvent(label, params)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun setFirebaseScreenViewEvent(gaAction: GaAction, className: String?) {
    if (BuildConfig.DEBUG) {
        Log.d("GaAction", "Screen ${gaAction.label}")
        return
    }

    try {

        val firebaseAnalytics = Firebase.analytics

        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, gaAction.label)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, className ?: "")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// keyName : ui_action말고, 다른 값을 넣을 때 사용할 변수명, keyValue : keyName에 넣을 값
fun setUiActionFirebaseGoogleAnalyticsWithKey(
    action: String?,
    label: String?,
    keyName: String?,
    keyValue: Int
) {
    if (BuildConfig.CHINA) {
        return
    }
    try {
        val firebaseAnalytics = Firebase.analytics

        val params = Bundle()
        params.putString("ui_action", action)
        params.putInt(keyName, keyValue)
        firebaseAnalytics.logEvent(label ?: return, params)
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}