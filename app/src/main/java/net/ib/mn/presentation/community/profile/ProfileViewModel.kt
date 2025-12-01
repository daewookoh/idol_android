package net.ib.mn.presentation.community.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.data.repository.UsersRepository
import net.ib.mn.util.LocaleUtil

/**
 * ProfileViewModel - 유저 프로필 ViewModel
 *
 * Old 프로젝트의 FeedActivity 로직을 참고하여 구현
 * - 본인 프로필(isMine=true): UserCacheRepository에서 most 정보 조회
 * - 타인 프로필(isMine=false): 전달받은 mostIdolName 사용
 */
class ProfileViewModel(
    private val context: Context,
    private val usersRepository: UsersRepository,
    private val userCacheRepository: UserCacheRepository,
    private val userId: Int,
    userNickname: String,
    userImageUrl: String?,
    userLevel: Int,
    mostIdolName: String?,
    private val isMine: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var currentData = ProfileData(
        id = userId,
        nickname = userNickname,
        imageUrl = userImageUrl,
        level = userLevel,
        idolName = mostIdolName,
        statusMessage = null,
        isFeedPrivate = false
    )

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Success(user = currentData)

            // 본인 프로필이고 most가 없으면 캐시에서 로드
            if (isMine && currentData.idolName.isNullOrEmpty()) {
                loadMostFromCache()?.let { idolName ->
                    currentData = currentData.copy(idolName = idolName)
                    _uiState.value = ProfileUiState.Success(user = currentData)
                }
            }

            // getStatus API에서 상태 메시지 및 비공개 여부 로드
            loadStatusInfo()
        }
    }

    /** getStatus API 호출하여 상태 메시지 및 피드 비공개 여부 로드 */
    private suspend fun loadStatusInfo() {
        try {
            val json = usersRepository.getStatus(userId).getOrNull() ?: return
            if (!json.optBoolean("success")) return

            val statusMessage = json.optString("status_message", "").takeIf { it.isNotEmpty() && it != "null" }
            val feedIsViewable = json.optString("feed_is_viewable", "Y")
            val isFeedPrivate = feedIsViewable == "N" && !isMine

            currentData = currentData.copy(
                statusMessage = statusMessage,
                isFeedPrivate = isFeedPrivate
            )
            _uiState.value = ProfileUiState.Success(user = currentData)
        } catch (e: Exception) {
            // 에러 무시
        }
    }

    /** 본인 프로필: UserCacheRepository에서 most 정보 조회 */
    private fun loadMostFromCache(): String? {
        val most = userCacheRepository.getUserData()?.most ?: return null
        return LocaleUtil.getLocalizedIdolName(context, most).takeIf { it.isNotEmpty() }
    }
}

/** ProfileUiState - UI 상태 */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: ProfileData) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

/** ProfileData - 유저 프로필 데이터 */
data class ProfileData(
    val id: Int,
    val nickname: String,
    val imageUrl: String?,
    val level: Int,
    val idolName: String?,
    val statusMessage: String?,
    val isFeedPrivate: Boolean = false
)

/** ProfileViewModelFactory */
class ProfileViewModelFactory(
    private val context: Context,
    private val usersRepository: UsersRepository,
    private val userCacheRepository: UserCacheRepository,
    private val userId: Int,
    private val userNickname: String,
    private val userImageUrl: String?,
    private val userLevel: Int,
    private val mostIdolName: String?,
    private val isMine: Boolean
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(
            context, usersRepository, userCacheRepository,
            userId, userNickname, userImageUrl, userLevel, mostIdolName, isMine
        ) as T
    }
}
