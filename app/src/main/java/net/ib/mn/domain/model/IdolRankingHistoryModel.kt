package net.ib.mn.domain.model

import android.content.Context
import com.google.gson.annotations.SerializedName
import net.ib.mn.util.LocaleUtil
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.TimeZone

/**
 * 랭킹 변동 히스토리 모델
 * Old 프로젝트의 HallAggHistoryModel 참고
 */
data class IdolRankingHistoryModel(
    val heart: Long = 0,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    val rank: Int = 0,
    val type: String = "",
    val status: String? = null,  // increase, same, new, decrease
    val difference: Int = 0,

    @SerializedName("resource_uri")
    val resourceUri: String? = null,

    val league: String? = null,

    val refdate: String = ""
) {
    /**
     * 포맷된 날짜 문자열 반환
     * Old: getRefdate(context)
     */
    fun getFormattedRefDate(context: Context): String {
        if (refdate.isEmpty()) return ""

        return try {
            val appLocale = LocaleUtil.getAppLocale(context)
            val fixedDateFormat = SimpleDateFormat("yyyy-MM-dd", appLocale)
            fixedDateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

            val returnDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, appLocale)
            returnDateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

            val date = fixedDateFormat.parse(refdate)
            date?.let { returnDateFormat.format(it) } ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
