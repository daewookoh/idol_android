package net.ib.mn.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.remote.dto.ArticleVoteResponse
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.repository.ArticlesRepository
import javax.inject.Inject

/**
 * ExoArticleViewModel - ExoArticle 컴포넌트의 모든 액션을 처리하는 ViewModel
 *
 * 네비게이션, API 호출, 다이얼로그 등 모든 액션을 중앙에서 관리
 */
@HiltViewModel
class ExoArticleViewModel @Inject constructor(
    private val articlesRepository: ArticlesRepository
) : ViewModel() {

    // 네비게이션 이벤트
    private val _navigationEvent = MutableSharedFlow<ExoArticleNavigation>()
    val navigationEvent: SharedFlow<ExoArticleNavigation> = _navigationEvent.asSharedFlow()

    // 다이얼로그/바텀시트 이벤트
    private val _dialogEvent = MutableSharedFlow<ExoArticleDialog>()
    val dialogEvent: SharedFlow<ExoArticleDialog> = _dialogEvent.asSharedFlow()

    /**
     * 프로필 화면으로 이동
     */
    fun navigateToProfile(
        userId: Int,
        nickname: String,
        imageUrl: String?,
        level: Int,
        mostIdolName: String?
    ) {
        viewModelScope.launch {
            _navigationEvent.emit(
                ExoArticleNavigation.Profile(
                    userId = userId,
                    nickname = nickname,
                    imageUrl = imageUrl,
                    level = level,
                    mostIdolName = mostIdolName
                )
            )
        }
    }

    /**
     * 게시글 상세 화면으로 이동 (댓글 포함)
     */
    fun navigateToArticleDetail(article: ArticleModel) {
        viewModelScope.launch {
            _navigationEvent.emit(
                ExoArticleNavigation.ArticleDetail(
                    articleId = article.id,
                    article = article
                )
            )
        }
    }

    /**
     * 미디어 상세 화면으로 이동
     */
    fun navigateToMediaDetail(article: ArticleModel, mediaIndex: Int) {
        viewModelScope.launch {
            _navigationEvent.emit(
                ExoArticleNavigation.MediaDetail(
                    article = article,
                    mediaIndex = mediaIndex
                )
            )
        }
    }

    /**
     * 커뮤니티 화면으로 이동
     */
    fun navigateToCommunity(idolId: Int) {
        viewModelScope.launch {
            _navigationEvent.emit(
                ExoArticleNavigation.Community(idolId = idolId)
            )
        }
    }

    /**
     * 더보기 옵션 표시
     */
    fun showMoreOptions(article: ArticleModel) {
        viewModelScope.launch {
            _dialogEvent.emit(
                ExoArticleDialog.MoreOptions(article = article)
            )
        }
    }

    /**
     * 번역 처리
     */
    fun translateContent(content: String, nation: String?) {
        viewModelScope.launch {
            _dialogEvent.emit(
                ExoArticleDialog.Translation(
                    content = content,
                    nation = nation
                )
            )
        }
    }

    /**
     * 좋아요 API 호출
     */
    fun postLike(articleId: String, like: Boolean) {
        viewModelScope.launch {
            try {
                articlesRepository.likeArticle(articleId, like)
            } catch (e: Exception) {
                // 에러 무시 (로컬 상태는 이미 업데이트됨)
            }
        }
    }

    /**
     * 하트 투표 API 호출
     */
    fun voteArticle(
        articleId: String,
        hearts: Long,
        onSuccess: (ArticleVoteResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = articlesRepository.voteArticle(articleId, hearts)
                if (response.success) {
                    onSuccess(response)
                } else {
                    onError(response.msg ?: "투표에 실패했습니다.")
                }
            } catch (e: Exception) {
                onError(e.message ?: "투표에 실패했습니다.")
            }
        }
    }
}

/**
 * ExoArticle 네비게이션 이벤트
 */
sealed interface ExoArticleNavigation {
    data class Profile(
        val userId: Int,
        val nickname: String,
        val imageUrl: String?,
        val level: Int,
        val mostIdolName: String?
    ) : ExoArticleNavigation

    data class ArticleDetail(
        val articleId: String,
        val article: ArticleModel
    ) : ExoArticleNavigation

    data class MediaDetail(
        val article: ArticleModel,
        val mediaIndex: Int
    ) : ExoArticleNavigation

    data class Community(
        val idolId: Int
    ) : ExoArticleNavigation
}

/**
 * ExoArticle 다이얼로그/바텀시트 이벤트
 */
sealed interface ExoArticleDialog {
    data class MoreOptions(val article: ArticleModel) : ExoArticleDialog
    data class Translation(val content: String, val nation: String?) : ExoArticleDialog
}
