package net.ib.mn.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.remote.dto.ArticleVoteResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.ArticleTranslateState
import net.ib.mn.domain.repository.ArticlesRepository
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import org.json.JSONArray
import javax.inject.Inject

/**
 * ExoArticleViewModel - ExoArticle 컴포넌트의 모든 액션을 처리하는 ViewModel
 *
 * 네비게이션, API 호출, 다이얼로그 등 모든 액션을 중앙에서 관리
 */
private const val LEVEL_ADMIN = 10

@HiltViewModel
class ExoArticleViewModel @Inject constructor(
    private val articlesRepository: ArticlesRepository,
    private val userCacheRepository: net.ib.mn.data.repository.UserCacheRepository,
    private val reportRepository: net.ib.mn.data.repository.ReportRepository,
    private val configRepository: net.ib.mn.domain.repository.ConfigRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    /**
     * 현재 로그인한 사용자 ID
     */
    val myUserId: Int?
        get() = userCacheRepository.getUserData()?.id

    /**
     * 현재 로그인한 사용자가 관리자인지 여부 (heart == 10)
     */
    val isAdmin: Boolean
        get() = userCacheRepository.getUserData()?.heart == LEVEL_ADMIN

    /**
     * 신고 시 차감될 하트 수
     */
    val reportHeart: Int
        get() = configRepository.getReportHeart()

    /**
     * tagId로 태그 이름 찾기 (FREE_BOARD에서 사용)
     */
    fun getTagName(tagId: Int): String? {
        val tagsJson = preferencesManager.getBoardTagsSync() ?: return null
        return try {
            val jsonArray = JSONArray(tagsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optInt("id") == tagId) {
                    return obj.optString("name", null)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

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
    fun navigateToArticleDetail(article: ArticleModel, isFeed: Boolean = true) {
        viewModelScope.launch {
            _navigationEvent.emit(
                ExoArticleNavigation.ArticleDetail(
                    articleId = article.id,
                    article = article,
                    isFeed = isFeed
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
     * 공지사항 상세 화면으로 이동
     * API에서 상세 내용 (contentHtml)을 가져온 후 네비게이션
     *
     * @param article 공지사항 기본 정보
     * @param isDarkMode 다크모드 여부 (서버에서 다크모드용 HTML 반환)
     */
    fun navigateToNoticeDetail(article: ArticleModel, isDarkMode: Boolean = false) {
        viewModelScope.launch {
            val noticeId = article.id.toIntOrNull()
            if (noticeId != null) {
                articlesRepository.getNotice(noticeId, isDarkMode).collect { result ->
                    when (result) {
                        is net.ib.mn.domain.model.ApiResult.Success -> {
                            _navigationEvent.emit(
                                ExoArticleNavigation.NoticeDetail(
                                    article = result.data.toArticleModel()
                                )
                            )
                        }
                        is net.ib.mn.domain.model.ApiResult.Error -> {
                            _navigationEvent.emit(
                                ExoArticleNavigation.NoticeDetail(article = article)
                            )
                        }
                        is net.ib.mn.domain.model.ApiResult.Loading -> { /* 로딩 상태 무시 */ }
                    }
                }
            } else {
                _navigationEvent.emit(
                    ExoArticleNavigation.NoticeDetail(article = article)
                )
            }
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
     * 게시글 수정 화면으로 이동
     */
    fun onEditArticle(article: ArticleModel) {
        viewModelScope.launch {
            _navigationEvent.emit(ExoArticleNavigation.EditArticle(article = article))
        }
    }

    /**
     * 게시글 삭제
     */
    fun onDeleteArticle(article: ArticleModel) {
        viewModelScope.launch {
            _dialogEvent.emit(ExoArticleDialog.DeleteArticle(article = article))
        }
    }

    /**
     * 게시글 신고
     */
    fun onReportArticle(article: ArticleModel) {
        viewModelScope.launch {
            _dialogEvent.emit(ExoArticleDialog.ReportArticle(article = article))
        }
    }

    /**
     * 게시글 공유
     */
    fun onShareArticle(article: ArticleModel) {
        viewModelScope.launch {
            _dialogEvent.emit(ExoArticleDialog.ShareArticle(article = article))
        }
    }

    /**
     * 콘텐츠 번역 다이얼로그 표시
     *
     * @param content 번역할 콘텐츠
     * @param nation 원본 언어 국가 코드
     */
    fun translateContent(content: String, nation: String?) {
        viewModelScope.launch {
            _dialogEvent.emit(ExoArticleDialog.Translation(content = content, nation = nation))
        }
    }

    /**
     * 게시글 번역
     * Old 프로젝트의 ITranslation.translateArticle과 동일한 로직
     *
     * @param article 번역할 게시글
     * @param onArticleUpdated 번역 완료 후 업데이트된 게시글 콜백
     */
    fun translateArticle(article: ArticleModel, onArticleUpdated: (ArticleModel) -> Unit) {
        val articleId = article.id.toLongOrNull() ?: return

        if (article.translateState == ArticleTranslateState.ORIGINAL) {
            // 번역 시작 - TRANSLATING 상태로 변경
            article.originalContent = article.content
            article.originalTitle = article.title
            article.translateState = ArticleTranslateState.TRANSLATING
            onArticleUpdated(article)

            viewModelScope.launch {
                articlesRepository.translateArticle(articleId).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            val translatedArticle = result.data
                            // 번역된 내용을 현재 article에 적용
                            article.translateState = ArticleTranslateState.TRANSLATED
                            // 주의: data class의 val 필드는 직접 수정 불가하므로 copy를 사용하거나
                            // 번역된 content를 다이얼로그로 보여주는 방식 사용
                            // 여기서는 이벤트로 번역된 article 전달
                            onArticleUpdated(article.copy(
                                content = translatedArticle.content,
                                title = translatedArticle.title,
                                translateState = ArticleTranslateState.TRANSLATED,
                                originalContent = article.originalContent,
                                originalTitle = article.originalTitle
                            ))
                            logD("ExoArticleViewModel", "Article translated: $articleId")
                        }
                        is ApiResult.Error -> {
                            // 번역 실패 - 원래 상태로 복원
                            article.translateState = ArticleTranslateState.ORIGINAL
                            onArticleUpdated(article)
                            logE("ExoArticleViewModel", "Failed to translate article: ${result.error.message}")
                        }
                        else -> { /* skip */ }
                    }
                }
            }
        } else {
            // 이미 번역된 상태 - 원문으로 복원
            onArticleUpdated(article.copy(
                content = article.originalContent,
                title = article.originalTitle,
                translateState = ArticleTranslateState.ORIGINAL
            ))
        }
    }

    /**
     * 게시글 상세 정보 조회 (최신 데이터)
     */
    fun loadArticle(
        articleId: Long,
        onSuccess: (ArticleModel) -> Unit,
        onError: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val article = articlesRepository.getArticle(articleId)
                onSuccess(article)
            } catch (e: Exception) {
                onError?.invoke()
            }
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

    /**
     * 게시글 신고 API 호출
     */
    fun reportArticle(
        articleId: String,
        onSuccess: () -> Unit,
        onError: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val articleIdLong = articleId.toLongOrNull()
            if (articleIdLong == null) {
                onError(0)
                return@launch
            }
            when (val result = reportRepository.reportArticle(articleIdLong)) {
                is net.ib.mn.data.repository.ReportResult.Success -> onSuccess()
                is net.ib.mn.data.repository.ReportResult.Error -> onError(result.gcode)
            }
        }
    }

    /**
     * 게시글 삭제 API 호출
     */
    fun deleteArticle(
        articleId: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            val articleIdLong = articleId.toLongOrNull()
            if (articleIdLong == null) {
                onError()
                return@launch
            }
            articlesRepository.deleteArticle(articleIdLong).collect { result ->
                when (result) {
                    is net.ib.mn.domain.model.ApiResult.Success -> {
                        if (result.data) {
                            onSuccess()
                        } else {
                            onError()
                        }
                    }
                    is net.ib.mn.domain.model.ApiResult.Error -> onError()
                    is net.ib.mn.domain.model.ApiResult.Loading -> { /* 로딩 상태 무시 */ }
                }
            }
        }
    }

    companion object {
        // 게시글 신고 gcode
        const val GCODE_ALREADY_REPORTED = 2201
        const val GCODE_DAILY_LIMIT = 2202
        const val GCODE_TIME_LIMIT = 2203
        const val GCODE_NOT_ENOUGH_HEART = 2204
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
        val article: ArticleModel,
        val isFeed: Boolean = true
    ) : ExoArticleNavigation

    data class MediaDetail(
        val article: ArticleModel,
        val mediaIndex: Int
    ) : ExoArticleNavigation

    data class Community(
        val idolId: Int
    ) : ExoArticleNavigation

    data class NoticeDetail(
        val article: ArticleModel
    ) : ExoArticleNavigation

    data class EditArticle(
        val article: ArticleModel
    ) : ExoArticleNavigation
}

/**
 * ExoArticle 다이얼로그/바텀시트 이벤트
 */
sealed interface ExoArticleDialog {
    data class MoreOptions(val article: ArticleModel) : ExoArticleDialog
    data class Translation(val content: String, val nation: String?) : ExoArticleDialog
    data class DeleteArticle(val article: ArticleModel) : ExoArticleDialog
    data class ReportArticle(val article: ArticleModel) : ExoArticleDialog
    data class ShareArticle(val article: ArticleModel) : ExoArticleDialog
}
