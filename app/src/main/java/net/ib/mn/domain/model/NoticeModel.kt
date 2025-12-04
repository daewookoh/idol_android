package net.ib.mn.domain.model

import androidx.compose.runtime.Immutable

/**
 * Notice Presentation Model (공지사항/고정글)
 * - old 프로젝트의 NoticeEventModel과 동일한 구조
 * - Immutable data class for Compose recomposition optimization
 */
@Immutable
data class NoticeModel(
    val id: String,
    val title: String,
    val content: String = "",
    val contentHtml: String = "",
    val createdAt: Long = 0L, // Timestamp in milliseconds
    val isOpen: Boolean = false
) {
    /**
     * NoticeModel을 ArticleModel로 변환
     * ArticleDetailScreen에서 사용하기 위함
     */
    fun toArticleModel(): ArticleModel = ArticleModel(
        id = id,
        title = title,
        content = content,
        contentHtml = contentHtml
    )
}
