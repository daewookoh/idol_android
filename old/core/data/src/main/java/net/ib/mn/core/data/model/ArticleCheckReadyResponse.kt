package net.ib.mn.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 투표 후 응답
 */
@Serializable
data class ArticleCheckReadyResponse (
    var gcode: Int = 0,
    @SerialName("article_id") var articleId: Int = 0,
    var reward: Int = 0,
    var msg: String? = null,
    var success: Boolean = false,
)
