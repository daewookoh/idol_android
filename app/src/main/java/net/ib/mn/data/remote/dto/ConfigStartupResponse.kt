package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * /configs/startup/ API Response
 *
 * 앱 시작 시 필요한 설정 정보를 제공
 * - 욕설 필터
 * - 공지사항
 * - 이벤트 목록
 * - SNS 채널 정보
 * - 업로드 제한 정보 등
 */
data class ConfigStartupResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: ConfigStartupData?
)

data class ConfigStartupData(
    @SerializedName("badword")
    val badWords: List<String>?,

    @SerializedName("boardTag")
    val boardTags: List<BoardTag>?,

    @SerializedName("lgcode")
    val lgCodes: List<LgCode>?,

    @SerializedName("sns")
    val snsChannels: List<SnsChannel>?,

    @SerializedName("noticeList")
    val noticeList: List<Notice>?,

    @SerializedName("eventList")
    val eventList: List<Event>?,

    @SerializedName("familyAppList")
    val familyAppList: List<FamilyApp>?,

    @SerializedName("uploadVideoSpec")
    val uploadVideoSpec: UploadVideoSpec?,

    @SerializedName("endPopup")
    val endPopup: EndPopup?,

    @SerializedName("newPicks")
    val newPicks: List<NewPick>?,

    @SerializedName("helpInfos")
    val helpInfos: List<HelpInfo>?
)

data class BoardTag(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)

data class LgCode(
    @SerializedName("code")
    val code: String,

    @SerializedName("name")
    val name: String
)

data class SnsChannel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("type")
    val type: String, // "twitter", "instagram", "youtube", etc.

    @SerializedName("url")
    val url: String,

    @SerializedName("name")
    val name: String
)

data class Notice(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("is_important")
    val isImportant: Boolean?
)

data class Event(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("start_date")
    val startDate: String,

    @SerializedName("end_date")
    val endDate: String,

    @SerializedName("link_url")
    val linkUrl: String?
)

data class FamilyApp(
    @SerializedName("name")
    val name: String,

    @SerializedName("package_name")
    val packageName: String,

    @SerializedName("icon_url")
    val iconUrl: String?,

    @SerializedName("store_url")
    val storeUrl: String
)

data class UploadVideoSpec(
    @SerializedName("max_size_mb")
    val maxSizeMb: Int?,

    @SerializedName("max_duration_sec")
    val maxDurationSec: Int?,

    @SerializedName("allowed_formats")
    val allowedFormats: List<String>?
)

data class EndPopup(
    @SerializedName("enabled")
    val enabled: Boolean?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("message")
    val message: String?,

    @SerializedName("image_url")
    val imageUrl: String?
)

data class NewPick(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("options")
    val options: List<String>?
)

data class HelpInfo(
    @SerializedName("category")
    val category: String,

    @SerializedName("question")
    val question: String,

    @SerializedName("answer")
    val answer: String
)
