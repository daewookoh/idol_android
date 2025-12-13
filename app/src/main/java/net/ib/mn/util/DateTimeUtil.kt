package net.ib.mn.util

import android.content.Context
import android.text.format.DateUtils
import net.ib.mn.R
import net.ib.mn.domain.model.AdDate
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 날짜/시간 관련 유틸리티
 *
 * 프로젝트 전체에서 공용으로 사용되는 날짜/시간 관련 함수들을 모아놓은 유틸리티
 */
object DateTimeUtil {

    // ============================================================
    // 상수
    // ============================================================

    private const val SERVER_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    private const val SERVER_DATE_FORMAT_WITH_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    private const val DATE_FORMAT_DOT = "yyyy.MM.dd"
    private const val DATE_FORMAT_DOT_SPACED = "yyyy. M. d."

    const val MILLIS_PER_SECOND = 1000L
    const val MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND
    const val MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE
    const val MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR

    /**
     * 상태 코드 (HeartPick, OnePick 등에서 공용 사용)
     */
    object Status {
        const val UPCOMING = 0  // 진행예정
        const val ACTIVE = 1    // 진행중
        const val ENDED = 2     // 종료
    }

    // ============================================================
    // 서버 날짜 파싱
    // ============================================================

    private val serverDateFormat = SimpleDateFormat(SERVER_DATE_FORMAT, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Seoul")
    }

    private val serverDateFormatWithZ = SimpleDateFormat(SERVER_DATE_FORMAT_WITH_Z, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Seoul")
    }

    /**
     * 서버 날짜 문자열 파싱
     */
    fun parseServerDate(dateString: String?): Date? {
        if (dateString.isNullOrEmpty()) return null
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
     * 서버 날짜 문자열을 밀리초로 변환
     */
    fun parseServerDateToMillis(dateString: String?): Long {
        return parseServerDate(dateString)?.time ?: 0L
    }

    // ============================================================
    // D-Day 계산
    // ============================================================

    /**
     * D-Day 계산 (HeartPick, OnePick 등에서 공용 사용)
     *
     * @param context Context (문자열 리소스용)
     * @param endAt 종료일시 (yyyy-MM-dd'T'HH:mm:ss 형식)
     * @param status 상태 (0: UPCOMING, 1: ACTIVE, 2: ENDED)
     * @return D-Day 텍스트
     *   - UPCOMING: "진행예정" (다국어)
     *   - ENDED: "종료" (다국어)
     *   - ACTIVE: "D-{days}" 또는 "HH:MM" (24시간 미만)
     */
    fun calculateDDay(context: Context, endAt: String, status: Int): String {
        return try {
            when (status) {
                Status.UPCOMING -> context.getString(R.string.upcoming)
                Status.ENDED -> context.getString(R.string.vote_finish)
                else -> calculateActiveDDay(context, endAt)
            }
        } catch (e: Exception) {
            "D-Day"
        }
    }

    /**
     * ACTIVE 상태의 D-Day 계산
     */
    private fun calculateActiveDDay(context: Context, endAt: String): String {
        val endDate = parseServerDate(endAt) ?: return "D-Day"
        val now = Calendar.getInstance().time
        val diff = endDate.time - now.time
        val days = diff / MILLIS_PER_DAY

        return when {
            diff < 0 -> context.getString(R.string.vote_finish)
            diff < MILLIS_PER_DAY -> formatTimeHHMM(diff)
            else -> "D-$days"
        }
    }

    /**
     * D-Day 계산 (초 단위 포함, HeartPickDetailViewModel 카운트다운용)
     *
     * @param context Context
     * @param endAt 종료일시
     * @param status 상태
     * @return D-Day 텍스트 (24시간 미만이면 HH:MM:SS)
     */
    fun calculateDDayWithSeconds(context: Context, endAt: String, status: Int): String {
        return try {
            when (status) {
                Status.UPCOMING -> context.getString(R.string.upcoming)
                Status.ENDED -> context.getString(R.string.vote_finish)
                else -> {
                    val endDate = parseServerDate(endAt) ?: return "D-Day"
                    val now = Calendar.getInstance().time
                    val diff = endDate.time - now.time
                    val days = diff / MILLIS_PER_DAY

                    when {
                        diff < 0 -> context.getString(R.string.vote_finish)
                        diff < MILLIS_PER_DAY -> formatTimeHHMMSS(diff)
                        else -> "D-$days"
                    }
                }
            }
        } catch (e: Exception) {
            "D-Day"
        }
    }

    /**
     * 종료일까지 남은 밀리초 계산
     */
    fun getRemainingMillis(endAt: String): Long {
        val endDate = parseServerDate(endAt) ?: return 0L
        val now = Calendar.getInstance().time
        return endDate.time - now.time
    }

    // ============================================================
    // 시간 포맷팅
    // ============================================================

    /**
     * 밀리초를 HH:MM 형식으로 포맷
     */
    fun formatTimeHHMM(millis: Long): String {
        val hours = millis / MILLIS_PER_HOUR
        val minutes = (millis % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE
        return String.format(Locale.US, "%02d:%02d", hours, minutes)
    }

    /**
     * 밀리초를 HH:MM:SS 형식으로 포맷
     */
    fun formatTimeHHMMSS(millis: Long): String {
        val hours = millis / MILLIS_PER_HOUR
        val minutes = (millis % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE
        val seconds = (millis % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    // ============================================================
    // 기간 포맷팅
    // ============================================================

    /**
     * 기간을 "yyyy.MM.dd ~ yyyy.MM.dd" 형식으로 포맷
     */
    fun formatPeriod(beginAt: String?, endAt: String?): String {
        if (beginAt.isNullOrEmpty() || endAt.isNullOrEmpty()) return ""
        return try {
            val beginDate = parseServerDate(beginAt) ?: return ""
            val endDate = parseServerDate(endAt) ?: return ""
            val outputFormat = SimpleDateFormat(DATE_FORMAT_DOT, Locale.getDefault())
            "${outputFormat.format(beginDate)} ~ ${outputFormat.format(endDate)}"
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 기간을 "yyyy. M. d. ~ yyyy. M. d." 형식으로 포맷 (OnePick용)
     */
    fun formatPeriodSpaced(beginAt: String?, endAt: String?): String {
        if (beginAt.isNullOrEmpty() || endAt.isNullOrEmpty()) return ""
        return try {
            val beginDate = parseServerDate(beginAt) ?: return ""
            val endDate = parseServerDate(endAt) ?: return ""
            val outputFormat = SimpleDateFormat(DATE_FORMAT_DOT_SPACED, Locale.getDefault())
            "${outputFormat.format(beginDate)} ~ ${outputFormat.format(endDate)}"
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 종료일을 "Until yyyy.MM.dd" / "yyyy.MM.dd까지" 형식으로 포맷
     */
    fun formatEndDateWithUntil(context: Context, endAt: String?): String {
        if (endAt.isNullOrEmpty()) return ""
        return try {
            val endDate = parseServerDate(endAt) ?: return ""
            val outputFormat = SimpleDateFormat(DATE_FORMAT_DOT, Locale.getDefault())
            val dateStr = outputFormat.format(endDate)
            context.getString(R.string.finish_at, dateStr)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 시작일까지 D-Day 계산 (UPCOMING 상태용)
     * 사용자 타임존 기준으로 계산
     */
    fun calculateOpenDDay(context: Context, beginAt: String?): String {
        if (beginAt.isNullOrEmpty()) return context.getString(R.string.vote_dday, "0")
        return try {
            val userZoneId = TimeZone.getDefault().toZoneId()
            val dateFormat = SimpleDateFormat(SERVER_DATE_FORMAT, Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val beginDate = dateFormat.parse(beginAt)
            if (beginDate != null) {
                val nowUserZone = ZonedDateTime.now(userZoneId).toLocalDate().atStartOfDay(userZoneId)
                val beginUserZone = Instant.ofEpochMilli(beginDate.time).atZone(userZoneId).toLocalDate().atStartOfDay(userZoneId)
                val diffInDays = ChronoUnit.DAYS.between(nowUserZone.toLocalDate(), beginUserZone.toLocalDate())
                context.getString(R.string.vote_dday, diffInDays.toString())
            } else {
                context.getString(R.string.vote_dday, "0")
            }
        } catch (e: Exception) {
            context.getString(R.string.vote_dday, "0")
        }
    }

    // ============================================================
    // 날짜 포맷팅
    // ============================================================

    /**
     * 날짜를 "yyyy.MM.dd" 형식으로 포맷팅
     */
    fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""
        return try {
            val date = parseServerDate(dateString) ?: return ""
            SimpleDateFormat(DATE_FORMAT_DOT, Locale.getDefault()).format(date)
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
            SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 날짜를 full 형식으로 포맷팅 (예: "2024.11.27 오후 5:05")
     */
    fun formatFullDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""
        return try {
            val date = parseServerDate(dateString) ?: return ""
            SimpleDateFormat("yyyy.M.d a h:mm", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 날짜를 한국 시간대 기준 full 형식으로 포맷팅
     */
    fun formatFullDateKorea(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""
        return try {
            val date = parseServerDate(dateString) ?: return ""
            SimpleDateFormat("yyyy.M.d a h:mm", Locale.KOREA).apply {
                timeZone = TimeZone.getTimeZone("Asia/Seoul")
            }.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * KST 기준 오늘이면 시간만, 아니면 날짜만 표시
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
                SimpleDateFormat("a h:mm", Locale.KOREA).apply {
                    timeZone = kstTimeZone
                }.format(date)
            } else {
                SimpleDateFormat("yyyy. M.d.", Locale.KOREA).apply {
                    timeZone = kstTimeZone
                }.format(date)
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 상대적인 시간 문자열 반환 (예: "5분 전", "2시간 전", "어제")
     */
    fun getRelativeTimeSpan(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return ""
        return try {
            val date = parseServerDate(dateString) ?: return ""
            val now = System.currentTimeMillis()
            DateUtils.getRelativeTimeSpanString(
                date.time,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 시작일이 현재 시간으로부터 48시간 이내인지 확인 (신규 카드 표시용)
     *
     * @param beginAt 시작일 (yyyy-MM-dd'T'HH:mm:ss 형식)
     * @return 48시간 이내이면 true
     */
    fun isWithin48Hours(beginAt: String?): Boolean {
        if (beginAt.isNullOrEmpty()) return false
        return try {
            val beginDate = parseServerDate(beginAt) ?: return false
            val now = Calendar.getInstance().time
            val hoursDifference = (now.time - beginDate.time) / MILLIS_PER_HOUR
            hoursDifference in 0..47
        } catch (e: Exception) {
            false
        }
    }
}

// ============================================================
// 확장 함수
// ============================================================

/**
 * 광고 기간 문자열을 사용자 친화적인 형식으로 변환
 */
fun String.getAdDatePeriod(context: Context): String {
    return when {
        this.contains(AdDate.DAY.value) -> this.formatAdPeriod(
            AdDate.DAY.value,
            R.string.date_format_day,
            R.string.date_format_days,
            context
        )
        this.contains(AdDate.WEEK.value) -> this.formatAdPeriod(
            AdDate.WEEK.value,
            R.string.date_format_week,
            R.string.date_format_weeks,
            context
        )
        this.contains(AdDate.MONTH.value) -> this.formatAdPeriod(
            AdDate.MONTH.value,
            R.string.date_format_month,
            R.string.date_format_months,
            context
        )
        else -> ""
    }
}

private fun String.formatAdPeriod(
    period: String,
    singularResId: Int,
    pluralResId: Int,
    context: Context
): String {
    return try {
        val periodValue = this.substring(0, this.indexOf(period)).toInt()
        if (periodValue == 1) {
            context.getString(singularResId)
        } else {
            String.format(context.getString(pluralResId), periodValue)
        }
    } catch (e: Exception) {
        ""
    }
}
