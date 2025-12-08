package net.ib.mn.util

import android.content.Context
import android.os.Build
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.data.remote.dto.MostIdol
import java.util.Locale

/**
 * 로케일 관련 유틸리티
 */
object LocaleUtil {

    private fun getDeviceLocale(context: Context): Locale =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

    /**
     * 현재 로케일이 지원 로케일 목록에 포함되는지 확인
     */
    fun isExistCurrentLocale(context: Context, locales: List<String>): Boolean =
        locales.contains(getDeviceLocale(context).language)

    /**
     * 시스템 언어 코드 반환 (예: "ko_KR", "en_US", "ja_JP", "zh_CN", "zh_TW")
     */
    fun getSystemLanguage(context: Context): String =
        getDeviceLocale(context).let { "${it.language}_${it.country}" }

    /**
     * 위키용 로케일 코드 반환 (ko, en, ja, zh-cn, zh-tw)
     */
    fun getWikiLocale(context: Context): String = when (getSystemLanguage(context)) {
        "ko_KR" -> "ko"
        "zh_CN" -> "zh-cn"
        "zh_TW" -> "zh-tw"
        "ja_JP" -> "ja"
        else -> "en"
    }

    /**
     * 앱 로케일 반환
     */
    fun getAppLocale(context: Context): Locale = getDeviceLocale(context)

    /**
     * 공유용 로케일 코드 반환 (ko, en, ja, zh-cn, zh-tw)
     */
    fun getShareLocale(context: Context): String = when (val locale = getSystemLanguage(context)) {
        "zh_CN" -> "zh-cn"
        "zh_TW" -> "zh-tw"
        "ko_KR", "ja_JP" -> locale.substringBefore("_")
        else -> "en"
    }

    /**
     * 스케줄용 로케일 코드 반환
     * Old 프로젝트의 IdolSchedule.scheduleLocaleString과 동일
     */
    fun getScheduleLocale(context: Context): String {
        val lang = getDeviceLocale(context).language
        return when {
            lang.startsWith("ko") -> "ko"
            lang.startsWith("ja") -> "ja"
            lang.startsWith("zh") -> {
                val country = getDeviceLocale(context).country
                if (country == "TW" || country == "HK") "zh-tw" else "zh-cn"
            }
            else -> "en"
        }
    }

    /**
     * MostIdol 객체에서 언어에 맞는 아이돌 이름 추출
     */
    fun getLocalizedIdolName(context: Context, most: MostIdol): String =
        getLocalizedName(
            context = context,
            name = most.name.orEmpty(),
            nameEn = most.nameEn.orEmpty(),
            nameZh = most.nameZh.orEmpty(),
            nameZhTw = most.nameZhTw.orEmpty(),
            nameJp = most.nameJp.orEmpty()
        )

    /**
     * IdolEntity 객체에서 언어에 맞는 아이돌 이름 추출
     */
    fun getLocalizedIdolName(context: Context, idol: IdolEntity): String =
        getLocalizedName(
            context = context,
            name = idol.name,
            nameEn = idol.nameEn,
            nameZh = idol.nameZh,
            nameZhTw = idol.nameZhTw,
            nameJp = idol.nameJp
        )

    private fun getLocalizedName(
        context: Context,
        name: String,
        nameEn: String,
        nameZh: String,
        nameZhTw: String,
        nameJp: String
    ): String {
        val lang = getSystemLanguage(context).lowercase()
        return when {
            lang.startsWith("ko") -> name.ifEmpty { nameEn }
            lang.startsWith("en") && nameEn.isNotEmpty() -> nameEn
            lang.startsWith("zh_tw") && nameZhTw.isNotEmpty() -> nameZhTw
            lang.startsWith("zh") && nameZh.isNotEmpty() -> nameZh
            lang.startsWith("ja") && nameJp.isNotEmpty() -> nameJp
            nameEn.isNotEmpty() -> nameEn
            else -> name.ifEmpty { nameEn }
        }
    }

    /**
     * 번역 가능한 텍스트 추출 (URL, 해시태그, 이모지, 멘션 제거)
     * old 프로젝트의 UtilK.extractTranslatable과 동일
     */
    fun extractTranslatable(input: String): String {
        // URL 패턴
        val urlPattern = "(http|https)://(([\\w\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\-\\+\\=\\(\\)\\{\\}\\?\\<\\>])*)+([\\.|/](([\\w\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\-\\+\\=\\(\\)\\{\\}\\?\\<\\>])*))+"

        // 해시태그 패턴
        val hashtagPattern = "#(\\p{L}|_|[0-9])+"

        // 이모지 패턴 (문자/숫자/기호 이외의 문자를 제거)
        val emojiPattern = "[^\\p{L}\\p{N}\\p{P}\\p{Z}]"

        // 멘션 패턴
        val mentionPattern = "@\\{\\d+:[^}]+\\}"

        // 패턴을 통합
        val combinedPattern = "$urlPattern|$hashtagPattern|$emojiPattern|$mentionPattern"

        // Regex를 이용해 매칭되는 부분을 제거
        return Regex(combinedPattern).replace(input, "")
            .replace("\\s+".toRegex(), " ")  // 여러 공백을 하나로
            .trim()
    }
}
