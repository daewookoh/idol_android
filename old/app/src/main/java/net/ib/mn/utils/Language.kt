package net.ib.mn.utils

import androidx.annotation.StringRes
import net.ib.mn.R

enum class BoardLanguage(
    val code: String,
    @StringRes val labelResId: Int
) {
    ALL("all", R.string.filter_all_language),
    KOREAN("ko_KR", R.string.language_korean),
    ENGLISH("en_US", R.string.language_english),
    CHINESE_CN("zh_CN", R.string.language_chinese_cn),
    CHINESE_TW("zh_TW", R.string.language_chinese_tw),
    JAPANESE("ja_JP", R.string.language_japanese),
    INDONESIAN("in_ID", R.string.language_indonesian),
    THAI("th_TH", R.string.language_thai),
    VIETNAMESE("vi_VN", R.string.language_vietnamese),
    SPANISH("es_ES", R.string.language_spanish),
    PORTUGUESE("pt_BR", R.string.language_portuguese),
    GERMAN("de_DE", R.string.language_germany),
    FRENCH("fr_FR", R.string.language_french),
    ITALIAN("it_IT", R.string.language_italian),
    ARABIC("ar_001", R.string.language_arabic),
    PERSIAN("fa_IR", R.string.language_persian),
    RUSSIAN("ru_RU", R.string.language_russian),
    TURKISH("tr_TR", R.string.language_turkish);

    companion object {

        fun fromCode(code: String): BoardLanguage =
            entries.find { it.code == code } ?: ALL

        fun fromResId(@StringRes resId: Int): BoardLanguage? =
            entries.find { it.labelResId == resId }

        fun all(): List<BoardLanguage> = entries
    }
}