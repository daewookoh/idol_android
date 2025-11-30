package net.ib.mn.presentation.community

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.data.repository.UsersRepository
import net.ib.mn.data.repository.WikiRepository
import net.ib.mn.ui.components.RankingItem
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val usersRepository: UsersRepository,
    private val userCacheRepository: UserCacheRepository,
    val wikiRepository: WikiRepository
) : ViewModel() {

    private val _isUpdatingMost = MutableStateFlow(false)
    val isUpdatingMost: StateFlow<Boolean> = _isUpdatingMost.asStateFlow()

    /**
     * 해당 아이돌이 사용자의 최애인지 확인
     */
    fun isMostIdol(idolId: Int?): Boolean {
        return idolId != null && userCacheRepository.getMostIdolId() == idolId
    }

    /**
     * 해당 아이돌이 사용자의 즐겨찾기에 있는지 확인
     */
    fun isFavoriteIdol(idolId: Int?): Boolean {
        return idolId != null && userCacheRepository.getFavoriteIdolIds().contains(idolId)
    }

    /**
     * 사용자 resource URI 가져오기
     */
    fun getUserResourceUri(): String? {
        return userCacheRepository.getUserResourceUri()
    }

    /**
     * 최애 아이돌 변경 API 호출
     *
     * @param rankingItem 아이돌 정보
     * @param currentIsMost 현재 최애 상태
     * @param onSuccess 성공 시 콜백 (새 최애 상태 전달)
     * @param onError 에러 시 콜백 (에러 메시지 전달)
     */
    suspend fun updateMostIdol(
        rankingItem: RankingItem,
        currentIsMost: Boolean,
        onSuccess: (newIsMost: Boolean) -> Unit,
        onError: (message: String?) -> Unit
    ) {
        _isUpdatingMost.value = true

        val userResourceUri = getUserResourceUri()
        if (userResourceUri == null) {
            _isUpdatingMost.value = false
            onError(null)
            return
        }

        val idolResourceUri = if (currentIsMost) {
            null // 최애 해제
        } else {
            rankingItem.resourceUri ?: "/api/v1/idols/${rankingItem.id}/"
        }

        val result = usersRepository.updateMost(userResourceUri, idolResourceUri)
        result.onSuccess {
            val newIsMost = !currentIsMost

            // 로컬 캐시 업데이트
            if (newIsMost) {
                userCacheRepository.updateMostIdolCache(
                    idolId = rankingItem.id.toIntOrNull(),
                    idolCategory = rankingItem.category,
                    idolChartCode = rankingItem.chartCode
                )
            } else {
                userCacheRepository.updateMostIdolCache(
                    idolId = null,
                    idolCategory = null,
                    idolChartCode = null
                )
            }

            onSuccess(newIsMost)
        }.onFailure { error ->
            onError(error.message)
        }

        _isUpdatingMost.value = false
    }
}
