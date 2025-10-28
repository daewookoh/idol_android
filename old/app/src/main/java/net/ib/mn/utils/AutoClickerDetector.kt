package net.ib.mn.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager

/**
 * ProjectName: idol_app_renew
 *
 * Description: 접근성 관련  체크를 위한  detector이다.
 * autoclicker 들이 대부분  접근성 기능을 사용하기때문에
 *  해당 접근성 기능  로그 ->FEEDBACK_GENERIC 을 사용하는 서비스들을  감지해서  return 해준다.
 * * */
object AutoClickerDetector {
    fun checkAccessibilityServicesEnabled(context: Context): MutableList<AccessibilityServiceInfo> {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    }
}