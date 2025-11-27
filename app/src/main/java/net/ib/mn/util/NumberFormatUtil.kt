package net.ib.mn.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * 숫자 포맷팅 유틸리티
 */
object NumberFormatUtil {

    /**
     * 숫자를 천 단위 콤마가 포함된 문자열로 변환
     *
     * 예시:
     * - 1000 -> "1,000"
     * - 1234567 -> "1,234,567"
     *
     * @param value 포맷팅할 숫자
     * @param locale 로케일 (기본값: US)
     * @return 콤마가 포함된 문자열
     */
    fun formatWithComma(value: Long, locale: Locale = Locale.US): String {
        return NumberFormat.getNumberInstance(locale).format(value)
    }

    /**
     * Int를 천 단위 콤마가 포함된 문자열로 변환
     */
    fun formatWithComma(value: Int, locale: Locale = Locale.US): String {
        return NumberFormat.getNumberInstance(locale).format(value)
    }

    /**
     * 숫자를 축약된 형태로 포맷팅
     *
     * 예시:
     * - 999 -> "999"
     * - 1,000 -> "1K"
     * - 2,200 -> "2.2K"
     * - 1,500,000 -> "1.5M"
     *
     * @param value 포맷팅할 숫자
     * @return 축약된 문자열
     */
    fun formatNumberShort(value: Int): String {
        return when {
            value < 1000 -> "$value"
            value < 1000000 -> {
                val formattedValue = value.toDouble() / 1000.0
                val decimalFormat = DecimalFormat("#.#")
                "${decimalFormat.format(formattedValue)}K"
            }
            else -> {
                val formattedValue = value.toDouble() / 1000000.0
                val decimalFormat = DecimalFormat("#.#")
                "${decimalFormat.format(formattedValue)}M"
            }
        }
    }

    /**
     * 팔로워 수를 축약된 형태로 포맷팅 (+ 접미사 포함)
     *
     * 예시:
     * - 99 -> "99+"
     * - 150 -> "100+"
     * - 1,500 -> "1K+"
     * - 35,000 -> "35K+"
     * - 1,500,000 -> "1.5M+"
     *
     * @param value 포맷팅할 숫자
     * @return 축약된 문자열 (+ 포함)
     */
    fun formatFollowerCount(value: Int): String {
        return when {
            value < 100 -> "${value}+"
            value < 1000 -> "${(value / 100) * 100}+"
            value < 1000000 -> {
                val kValue = value / 1000
                "${kValue}K+"
            }
            else -> {
                val formattedValue = value.toDouble() / 1000000.0
                val decimalFormat = DecimalFormat("#.#")
                "${decimalFormat.format(formattedValue)}M+"
            }
        }
    }

    /**
     * 문자열로 된 숫자를 축약된 형태로 포맷팅
     *
     * @param value 포맷팅할 숫자 문자열 (콤마 포함 가능)
     * @return 축약된 문자열
     */
    fun formatNumberShort(value: String): String {
        val cleanedValue = value.replace(",", "")
        val intValue = cleanedValue.toIntOrNull() ?: return value
        return formatNumberShort(intValue)
    }
}
