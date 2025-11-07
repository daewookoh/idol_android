package net.ib.mn.util

import java.text.DecimalFormat

/**
 * 숫자 포맷팅 유틸리티
 */
object NumberFormatUtil {

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
