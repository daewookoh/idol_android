package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName
import java.util.Date

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
    val createdAt: Date = Date(),

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
)

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
 */
data class ArticleFile(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("file_url")
    val fileUrl: String? = null,

    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @SerializedName("file_type")
    val fileType: String? = null, // image, video, etc.

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0
)
