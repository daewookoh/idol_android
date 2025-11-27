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
