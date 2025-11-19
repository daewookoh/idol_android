package net.ib.mn.util

import android.content.Context
import android.os.Build

/**
 * 로케일 관련 유틸리티
 */
object LocaleUtil {
    /**
     * 현재 로케일이 지원 로케일 목록에 포함되는지 확인
     *
     * @param context Context
     * @param locales 지원 로케일 목록 (언어 코드 리스트)
     * @return 현재 로케일이 목록에 포함되면 true
     */
    fun isExistCurrentLocale(context: Context, locales: List<String>): Boolean {
        val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        return locales.contains(currentLocale.language)
    }
}
