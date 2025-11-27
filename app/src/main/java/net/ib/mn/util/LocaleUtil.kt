package net.ib.mn.util

import android.content.Context
import android.os.Build
import java.util.Locale

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

    /**
     * 시스템 언어 코드 반환
     * old 프로젝트의 Util.getSystemLanguage()와 동일
     *
     * @param context Context
     * @return 언어_국가 형식 (예: "ko_KR", "en_US", "ja_JP", "zh_CN", "zh_TW")
     */
    fun getSystemLanguage(context: Context): String {
        val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        return "${locale.language}_${locale.country}"
    }

    /**
     * 위키용 로케일 코드 반환
     *
     * @param context Context
     * @return 위키 API용 로케일 (ko, en, ja, zh-cn, zh-tw)
     */
    fun getWikiLocale(context: Context): String {
        return when (getSystemLanguage(context)) {
            "ko_KR" -> "ko"
            "zh_CN" -> "zh-cn"
            "zh_TW" -> "zh-tw"
            "ja_JP" -> "ja"
            else -> "en"
        }
    }
}
