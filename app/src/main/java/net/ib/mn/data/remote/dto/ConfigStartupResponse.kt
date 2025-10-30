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

    @SerializedName("objects")
    val data: ConfigStartupData?
)

data class ConfigStartupData(
    @SerializedName("badword")
    val badWords: List<BadWord>?,  // ✅ List<BadWordModel> 구조 (old 프로젝트와 동일)

    @SerializedName("board_tag")  // ✅ snake_case로 수정 (old 프로젝트와 동일)
    val boardTags: List<BoardTag>?,

    @SerializedName("lgcode")
    val lgCodes: List<String>?,  // ✅ List<String>으로 수정 (old 프로젝트와 동일)

    @SerializedName("sns")
    val snsChannels: List<SnsChannel>?,

    @SerializedName("notice_list")  // ✅ snake_case로 수정 + String으로 변경 (JSON 문자열)
    val noticeList: String?,

    @SerializedName("event_list")  // ✅ snake_case로 수정 + String으로 변경 (JSON 문자열)
    val eventList: String?,

    @SerializedName("family_app_list")  // ✅ snake_case로 수정
    val familyAppList: List<FamilyApp>?,

    @SerializedName("upload_video_spec")  // ✅ snake_case로 수정
    val uploadVideoSpec: UploadVideoSpec?,

    @SerializedName("end_popup")  // ✅ snake_case로 수정
    val endPopup: EndPopup?,

    @SerializedName("new_picks")  // ✅ snake_case로 수정
    val newPicks: NewPicks?,  // ✅ NewPicks 객체로 변경 (old 프로젝트와 동일)

    @SerializedName("help_infos")  // ✅ snake_case로 수정
    val helpInfos: HelpInfos?  // ✅ HelpInfos 객체로 변경 (old 프로젝트와 동일)
)

// ============================================================
// BadWord - 욕설 필터 (old 프로젝트 BadWordModel과 동일)
// ============================================================
data class BadWord(
    @SerializedName("exc")
    val exc: List<String> = emptyList(),  // 예외 단어

    @SerializedName("type")
    val type: String,  // 타입

    @SerializedName("word")
    val word: String  // 욕설 단어
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

// ============================================================
// NewPicks - 하트픽 New 표시 (old 프로젝트 NewPicksModel과 동일)
// ============================================================
data class NewPicks(
    @SerializedName("heartpick")
    val heartpick: Boolean,

    @SerializedName("onepick")
    val onepick: Boolean,

    @SerializedName("themepick")
    val themepick: Boolean
)

// ============================================================
// HelpInfos - 도움말 정보 (old 프로젝트 HelpInfosModel과 동일)
// ============================================================
data class HelpInfos(
    @SerializedName("heartpick")
    val heartPick: String? = null,

    @SerializedName("onepick")
    val onePick: String? = null,

    @SerializedName("themepick")
    val themePick: String? = null,

    @SerializedName("free_board_placeholder")
    val freeBoardPlaceHolder: String? = null
)

// ============================================================
// 아래 클래스들은 noticeList, eventList를 JSON 파싱할 때 사용
// (서버는 JSON 문자열로 반환)
// ============================================================
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
