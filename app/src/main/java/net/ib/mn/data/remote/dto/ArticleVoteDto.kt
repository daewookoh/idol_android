package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 게시글 투표 요청
 * Old 프로젝트의 GiveHeartToArticleDTO와 동일
 */
data class ArticleVoteRequest(
    @SerializedName("article_id")
    val articleId: String,

    @SerializedName("number")
    val hearts: Long
)

/**
 * 게시글 투표 응답
 * Old 프로젝트의 GiveHeartModel과 동일
 */
data class ArticleVoteResponse(
    @SerializedName("bonus_heart")
    val bonusHeart: Int? = null,

    @SerializedName("event_heart")
    val eventHeart: Boolean = false,

    @SerializedName("event_heart_count")
    val eventHeartCount: Int = 0,

    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("success")
    val success: Boolean = false
)

/**
 * 게시글 좋아요 요청
 * Old 프로젝트의 LikeArticleDTO와 동일
 */
data class ArticleLikeRequest(
    @SerializedName("article_id")
    val articleId: String,

    @SerializedName("like")
    val like: Boolean
)

/**
 * 게시글 좋아요 응답
 * Old 프로젝트의 ArticleLikeModel과 동일
 */
data class ArticleLikeResponse(
    @SerializedName("gcode")
    val gcode: Int = 0,

    @SerializedName("liked")
    val liked: Boolean = false,

    @SerializedName("success")
    val success: Boolean = false
)

/**
 * 피드/자유게시판 게시글 작성 요청
 * Old 프로젝트의 CreateArticleDTO와 동일
 * API: POST articles/create/
 */
data class CreateArticleRequest(
    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("idol_id")
    val idolId: String,

    @SerializedName("link_title")
    val linkTitle: String = "",

    @SerializedName("link_desc")
    val linkDesc: String = "",

    @SerializedName("link_url")
    val linkUrl: String = "",

    @SerializedName("show")
    val show: String = "A",  // A: 전체공개, M: 최애공개

    @SerializedName("files")
    val files: List<FileData> = emptyList(),

    @SerializedName("tag_id")
    val tagId: String = "1"  // 기본값: 자유 태그
)

/**
 * 덕질게시판(팬톡) 게시글 작성 요청
 * Old 프로젝트의 InsertArticleDTO와 동일
 * API: POST articles/insert/
 */
data class InsertArticleRequest(
    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("idol_id")
    val idolId: String,

    @SerializedName("link_title")
    val linkTitle: String = "",

    @SerializedName("link_desc")
    val linkDesc: String = "",

    @SerializedName("link_url")
    val linkUrl: String = "",

    @SerializedName("show_scope")
    val showScope: String = "A",  // A: 전체공개, M: 최애공개

    @SerializedName("files")
    val files: List<FileData> = emptyList()
)

/**
 * 파일 데이터
 * Old 프로젝트의 FileData와 동일
 */
data class FileData(
    @SerializedName("seq")
    val seq: Int = 0,

    @SerializedName("size")
    val size: Long = 0,

    @SerializedName("saved_filename")
    val savedFilename: String = "",

    @SerializedName("origin_name")
    val originName: String = ""
)

/**
 * 게시글 작성 응답
 * Old 프로젝트의 GenericArticleResponse와 동일
 */
data class CreateArticleResponse(
    @SerializedName("gcode")
    val gcode: Int = 0,

    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("article_id")
    val articleId: Long? = null,

    @SerializedName("provide")
    val provide: Long = 0  // 하트 보상
) {
    companion object {
        const val GCODE_SUCCESS = 0                // 게시글 작성 성공
        const val GCODE_SUCCESS_WITH_HEART = 1     // 게시글 작성 성공 + 하트 보상
        const val GCODE_UPDATE_SUCCESS = 2000      // 게시글 수정 성공
    }
}
