package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScheduleDto(
    @SerializedName("id") val id: Int,
    @SerializedName("article_id") val articleId: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("dtstart") val dtstartStr: String?,
    @SerializedName("allday") val allday: Int?,
    @SerializedName("location") val location: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("extra") val extra: String?,
    @SerializedName("num_comments") val numComments: Int?,
    @SerializedName("num_yes") val numYes: Int?,
    @SerializedName("num_no") val numNo: Int?,
    @SerializedName("num_dup") val numDup: Int?,
    @SerializedName("vote") val vote: String?,
    @SerializedName("is_viewable") val isViewable: String?,
    @SerializedName("is_readonly") val isReadonly: String?,
    @SerializedName("user") val user: UserDto?,
    @SerializedName("created_at") val createdAtStr: String?,
    @SerializedName("updated_at") val updatedAtStr: String?
) {
    data class UserDto(
        @SerializedName("id") val id: Int?
    )

    companion object {
        private val UTC = java.util.TimeZone.getTimeZone("UTC")

        private val DATE_FORMATS = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = UTC },
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = UTC },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).apply { timeZone = UTC }
        )

        fun parseDate(dateStr: String?): Date? {
            if (dateStr.isNullOrBlank()) return null
            for (format in DATE_FORMATS) {
                try {
                    return format.parse(dateStr)
                } catch (e: Exception) {
                    // Try next format
                }
            }
            return null
        }
    }

    val dtstart: Date?
        get() = parseDate(dtstartStr)

    val createdAt: Date?
        get() = parseDate(createdAtStr)

    val updatedAt: Date?
        get() = parseDate(updatedAtStr)
}
