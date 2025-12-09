package net.ib.mn.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import org.json.JSONObject
import java.io.Serializable

/**
 * 채팅 메시지 모델
 * Room Entity로 로컬 DB에 저장됨
 */
@Entity(
    tableName = "chat_message",
    primaryKeys = ["user_id", "server_ts"]
)
data class ChatMessageModel(
    @ColumnInfo(name = "client_ts") val clientTs: Long = 0L,
    @ColumnInfo(name = "server_ts") val serverTs: Long = 0L,
    @ColumnInfo(name = "user_id") val userId: Int = 0,
    @ColumnInfo(name = "room_id") val roomId: Int = 0,
    @ColumnInfo(name = "content") val content: String = "",
    @ColumnInfo(name = "content_type") val contentType: String? = null,
    @ColumnInfo(name = "content_desc") val contentDesc: String? = null,
    @ColumnInfo(name = "status") val status: Boolean = false,
    @ColumnInfo(name = "status_failed") val statusFailed: Boolean = false,
    @ColumnInfo(name = "is_readable") val isReadable: Boolean = true,
    @ColumnInfo(name = "reported") val reported: Boolean = false,
    @ColumnInfo(name = "deleted") val deleted: Boolean = false,
    @ColumnInfo(name = "is_first_join_msg") val isFirstJoinMsg: Boolean = false,
    @ColumnInfo(name = "is_link_url") val isLinkUrl: Boolean = false,
    @ColumnInfo(name = "seq") val seq: Int = 0,
    @ColumnInfo(name = "receive_count") val receiveCount: Int = 0,
    @ColumnInfo(name = "read_count") val readCount: Int = 0,
    @ColumnInfo(name = "reports") val reports: Int = 0,
    @ColumnInfo(name = "type") val type: String? = null,
    @ColumnInfo(name = "account_id") val accountId: Int = 0
) : Serializable {

    companion object {
        // 메시지 타입 상수
        const val TYPE_REPORTED = "reported"
        const val TYPE_META = "meta"
        const val TYPE_NORMAL = "chat"
        const val TYPE_TEXT = "text/plain"
        const val TYPE_IMAGE = "text/vnd.exodus.image"
        const val TYPE_VIDEO = "text/vnd.exodus.video"
        const val TYPE_TOAST = "text/vnd.exodus.toast"
        const val TYPE_FATAL = "text/vnd.exodus.fatal"
        const val TYPE_LINK = "text/vnd.exodus.link"
    }

    /**
     * 텍스트 메시지 여부
     */
    val isTextMessage: Boolean
        get() = contentType == TYPE_TEXT || contentType == null

    /**
     * 이미지 메시지 여부
     */
    val isImageMessage: Boolean
        get() = contentType == TYPE_IMAGE

    /**
     * 링크 메시지 여부
     */
    val isLinkMessage: Boolean
        get() = contentType == TYPE_LINK || isLinkUrl

    /**
     * 시스템 메시지 여부 (토스트, 치명적 오류)
     */
    val isSystemMessage: Boolean
        get() = contentType == TYPE_TOAST || contentType == TYPE_FATAL

    /**
     * 삭제된 메시지 여부
     */
    val isDeletedMessage: Boolean
        get() = deleted

    /**
     * 신고된 메시지 여부
     */
    val isReportedMessage: Boolean
        get() = reported || contentType == TYPE_REPORTED

    // ============================================================
    // 이미지/이모티콘 content JSON 파싱
    // old 프로젝트: content = {"url": "...", "thumbnail": "...", "is_emoticon": "true"}
    // ============================================================

    /**
     * 이미지 content JSON 파싱 결과 (캐싱)
     */
    @delegate:Ignore
    private val imageContentJson: JSONObject? by lazy {
        if (isImageMessage) {
            try {
                JSONObject(content)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    /**
     * 이모티콘 여부 (is_emoticon = "true")
     */
    val isEmoticon: Boolean
        get() = imageContentJson?.optBoolean("is_emoticon", false) ?: false

    /**
     * 이미지 URL (원본)
     */
    val imageUrl: String?
        get() = imageContentJson?.optString("url")?.takeIf { it.isNotEmpty() }

    /**
     * 썸네일 URL
     */
    val thumbnailUrl: String?
        get() = imageContentJson?.optString("thumbnail")?.takeIf { it.isNotEmpty() }

    /**
     * 움짤 URL
     */
    val umjjalUrl: String?
        get() = imageContentJson?.optString("umjjal")?.takeIf { it.isNotEmpty() }

    /**
     * 썸네일 높이
     */
    val thumbHeight: Int
        get() = imageContentJson?.optString("thumb_height")?.toIntOrNull() ?: 0

    /**
     * 썸네일 너비
     */
    val thumbWidth: Int
        get() = imageContentJson?.optString("thumb_width")?.toIntOrNull() ?: 0

    /**
     * GIF 파일 여부
     */
    val isGif: Boolean
        get() {
            val url = displayImageUrl ?: return false
            return url.substringAfterLast(".").lowercase() == "gif"
        }

    /**
     * 표시할 이미지 URL (썸네일 우선, JSON 파싱 실패 시 content 자체 사용)
     * - JSON 형식: {"url": "...", "thumbnail": "..."} -> thumbnailUrl 또는 imageUrl 사용
     * - URL만 저장된 경우: content 자체를 URL로 사용 (이전 버전 호환)
     */
    val displayImageUrl: String?
        get() {
            // 1. JSON에서 썸네일 또는 원본 URL 추출
            val jsonUrl = thumbnailUrl ?: imageUrl
            if (!jsonUrl.isNullOrEmpty()) return jsonUrl

            // 2. JSON 파싱 실패 시 content 자체가 URL인지 확인 (이전 버전 호환)
            if (isImageMessage && content.startsWith("http")) {
                return content
            }
            return null
        }

    /**
     * 이모티콘 URL에서 emoticonId 추출
     *
     * 서버에서 받은 URL 형식: http://idol-emoticon.gcdn.ntruss.com/21/21_45.webp
     * - 마지막 파일명에서 _ 뒤의 숫자가 emoticonId (예: 45)
     * - 또는 /e/45.t.webp 형식에서 45 추출
     */
    val emoticonIdFromUrl: Int?
        get() {
            val url = thumbnailUrl ?: imageUrl ?: return null

            // 1. /e/{id}.t.webp 형식 (올바른 CDN URL)
            val cdnPattern = Regex("/e/(\\d+)\\.t\\.webp")
            cdnPattern.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }

            // 2. {setId}_{emoticonId}.webp 형식 (이전 형식)
            val filename = url.substringAfterLast("/").substringBeforeLast(".")
            if (filename.contains("_")) {
                filename.substringAfterLast("_").toIntOrNull()?.let { return it }
            }

            // 3. 순수 숫자만 있는 경우 (예: 45.webp)
            filename.toIntOrNull()?.let { return it }

            return null
        }

    /**
     * cdnUrl을 사용하여 올바른 이모티콘 URL 생성
     * old 프로젝트의 UtilK.fileImageUrl() 형식: ${cdnUrl}/e/${emoticonId}.t.webp
     *
     * @param cdnUrl CDN 베이스 URL
     * @return 올바른 이모티콘 URL, 이모티콘이 아니거나 ID를 추출할 수 없으면 null
     */
    fun getCorrectEmoticonUrl(cdnUrl: String): String? {
        if (!isEmoticon || cdnUrl.isEmpty()) return null
        val emoticonId = emoticonIdFromUrl ?: return null
        return "$cdnUrl/e/$emoticonId.t.webp"
    }
}
