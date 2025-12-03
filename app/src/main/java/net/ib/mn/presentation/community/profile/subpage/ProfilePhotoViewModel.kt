package net.ib.mn.presentation.community.profile.subpage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.repository.ArticlesRepository
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import javax.inject.Inject

private const val PHOTO_TYPE = "image"
private const val PAGE_LIMIT = 30

/**
 * ProfilePhotoViewModel - 프로필 사진 탭 ViewModel
 */
@HiltViewModel
class ProfilePhotoViewModel @Inject constructor(
    private val articlesRepository: ArticlesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfilePhotoUiState>(ProfilePhotoUiState.Loading)
    val uiState: StateFlow<ProfilePhotoUiState> = _uiState.asStateFlow()

    private var userId: Int = 0
    private var isSelf: Boolean = false
    private var currentOffset: Int = 0
    private var hasMoreData: Boolean = true
    private var isLoading: Boolean = false
    private val photos = mutableListOf<ProfilePhotoItem>()
    private val articlesMap = mutableMapOf<String, ArticleModel>()

    /** 게시글 ID로 ArticleModel 조회 */
    fun getArticle(articleId: String): ArticleModel? = articlesMap[articleId]

    fun loadPhotos(userId: Int, isSelf: Boolean = false) {
        if (this.userId == userId && photos.isNotEmpty()) return

        this.userId = userId
        this.isSelf = isSelf
        currentOffset = 0
        hasMoreData = true
        photos.clear()
        articlesMap.clear()
        _uiState.value = ProfilePhotoUiState.Loading
        fetchPhotos()
    }

    fun loadMore() {
        if (isLoading || !hasMoreData) return
        fetchPhotos()
    }

    private fun fetchPhotos() {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            articlesRepository.getFeedActivity(
                userId = userId,
                type = PHOTO_TYPE,
                offset = currentOffset,
                limit = PAGE_LIMIT,
                isSelf = isSelf
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        if (photos.isEmpty()) {
                            _uiState.value = ProfilePhotoUiState.Loading
                        }
                    }
                    is ApiResult.Success -> handleSuccess(result.data.articles, result.data.totalCount)
                    is ApiResult.Error -> handleError(result.message)
                }
            }
        }
    }

    private fun handleSuccess(articles: List<ArticleModel>, totalCount: Int) {
        // 비공개 피드
        if (totalCount == -1) {
            _uiState.value = ProfilePhotoUiState.Private
            isLoading = false
            return
        }

        if (articles.isEmpty()) {
            hasMoreData = false
            if (photos.isEmpty()) {
                _uiState.value = ProfilePhotoUiState.Empty
            }
        } else {
            // ArticleModel 저장 (ArticleDetail 화면 이동용)
            articles.forEach { articlesMap[it.id] = it }
            photos.addAll(articles.map { it.toPhotoItem() })
            currentOffset += articles.size
            hasMoreData = articles.size >= PAGE_LIMIT

            _uiState.value = ProfilePhotoUiState.Success(
                photos = photos.toList(),
                hasMore = hasMoreData
            )
        }
        isLoading = false
    }

    private fun handleError(message: String?) {
        if (photos.isEmpty()) {
            _uiState.value = ProfilePhotoUiState.Error(message ?: "Failed to load photos")
        }
        isLoading = false
    }

    private fun ArticleModel.toPhotoItem(): ProfilePhotoItem {
        // mediaFiles를 사용해야 옛날 게시글(files 비어있음)도 처리됨
        val firstFile = mediaFiles.firstOrNull()
        val originUrl = firstFile?.originUrl
        // GIF 판별: originUrl이 .gif로 끝나거나, umjjalUrl이 있고 originUrl이 mp4가 아닌 경우
        val isGifFile = originUrl?.endsWith(".gif", ignoreCase = true) == true ||
                (firstFile?.umjjalUrl != null && originUrl?.endsWith(".mp4", ignoreCase = true) != true)
        // Video 판별: originUrl이 .mp4로 끝나는 경우 (GIF가 아닌)
        val isVideoFile = originUrl?.endsWith(".mp4", ignoreCase = true) == true && !isGifFile

        return ProfilePhotoItem(
            id = id,
            thumbnailUrl = (firstFile?.thumbnailUrl ?: thumbnailUrl ?: imageUrl).toSecureUrl(),
            originalUrl = (firstFile?.originUrl ?: firstFile?.fileUrl ?: imageUrl).toSecureUrl(),
            isVideo = isVideoFile,
            isGif = isGifFile
        )
    }
}

/** UI 상태 */
sealed interface ProfilePhotoUiState {
    data object Loading : ProfilePhotoUiState
    data object Empty : ProfilePhotoUiState
    data object Private : ProfilePhotoUiState
    data class Success(val photos: List<ProfilePhotoItem>, val hasMore: Boolean = true) : ProfilePhotoUiState
    data class Error(val message: String) : ProfilePhotoUiState
}

/** 사진 아이템 */
data class ProfilePhotoItem(
    val id: String,
    val thumbnailUrl: String?,
    val originalUrl: String?,
    val isVideo: Boolean = false,
    val isGif: Boolean = false
)
