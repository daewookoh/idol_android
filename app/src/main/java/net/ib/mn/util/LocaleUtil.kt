package net.ib.mn.util

import android.content.Context
import android.os.Build
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.data.remote.dto.MostIdol
import net.ib.mn.domain.model.SearchIdolModel
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
     * 웹뷰용 로케일 코드 반환
     * old 프로젝트의 LanguagePreferenceRepositoryImpl.getSystemLanguage()와 동일한 방식
     *
     * - 저장된 언어 설정이 있으면 우선 사용
     * - 중국어는 zh-cn, zh-tw 형식으로 반환
     * - 그 외에는 언어 코드만 반환 (ko, en, ja)
     *
     * @param savedLanguage 저장된 언어 설정 (ex: "ko_KR", "zh_CN", "en")
     */
    fun getWebViewLocale(context: Context, savedLanguage: String?): String {
        // 저장된 언어 설정이 있으면 우선 사용
        if (!savedLanguage.isNullOrEmpty()) {
            return when {
                savedLanguage.startsWith("zh_") || savedLanguage.startsWith("zh-") -> {
                    savedLanguage.replace("_", "-").lowercase()
                }
                savedLanguage.contains("_") -> {
                    savedLanguage.substringBefore("_")
                }
                else -> savedLanguage
            }
        }

        // 저장된 설정이 없으면 시스템 언어 사용
        val locale = getDeviceLocale(context)
        val langCode = locale.language
        val countryCode = locale.country

        return if (langCode == "zh") {
            "$langCode-${countryCode.lowercase()}"
        } else {
            langCode
        }
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
     * MostIdol 객체에서 "이름_그룹명" 형식의 전체 이름 추출
     * ExoNameWithGroup과 함께 사용
     *
     * API에서 name이 이미 "이름_그룹명" 형식으로 올 수 있음
     * - name에 "_"가 포함되어 있으면 그대로 반환
     * - 없고 groupName이 있으면 "이름_그룹명" 형식으로 결합
     * - 솔로(type == "S")면 이름만 반환
     */
    fun getLocalizedIdolFullName(context: Context, most: MostIdol): String {
        val name = getLocalizedIdolName(context, most)

        // 이름이 비어있으면 빈 문자열 반환
        if (name.isEmpty()) {
            return ""
        }

        // 이미 "_"가 포함되어 있으면 그대로 반환
        if (name.contains("_")) {
            return name
        }

        val isSolo = most.type.equals("S", ignoreCase = true)
        return if (!isSolo && !most.groupName.isNullOrEmpty()) {
            "${name}_${most.groupName}"
        } else {
            name
        }
    }

    /**
     * SearchIdolModel 객체에서 언어에 맞는 아이돌 이름 추출
     */
    fun getLocalizedIdolName(context: Context, idol: SearchIdolModel): String =
        getLocalizedName(
            context = context,
            name = idol.name,
            nameEn = idol.nameEn.orEmpty(),
            nameZh = idol.nameZh.orEmpty(),
            nameZhTw = idol.nameZhTw.orEmpty(),
            nameJp = idol.nameJp.orEmpty()
        )

    /**
     * SearchIdolModel 객체에서 "이름_그룹명" 형식의 전체 이름 추출
     */
    fun getLocalizedIdolFullName(context: Context, idol: SearchIdolModel): String {
        val name = getLocalizedIdolName(context, idol)

        if (name.isEmpty()) {
            return ""
        }

        if (name.contains("_")) {
            return name
        }

        val isSolo = idol.type.equals("S", ignoreCase = true)
        return if (!isSolo && !idol.groupName.isNullOrEmpty()) {
            "${name}_${idol.groupName}"
        } else {
            name
        }
    }

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

    fun getLocalizedName(
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
