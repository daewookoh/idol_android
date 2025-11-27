package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 게시글 모델
 */
data class ArticleModel(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("content")
    val content: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @SerializedName("umjjal_url")
    val umjjalUrl: String? = null,

    @SerializedName("heart")
    val heart: Long = 0,

    @SerializedName("like_count")
    val likeCount: Int = 0,

    @SerializedName("num_comments")
    val commentCount: Int = 0,

    @SerializedName("view_count")
    val viewCount: Int = 0,

    @SerializedName("report_count")
    val reportCount: Int = 0,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("resource_uri")
    val resourceUri: String = "",

    @SerializedName("is_viewable")
    val isViewable: String? = "Y",

    @SerializedName("is_most_only")
    val isMostOnly: String? = null,

    @SerializedName("is_welcome")
    val isWelcome: String? = null,

    @SerializedName("is_popular")
    val isPopular: Boolean = false,

    @SerializedName("tag_id")
    val tagId: Int = 0,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("nation")
    val nation: String? = null,

    @SerializedName("user_like")
    val isUserLike: Boolean = false,

    // User info (nested)
    @SerializedName("user")
    val user: ArticleUser? = null,

    // Files
    @SerializedName("files")
    val files: List<ArticleFile> = emptyList()
) {
    /**
     * 미디어 파일 목록 반환
     * Old 프로젝트의 BaseArticleViewHolder.setReOrganizeArticleModel() 로직과 동일
     * - files가 비어있으면 imageUrl, thumbnailUrl, umjjalUrl로 생성
     * - seq 순으로 정렬
     */
    val mediaFiles: List<ArticleFile>
        get() {
            return if (files.isEmpty()) {
                // 옛날에 올린 경우, files가 없어서 직접 생성
                val thumbnail = thumbnailUrl ?: imageUrl
                if (thumbnail != null || umjjalUrl != null) {
                    listOf(ArticleFile(
                        originUrl = null,
                        thumbnailUrl = thumbnail,
                        umjjalUrl = umjjalUrl
                    ))
                } else {
                    emptyList()
                }
            } else {
                files.sortedBy { it.seq }
            }
        }
}

/**
 * 게시글 작성자 정보
 */
data class ArticleUser(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("nickname")
    val nickname: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("level")
    val level: Int = 0,

    @SerializedName("resource_uri")
    val resourceUri: String? = null
) {
    val imageUrlCommunity: String
        get() = imageUrl?.replace("/profile/", "/profile_community/") ?: ""
}

/**
 * 게시글 첨부 파일
 * Old 프로젝트의 RemoteFileModel과 호환
 */
data class ArticleFile(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("origin_url")
    val originUrl: String? = null,

    @SerializedName("file_url")
    val fileUrl: String? = null,

    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @SerializedName("umjjal_url")
    val umjjalUrl: String? = null,

    @SerializedName("file_type")
    val fileType: String? = null, // image, video, etc.

    @SerializedName("seq")
    val seq: Int = 1,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0
) {
    /**
     * 비디오 파일인지 확인 (mp4 확장자)
     */
    val isVideo: Boolean
        get() = originUrl?.endsWith(".mp4", ignoreCase = true) == true

    /**
     * GIF/움짤인지 확인 (umjjalUrl이 있으면 GIF)
     */
    val isGif: Boolean
        get() = !umjjalUrl.isNullOrEmpty()

    /**
     * 미디어 타입 (VIDEO, GIF, IMAGE)
     */
    val mediaType: MediaType
        get() = when {
            isVideo -> MediaType.VIDEO
            isGif -> MediaType.GIF
            else -> MediaType.IMAGE
        }

    /**
     * 표시할 이미지/썸네일 URL 반환
     * - 비디오/GIF: thumbnailUrl 사용
     * - 이미지: originUrl > fileUrl > thumbnailUrl
     */
    val displayUrl: String?
        get() = when {
            isVideo || isGif -> thumbnailUrl
            else -> originUrl ?: fileUrl ?: thumbnailUrl
        }

    /**
     * 원본 이미지 URL (고화질)
     */
    val originalUrl: String?
        get() = originUrl ?: fileUrl

    /**
     * 재생할 미디어 URL (GIF는 umjjalUrl, 비디오는 originUrl)
     */
    val playableUrl: String?
        get() = when {
            isGif -> umjjalUrl
            isVideo -> originUrl
            else -> null
        }
}

/**
 * 미디어 타입
 */
enum class MediaType {
    IMAGE,  // 일반 이미지
    GIF,    // 움짤 (umjjalUrl 사용)
    VIDEO   // 동영상 (mp4)
}
