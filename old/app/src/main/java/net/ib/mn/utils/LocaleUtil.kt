package net.ib.mn.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleUtil {
    fun setLocale(context: Context) {
        var localeString = Util.getPreference(context, Const.PREF_LANGUAGE)
        if (localeString.isNullOrEmpty()) {
            localeString = getCurrentSystemLocale(context)?.toLanguageTag() ?: Const.locales[1]
        }

        // 그래도 null이면
        val locale: Locale = if (localeString.contains("_")) {
            val ls = localeString.split("_")
            Locale(ls[0], ls[1])
        } else {
            Locale.forLanguageTag(localeString)
        }

        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(locale.toLanguageTag())
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getCurrentSystemLocale(context: Context): Locale? =
        LocaleManagerCompat.getSystemLocales(context).get(0)

    fun convertHyphenToUnderscore(locale: Locale?): String {
        return if (locale == null) {
            //이경우엔 시스템상에서 언어 설정한게 하나도 없다는뜻이므로 친절하게 한국어 학습을 시켜줍니다.
            Const.locales[1]
        } else {
            locale.language + "_" + locale.country
        }
    }

    fun isExistCurrentLocale(context: Context, locales: List<String>): Boolean {
        val locale = Util.getPreference(context, Const.PREF_LANGUAGE).split("_")
        return if (locale[0].isEmpty()) {
            locales.any { it == context.resources.configuration.locales[0].language }
        } else {
            locales.any { it == locale[0] }
        }
    }

    fun getAppLocale(context: Context): Locale {
        val defaultSystemLang = Util.getSystemLanguage(context)
        val localeParts = defaultSystemLang.split("_")
        return when (localeParts.size) {
            2 -> Locale(localeParts[0], localeParts[1])
            1 -> Locale(localeParts[0])
            else -> Locale.getDefault()
        }
    }
}