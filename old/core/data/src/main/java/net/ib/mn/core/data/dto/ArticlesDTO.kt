package net.ib.mn.core.data.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONArray

@Serializable
data class GiveHeartToArticleDTO(
    @SerialName("article_id") var articleId: String,
    @SerialName("number") var hearts: Long
)

@Serializable
data class LikeArticleDTO(
    @SerialName("article_id") var articleId: String,
    @SerialName("like") var like: Boolean
)

/**
 * 일반 게시글 작성시 사용
 */
@Serializable
data class CreateArticleDTO(
    @SerialName("title") var title: String,
    @SerialName("content") var content: String,
    @SerialName("idol_id") var idolId: String,
    @SerialName("link_title") var linkTitle: String,
    @SerialName("link_desc") var linkDesc: String,
    @SerialName("link_url") var linkUrl: String,
    @SerialName("show") var show: String,
    @SerialName("files") var files: List<FileData> = emptyList(),
    @SerialName("tag_id") var tagId: String
)

/**
 * 덕질게시판 전용
 */

@Serializable
data class InsertArticleDTO(
    @SerialName("title") var title: String,
    @SerialName("content") var content: String,
    @SerialName("idol_id") var idolId: String,
    @SerialName("link_title") var linkTitle: String,
    @SerialName("link_desc") var linkDesc: String,
    @SerialName("link_url") var linkUrl: String,
    @SerialName("show_scope") var showScope: String,
    @SerialName("files") var files: List<FileData> = emptyList(),
)

@Serializable
@Parcelize
data class FileData(
    val seq: Int = 0,
    val size: Long = 0,
    @SerialName("saved_filename") val savedFilename: String = "",
    @SerialName("origin_name") val originName: String = ""
) : Parcelable

@Serializable
data class UpdateArticleDTO(
    @SerialName("article_id") var articleId: String,
    @SerialName("content") var content: String,
    @SerialName("title") var title: String? = null,
    @SerialName("link_title") var linkTitle: String? = null,
    @SerialName("link_desc") var linkDesc: String? = null,
    @SerialName("link_url") var linkUrl: String? = null,
    @SerialName("show") var show: String,
    @SerialName("tag_id") var tagId: String? = null
)

@Serializable
data class DownloadArticleDTO(
    @SerialName("article_id") var articleId: Long,
    @SerialName("seq") var seq: Int
)

@Serializable
data class ViewCountDTO(
    @SerialName("article_id") var articleId: Long
)

