package net.ib.mn.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 투표 후 응답
 */
@Serializable
data class GenericArticleResponse (
    var gcode: Int = 0,
    @SerialName("article_id") var articleId: Long = 0,
    var provide: Long = 0,
    var msg: String? = null,
    var success: Boolean = false,
)
