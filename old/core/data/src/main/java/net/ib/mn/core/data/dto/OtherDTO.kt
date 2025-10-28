package net.ib.mn.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class InquiryDTO(
    @SerialName("category") val category: String? = null,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("files") val files: List<UploadFileDTO>
)

@Serializable
data class UploadFileDTO(
    @SerialName("size") val size: Int,
    @SerialName("saved_filename") val savedFilename: String,
    @SerialName("origin_name") val originName: String,
)

@Serializable
data class DeleteFileDTO(
    @SerialName("bucket") val bucket: String,
    @SerialName("saved_filename") val savedFilename: String,
)

@Serializable
data class ReportDTO(
    @SerialName("article_id") val articleId: Long? = null,
    @SerialName("comment_id") val title: Long? = null,
)

@Serializable
data class ReportFeedDTO(
    @SerialName("user_id") val userId: Long,
    @SerialName("reason") val reason: String,
)

@Serializable
data class ReportHeartPickDTO(
    @SerialName("reply_id") val replyId: Long,
)

@Serializable
data class ReportChatRoomDTO(
    @SerialName("room_id") val roomId: Int,
    @SerialName("reason") val reason: String,
)

@Serializable
data class ChatRoomDTO(
    @SerialName("room_id") val roomId: Int,
)

@Serializable
data class PurchaseItemBurningDayDTO(
    @SerialName("burning_day") val burningDay: String,
)

@Serializable
data class ScheduleVoteDTO(
    @SerialName("schedule_id") val scheduleId: Int,
    @SerialName("vote") val vote: String? = null,
)

@Serializable
data class ScheduleWriteDTO(
    @SerialName("idol_id") val idolId: Int,
    @SerialName("idol_ids") val idolIds: String? = null,
    @SerialName("title") val title: String,
    @SerialName("category") val category: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("lat") val lat: String? = null,
    @SerialName("lng") val lng: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("dtstart") val dtstart: String? = null,
    @SerialName("duration") val duration: Int,
    @SerialName("allday") val allday: Int,
    @SerialName("extra") val extra: String? = null,
    @SerialName("locale") val locale: String? = null,
)

@Serializable
data class HeartpickVoteDTO(
    @SerialName("heartpick_id") val heartpickId: Int,
    @SerialName("heartpick_idol_id") val heartpickIdolId: Int,
    @SerialName("number") val number: Long,
)

@Serializable
data class OnepickVoteDTO(
    @SerialName("id") val id: Int,
    @SerialName("vote_ids") val voteIds: String,
    @SerialName("vote_type") val voteType: String? = null,
)

@Serializable
data class ThemepickVoteDTO(
    @SerialName("themepick_id") val id: Int,
    @SerialName("themepick_idol_id") val idolId: Int,
    @SerialName("vote_type") val voteType: String? = null,
)

@Serializable
data class SupportGiveDiamondDTO(
    @SerialName("support_id") val supportId: Int,
    @SerialName("number") val number: Int,
)

@Serializable
data class SupportLikeDTO(
    @SerialName("support_id") val supportId: Int,
)

@Serializable
data class PlayLikeDTO(
    @SerialName("live_id") val liveId: Int,
    @SerialName("heart") val heart: Int,
)

@Serializable
data class DeleteMessageByTypeDTO(
    @SerialName("type") val type: String? = null,
)

@Serializable
data class ClaimMessageDTO(
    @SerialName("id") val id: Int,
)

@Serializable
data class TakeCouponDTO(
    @SerialName("value") val value: String? = null,
)

@Serializable
data class AddFavoriteDTO(
    @SerialName("idol_id") val idolId: Int,
)

@Serializable
data class ClaimMissionRewardDTO(
    @SerialName("key") val key: String,
)


@Serializable
data class ReportLogDTO(
    @SerialName("key") val key: String,
    @SerialName("text") val text: String,
)

@Serializable
data class OpenNotificationDTO(
    @SerialName("id") val id: Int,
)
