package net.ib.mn.presentation.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.domain.manager.ArticleUpdateEvent
import net.ib.mn.domain.manager.ArticleUpdateManager
import net.ib.mn.domain.model.ArticleModel
import javax.inject.Inject

/**
 * ArticleDetailWrapper의 ViewModel
 * ArticleUpdateManager를 통해 게시글 업데이트 이벤트를 전파
 */
@HiltViewModel
class ArticleDetailWrapperViewModel @Inject constructor(
    private val articleUpdateManager: ArticleUpdateManager
) : ViewModel() {

    /**
     * 게시글 업데이트 이벤트 발행
     */
    suspend fun emitArticleUpdate(article: ArticleModel) {
        articleUpdateManager.emitUpdate(
            ArticleUpdateEvent.Updated(
                articleId = article.id,
                likeCount = article.likeCount,
                commentCount = article.commentCount,
                heart = article.heart
            )
        )
    }

    /**
     * 게시글 삭제 이벤트 발행
     */
    suspend fun emitArticleDeleted(articleId: String) {
        articleUpdateManager.emitUpdate(
            ArticleUpdateEvent.Deleted(articleId = articleId)
        )
    }
}
