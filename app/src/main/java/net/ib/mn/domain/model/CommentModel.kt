package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 댓글 모델
 */
data class CommentModel(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("content")
    val content: String? = null,

    @SerializedName("user")
    val user: CommentUser? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("resource_uri")
    val resourceUri: String? = null,

    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @SerializedName("thumb_height")
    val thumbHeight: Int = 0,

    @SerializedName("thumb_width")
    val thumbWidth: Int = 0,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("umjjal_url")
    val umjjalUrl: String? = null,

    @SerializedName("emoticon")
    val emoticonId: Int = NO_EMOTICON_ID,

    @SerializedName("nation")
    val nation: String? = null,

    // 이미지/움짤 여부 (contentAlt 대체)
    @SerializedName("content_alt")
    val contentAlt: ContentAlt? = null,

    // 번역 관련 필드 (UI 상태용, API 응답에는 없음)
    val originalContent: String? = null,
    val translateState: TranslateState = TranslateState.ORIGINAL,
    val isTranslatable: Boolean? = null
) {
    companion object {
        const val NO_EMOTICON_ID: Int = -100
    }

    /** 이미지 댓글인지 여부 */
    val hasImage: Boolean
        get() = contentAlt?.isImage == true || !imageUrl.isNullOrEmpty()

    /** 움짤 댓글인지 여부 */
    val hasUmjjal: Boolean
        get() = contentAlt?.isUmjjal == true || !umjjalUrl.isNullOrEmpty()

    /** 이모티콘 댓글인지 여부 (emoticonId 또는 contentAlt.isEmoticon 확인) */
    val hasEmoticon: Boolean
        get() = emoticonId != NO_EMOTICON_ID || contentAlt?.isEmoticon == true

    /** 실제 이모티콘 ID (emoticonId 또는 contentAlt.emoticonId) */
    val effectiveEmoticonId: Int
        get() = if (emoticonId != NO_EMOTICON_ID) emoticonId else (contentAlt?.emoticonId ?: NO_EMOTICON_ID)
}

/**
 * 댓글 작성자 정보
 */
data class CommentUser(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("nickname")
    val nickname: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("level")
    val level: Int = 0,

    @SerializedName("heart")
    val heart: Int = 0,

    @SerializedName("resource_uri")
    val resourceUri: String? = null,

    @SerializedName("emoticon")
    val emoticon: UserEmoticon? = null,

    // 구매한 뱃지 아이템 정보
    // - 0x01: 보안관 (deprecated - 더 이상 사용 안함)
    // - 0x02: 메신저 (deprecated - 더 이상 사용 안함)
    // - 0x04: 몰빵일 (사용중)
    @SerializedName("item_no")
    val itemNo: Int = 0
) {
    val imageUrlCommunity: String
        get() = imageUrl?.replace("/profile/", "/profile_community/") ?: ""

    /** 몰빵일 뱃지 구매 여부 */
    val hasAllInDayBadge: Boolean
        get() = (itemNo and 0x04) == 0x04
}

/**
 * 컨텐츠 대체 정보 (이미지/움짤/이모티콘 여부)
 * old: contentAlt.kt
 */
data class ContentAlt(
    @SerializedName("is_image")
    val isImage: Boolean = false,

    @SerializedName("is_umjjal")
    val isUmjjal: Boolean = false,

    @SerializedName("is_emoticon")
    val isEmoticon: Boolean = false,

    @SerializedName("emoticon_id")
    val emoticonId: Int? = null,

    @SerializedName("text")
    val text: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("umjjal_url")
    val umjjalUrl: String? = null
)

/**
 * 댓글 페이지네이션 응답
 */
data class CommentsResponse(
    @SerializedName("objects")
    val comments: List<CommentModel> = emptyList(),

    @SerializedName("meta")
    val meta: CommentsMeta? = null
)

/**
 * 댓글 메타 정보 (페이지네이션)
 */
data class CommentsMeta(
    @SerializedName("next")
    val next: String? = null,

    @SerializedName("previous")
    val previous: String? = null,

    @SerializedName("total_count")
    val totalCount: Int = 0,

    @SerializedName("limit")
    val limit: Int = 20,

    @SerializedName("offset")
    val offset: Int = 0
)
