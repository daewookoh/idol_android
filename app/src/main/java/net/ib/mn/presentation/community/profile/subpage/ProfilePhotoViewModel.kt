package net.ib.mn.presentation.community.profile.subpage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.repository.UsersRepository
import javax.inject.Inject

/**
 * ProfilePhotoViewModel - 프로필 사진 탭 ViewModel
 *
 * Old 프로젝트의 FeedActivity.getFeedPhoto() 로직을 참고
 */
@HiltViewModel
class ProfilePhotoViewModel @Inject constructor(
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfilePhotoUiState>(ProfilePhotoUiState.Loading)
    val uiState: StateFlow<ProfilePhotoUiState> = _uiState.asStateFlow()

    private var userId: Int = 0
    private var currentOffset: Int = 0
    private val limit: Int = 30
    private val photos = mutableListOf<ProfilePhotoItem>()

    /**
     * 사진 로드
     */
    fun loadPhotos(userId: Int) {
        if (this.userId == userId && photos.isNotEmpty()) {
            // 이미 로드된 경우 스킵
            return
        }

        this.userId = userId
        currentOffset = 0
        photos.clear()
        _uiState.value = ProfilePhotoUiState.Loading

        fetchPhotos()
    }

    /**
     * 더 많은 사진 로드
     */
    fun loadMore() {
        if (_uiState.value is ProfilePhotoUiState.Loading) return
        fetchPhotos()
    }

    private fun fetchPhotos() {
        viewModelScope.launch {
            // TODO: API 구현 필요 - articlesRepository.getFeedActivity(userId, "photo", offset, limit, isMine)
            // 현재는 빈 목록으로 Empty 상태 표시
            _uiState.value = ProfilePhotoUiState.Empty
        }
    }
}

/**
 * ProfilePhotoUiState - UI 상태
 */
sealed interface ProfilePhotoUiState {
    data object Loading : ProfilePhotoUiState
    data object Empty : ProfilePhotoUiState
    data class Success(val photos: List<ProfilePhotoItem>) : ProfilePhotoUiState
    data class Error(val message: String) : ProfilePhotoUiState
}

/**
 * ProfilePhotoItem - 사진 데이터
 */
data class ProfilePhotoItem(
    val id: String,
    val thumbnailUrl: String?,
    val originalUrl: String?
)
