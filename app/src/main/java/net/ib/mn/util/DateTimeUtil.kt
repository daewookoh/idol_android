package net.ib.mn.util

import android.text.format.DateUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 날짜/시간 관련 유틸리티
 */
object DateTimeUtil {

    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Seoul")
    }

    private val serverDateFormatWithZ = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Seoul")
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

    /**
     * 날짜를 한국 시간대 기준 full 형식으로 포맷팅 (예: "2024.11.27 오후 5:05")
     * 게시글에서 사용
     */
    fun formatFullDateKorea(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""

        return try {
            val date = parseServerDate(dateString) ?: return ""
            val format = SimpleDateFormat("yyyy.M.d a h:mm", Locale.KOREA).apply {
                timeZone = TimeZone.getTimeZone("Asia/Seoul")
            }
            format.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * KST 기준 오늘이면 시간만, 아니면 날짜만 표시
     * 게시판 목록에서 사용 (ExoArticleItem)
     * 날짜 형식: "2025. 11.10.", 시간 형식: "오후 3:30"
     */
    fun formatBoardDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""

        return try {
            val date = parseServerDate(dateString) ?: return ""
            val kstTimeZone = TimeZone.getTimeZone("Asia/Seoul")

            val now = Calendar.getInstance(kstTimeZone)
            val created = Calendar.getInstance(kstTimeZone).apply { time = date }

            if (now.get(Calendar.YEAR) == created.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == created.get(Calendar.DAY_OF_YEAR)
            ) {
                // 오늘: 시간만 표시 (오후 2:15 형식)
                SimpleDateFormat("a h:mm", Locale.KOREA).apply {
                    timeZone = kstTimeZone
                }.format(date)
            } else {
                // 오늘 아님: 날짜만 표시 (2025. 11.10. 형식)
                SimpleDateFormat("yyyy. M.d.", Locale.KOREA).apply {
                    timeZone = kstTimeZone
                }.format(date)
            }
        } catch (e: Exception) {
            ""
        }
    }
}
