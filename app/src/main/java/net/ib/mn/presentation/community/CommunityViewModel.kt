package net.ib.mn.presentation.community

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.ib.mn.data.repository.ReportRepository
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.data.repository.UsersRepository
import net.ib.mn.data.repository.WikiRepository
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.FavoritesRepository
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.tutorial.TutorialRepository
import net.ib.mn.util.IdolImageUtil
import net.ib.mn.util.RankingUtil
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommunityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val usersRepository: UsersRepository,
    val userCacheRepository: UserCacheRepository,
    val reportRepository: ReportRepository,
    private val favoritesRepository: FavoritesRepository,
    private val chartRankingRepository: net.ib.mn.data.repository.ChartRankingRepository,
    val wikiRepository: WikiRepository,
    private val idolRepository: IdolRepository,
    private val tutorialRepository: TutorialRepository
) : ViewModel() {

    private val _isUpdatingMost = MutableStateFlow(false)
    val isUpdatingMost: StateFlow<Boolean> = _isUpdatingMost.asStateFlow()

    private val _isUpdatingFavorite = MutableStateFlow(false)
    val isUpdatingFavorite: StateFlow<Boolean> = _isUpdatingFavorite.asStateFlow()

    // 현재 관찰 중인 idolId
    private val _currentIdolId = MutableStateFlow<Int?>(null)

    /**
     * 아이돌 실시간 데이터 클래스
     * 로컬 DB에서 Flow로 관찰되며, 필드 추가 시 확장 가능
     */
    data class IdolData(
        val id: String = "",
        val name: String = "",
        val nameEn: String? = null,
        val photoUrl: String? = null,
        val fandomName: String? = null,
        val groupId: Int? = null,
        val category: String? = null,
        val miracleCount: Int = 0,
        val fairyCount: Int = 0,
        val angelCount: Int = 0,
        val rookieCount: Int = 0,
        val anniversary: String? = null,
        val anniversaryDays: Int = 0,
        val top3ImageUrls: List<String?> = listOf(null, null, null),
        val top3VideoUrls: List<String?> = listOf(null, null, null),
        val mostCount: Int = 0,
        val heartCount: Long = 0,
        val birthday: String? = null,
        val resourceUri: String? = null,
        val sourceApp: String? = null
    )

    /**
     * 아이돌 실시간 데이터를 Flow로 관찰
     * idol 데이터가 변경되면 자동으로 업데이트됩니다.
     */
    val idolDataFlow: StateFlow<IdolData?> = _currentIdolId
        .flatMapLatest { idolId ->
            if (idolId == null || idolId <= 0) {
                flowOf(null)
            } else {
                idolRepository.observeIdolById(idolId).map { idol ->
                    idol?.let {
                        IdolData(
                            id = it.id.toString(),
                            name = RankingUtil.getLocalizedName(it, context),
                            nameEn = it.nameEn,
                            photoUrl = it.imageUrl,
                            fandomName = RankingUtil.getLocalizedFandomName(it, context),
                            groupId = it.groupId,
                            category = it.category,
                            miracleCount = it.miracleCount,
                            fairyCount = it.fairyCount,
                            angelCount = it.angelCount,
                            rookieCount = it.rookieCount,
                            anniversary = it.anniversary.takeIf { ann -> ann != "N" },
                            anniversaryDays = it.anniversaryDays ?: 0,
                            top3ImageUrls = IdolImageUtil.getTop3ImageUrls(it),
                            top3VideoUrls = IdolImageUtil.getTop3VideoUrls(it),
                            mostCount = it.mostCount,
                            heartCount = it.heart,
                            birthday = RankingUtil.formatBirthday(it.birthDay, it.isLunarBirthday, context),
                            resourceUri = it.resourceUri,
                            sourceApp = it.sourceApp
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * 관찰할 아이돌 ID 설정
     * CommunityScreen에서 idolId가 변경될 때 호출됩니다.
     */
    fun setIdolId(idolId: Int) {
        _currentIdolId.value = idolId
    }

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
     * @param idolData 아이돌 실시간 데이터
     * @param currentIsMost 현재 최애 상태
     * @param onSuccess 성공 시 콜백 (새 최애 상태 전달)
     * @param onError 에러 시 콜백 (에러 메시지 전달)
     */
    suspend fun updateMostIdol(
        idolData: IdolData,
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
            idolData.resourceUri ?: "/api/v1/idols/${idolData.id}/"
        }

        val result = usersRepository.updateMost(userResourceUri, idolResourceUri)
        result.onSuccess {
            val newIsMost = !currentIsMost

            // 로컬 캐시 업데이트
            val newMostIdolId: Int?
            val newMostIdolChartCode: String?

            if (newIsMost) {
                newMostIdolId = idolData.id.toIntOrNull()
                newMostIdolChartCode = idolData.sourceApp
                userCacheRepository.updateMostIdolCache(
                    idolId = newMostIdolId,
                    idolCategory = idolData.category,
                    idolChartCode = newMostIdolChartCode
                )
                // 최애 설정 시 즐겨찾기 캐시에도 추가 (Old 프로젝트와 동일 - 서버에서 자동 처리됨)
                newMostIdolId?.let { idolId ->
                    if (!userCacheRepository.getFavoriteIdolIds().contains(idolId)) {
                        // favorite API 호출 없이 캐시만 추가 (서버에서 자동 처리)
                        // addFavoriteToCache에 favoriteId가 필요하지만,
                        // 서버에서 자동 추가된 경우 favoriteId를 모르므로 -1로 임시 설정
                        // (다음 favorites/self API 호출 시 정확한 ID로 갱신됨)
                        userCacheRepository.addFavoriteToCache(idolId, -1)
                    }
                }
            } else {
                newMostIdolId = null
                newMostIdolChartCode = null
                userCacheRepository.updateMostIdolCache(
                    idolId = null,
                    idolCategory = null,
                    idolChartCode = null
                )
            }

            // ChartRankingRepository의 mostFavoriteIdolRankingItem 즉시 업데이트
            // → MyFavoritePage에서 실시간으로 반영됨
            chartRankingRepository.updateMostFavoriteIdol(newMostIdolId, newMostIdolChartCode)

            onSuccess(newIsMost)
        }.onFailure { error ->
            onError(error.message)
        }

        _isUpdatingMost.value = false
    }

    /**
     * 즐겨찾기 추가 API 호출
     *
     * @param idolId 아이돌 ID
     * @param onSuccess 성공 시 콜백 (새 즐겨찾기 상태 전달)
     * @param onError 에러 시 콜백 (에러 메시지 전달)
     */
    suspend fun addFavorite(
        idolId: Int,
        onSuccess: (newIsFavorite: Boolean) -> Unit,
        onError: (message: String?) -> Unit
    ) {
        _isUpdatingFavorite.value = true

        favoritesRepository.addFavorite(idolId).collect { result ->
            when (result) {
                is ApiResult.Loading -> { /* 로딩 중 */ }
                is ApiResult.Success -> {
                    val favoriteId = result.data
                    // 로컬 캐시 업데이트
                    userCacheRepository.addFavoriteToCache(idolId, favoriteId)
                    onSuccess(true)
                    _isUpdatingFavorite.value = false
                }
                is ApiResult.Error -> {
                    onError(result.message)
                    _isUpdatingFavorite.value = false
                }
            }
        }
    }

    /**
     * 즐겨찾기 삭제 API 호출
     *
     * @param idolId 아이돌 ID
     * @param onSuccess 성공 시 콜백 (새 즐겨찾기 상태 전달)
     * @param onError 에러 시 콜백 (에러 메시지 전달)
     */
    suspend fun removeFavorite(
        idolId: Int,
        onSuccess: (newIsFavorite: Boolean) -> Unit,
        onError: (message: String?) -> Unit
    ) {
        _isUpdatingFavorite.value = true

        // favorite ID 가져오기
        val favoriteId = userCacheRepository.getFavoriteId(idolId)
        if (favoriteId == null) {
            _isUpdatingFavorite.value = false
            onError("Favorite ID not found")
            return
        }

        favoritesRepository.removeFavorite(favoriteId).collect { result ->
            when (result) {
                is ApiResult.Loading -> { /* 로딩 중 */ }
                is ApiResult.Success -> {
                    // 로컬 캐시 업데이트
                    userCacheRepository.removeFavoriteFromCache(idolId)
                    onSuccess(false)
                    _isUpdatingFavorite.value = false
                }
                is ApiResult.Error -> {
                    onError(result.message)
                    _isUpdatingFavorite.value = false
                }
            }
        }
    }

    /**
     * 즐겨찾기 토글 (추가/삭제)
     *
     * @param idolId 아이돌 ID
     * @param currentIsFavorite 현재 즐겨찾기 상태
     * @param onSuccess 성공 시 콜백 (새 즐겨찾기 상태 전달)
     * @param onError 에러 시 콜백 (에러 메시지 전달)
     */
    suspend fun toggleFavorite(
        idolId: Int,
        currentIsFavorite: Boolean,
        onSuccess: (newIsFavorite: Boolean) -> Unit,
        onError: (message: String?) -> Unit
    ) {
        if (currentIsFavorite) {
            removeFavorite(idolId, onSuccess, onError)
        } else {
            addFavorite(idolId, onSuccess, onError)
        }
    }

    /**
     * 튜토리얼 완료 처리
     */
    fun updateTutorial(tutorialIndex: Int) {
        viewModelScope.launch {
            tutorialRepository.updateTutorial(tutorialIndex)
        }
    }
}
