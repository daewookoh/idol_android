package net.ib.mn.util

import android.content.Context
import android.os.LocaleList
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * 약관 관련 유틸리티 함수
 *
 * old 프로젝트의 Util.getAgreementLanguage()를 Compose 프로젝트에 맞게 구현
 */
object AgreementUtil {

    /**
     * 시스템 언어에 따라 약관 언어 코드를 반환
     *
     * @return 언어 코드 (한국어: "", 중국어 간체: "_zhcn", 중국어 번체: "_zhtw", 일본어: "_ja", 영어: "_en")
     */
    fun getAgreementLanguage(context: Context): String {
        val locale = getSystemLocale()
        val language = locale.language
        val country = locale.country

        return when {
            language == "ko" -> ""
            language == "zh" && country == "CN" -> "_zhcn"
            language == "zh" && country == "TW" -> "_zhtw"
            language == "ja" -> "_ja"
            else -> "_en"
        }
    }

    /**
     * 시스템 로케일 가져오기
     */
    private fun getSystemLocale(): Locale {
        return try {
            LocaleList.getDefault()[0]
        } catch (e: Exception) {
            Locale.getDefault()
        }
    }

    /**
     * 이용약관 URL 생성
     *
     * @param context Context
     * @return 이용약관 URL
     */
    fun getTermsOfServiceUrl(context: Context): String {
        val lang = getAgreementLanguage(context)
        return "${ServerUrl.HOST}/static/agreement1$lang.html"
    }

    /**
     * 개인정보 처리방침 URL 생성
     *
     * @param context Context
     * @return 개인정보 처리방침 URL
     */
    fun getPrivacyPolicyUrl(context: Context): String {
        val lang = getAgreementLanguage(context)
        return "${ServerUrl.HOST}/static/agreement3$lang.html"
    }
}
