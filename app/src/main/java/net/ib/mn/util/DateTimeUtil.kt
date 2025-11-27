package net.ib.mn.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 날짜/시간 관련 유틸리티
 */
object DateTimeUtil {

    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val serverDateFormatWithZ = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * 상대적인 시간 문자열 반환 (예: "5분 전", "2시간 전", "어제")
     *
     * @param dateString ISO 8601 형식의 날짜 문자열 (예: "2024-01-15T10:30:00Z")
     * @return 상대적인 시간 문자열
     */
    fun getRelativeTimeSpan(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""

        return try {
            val date = parseServerDate(dateString) ?: return ""
            val now = System.currentTimeMillis()
            val time = date.time

            DateUtils.getRelativeTimeSpanString(
                time,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 서버 날짜 문자열 파싱
     */
    private fun parseServerDate(dateString: String): Date? {
        return try {
            if (dateString.endsWith("Z")) {
                serverDateFormatWithZ.parse(dateString)
            } else {
                serverDateFormat.parse(dateString)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 날짜를 "yyyy.MM.dd" 형식으로 포맷팅
     */
    fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""

        return try {
            val date = parseServerDate(dateString) ?: return ""
            val format = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            format.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 날짜를 "yyyy.MM.dd HH:mm" 형식으로 포맷팅
     */
    fun formatDateTime(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""

        return try {
            val date = parseServerDate(dateString) ?: return ""
            val format = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
            format.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 날짜를 full 형식으로 포맷팅 (예: "2024.11.27 오후 5:05")
     * Old 프로젝트의 community_item.xml created_at 형식과 동일
     */
    fun formatFullDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""

        return try {
            val date = parseServerDate(dateString) ?: return ""
            val format = SimpleDateFormat("yyyy.M.d a h:mm", Locale.getDefault())
            format.format(date)
        } catch (e: Exception) {
            ""
        }
    }
}
