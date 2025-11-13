package net.ib.mn.presentation.main.myfavorite

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.repository.RankingCacheRepository
import net.ib.mn.data.repository.UserCacheRepository
import javax.inject.Inject

/**
 * My Favorite ViewModel (최적화 버전)
 *
 * Flow 기반 반응형 아키텍처:
 * - UserCacheRepository.mostFavoriteIdol Flow 구독으로 실시간 업데이트
 * - RankingCacheRepository.observeChartData로 차트별 데이터 자동 반영
 * - 불필요한 API 호출 제거, 캐시 기반 동작
 */
@HiltViewModel
class MyFavoriteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userCacheRepository: UserCacheRepository,
    private val rankingCacheRepository: RankingCacheRepository,
    val rankingRepository: net.ib.mn.domain.repository.RankingRepository
) : BaseViewModel<MyFavoriteContract.State, MyFavoriteContract.Intent, MyFavoriteContract.Effect>() {

    companion object {
        private const val TAG = "MyFavoriteVM"

        // 차트 코드 정의
        private val CHART_CODES = listOf(
            "PR_S_F",  // 여자 개인
            "PR_S_M",  // 남자 개인
            "PR_G_F",  // 여자 그룹
            "PR_G_M",  // 남자 그룹
            "GLOBALS"  // 종합
        )
    }

    // 차트별 섹션 정보
    data class ChartSection(
        val chartCode: String,
        val sectionName: String,
        val favoriteIds: Set<Int> = emptySet()
    )

    private val _chartSections = MutableStateFlow<List<ChartSection>>(emptyList())
    val chartSections: StateFlow<List<ChartSection>> = _chartSections.asStateFlow()

    // 최애 아이돌 정보 (UserCacheRepository Flow 직접 구독)
    val mostFavoriteIdol: StateFlow<MyFavoriteContract.MostFavoriteIdol?> =
        userCacheRepository.mostFavoriteIdol
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    init {
        // 즐겨찾기 ID 변경 감지하여 자동으로 섹션 업데이트
        observeFavoriteChanges()
    }

    override fun createInitialState(): MyFavoriteContract.State {
        return MyFavoriteContract.State()
    }

    override fun handleIntent(intent: MyFavoriteContract.Intent) {
        when (intent) {
            is MyFavoriteContract.Intent.LoadFavorites -> loadFavoritesFromCache()
            is MyFavoriteContract.Intent.RefreshFavorites -> loadFavoritesFromCache()
            is MyFavoriteContract.Intent.OnIdolClick -> onIdolClick(intent.idolId)
            is MyFavoriteContract.Intent.OnSettingClick -> onSettingClick()
            is MyFavoriteContract.Intent.OnPageVisible -> onPageVisible()
            is MyFavoriteContract.Intent.OnScreenVisible -> {}
            is MyFavoriteContract.Intent.OnScreenHidden -> {}
            is MyFavoriteContract.Intent.OnVoteSuccess -> onVoteSuccess(intent.idolId, intent.votedHeart)
        }
    }

    /**
     * 페이지 진입 시: 캐시 먼저 보여주고, 백그라운드에서 최신 데이터 로드
     */
    private fun onPageVisible() {
        // 1. 캐시 먼저 로드 (빠른 응답)
        loadFavoritesFromCache()

        // 2. 백그라운드에서 최신 데이터 가져오기
        refreshDataInBackground()
    }

    /**
     * 백그라운드에서 최신 데이터 갱신
     * - UserSelf API 호출하여 최애 변경 확인
     * - 즐겨찾기 목록 갱신
     * - 랭킹 캐시 갱신 (DB = Single Source of Truth)
     *
     * ✅ 안전한 이유:
     *   - updateVoteAndRefreshCache()가 DB를 먼저 업데이트한 후 캐시 재구성
     *   - UDP도 DB를 먼저 업데이트한 후 캐시 갱신
     *   - refreshChartData()는 항상 최신 DB 데이터를 기반으로 캐시 재구성
     *   - DB = Single Source of Truth 원칙 준수
     */
    private fun refreshDataInBackground() {
        viewModelScope.launch {
            try {
                // 1. UserSelf 최신 데이터 가져오기 (최애 변경 확인)
                userCacheRepository.refreshUserData()

                // 2. 즐겨찾기 목록 최신화
                userCacheRepository.refreshFavoriteIdols()

                // 3. 랭킹 데이터 갱신 (DB 기반 캐시 재구성)
                CHART_CODES.forEach { chartCode ->
                    rankingCacheRepository.refreshChartData(chartCode)
                }

                android.util.Log.d(TAG, "✅ Background refresh completed")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Background refresh failed: ${e.message}", e)
                // 에러가 나도 캐시 데이터는 이미 보여주고 있으므로 사용자에게 에러 표시 안 함
            }
        }
    }

    /**
     * 즐겨찾기 변경 감지 및 자동 섹션 업데이트
     */
    private fun observeFavoriteChanges() {
        viewModelScope.launch {
            userCacheRepository.favoriteIdolIds
                .collectLatest { favoriteIds ->
                    updateChartSections(favoriteIds.toSet())
                }
        }
    }

    /**
     * 캐시에서 즐겨찾기 목록 로드 (빠른 응답)
     */
    private fun loadFavoritesFromCache() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            try {
                val favoriteIdolIds = userCacheRepository.getFavoriteIdolIds().toSet()
                updateChartSections(favoriteIdolIds)

                setState {
                    copy(
                        isLoading = false,
                        favoriteIdols = emptyList(),
                        error = null
                    )
                }
            } catch (e: Exception) {
                setState {
                    copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
                setEffect { MyFavoriteContract.Effect.ShowError(e.message ?: "Unknown error") }
            }
        }
    }

    /**
     * 차트 섹션 업데이트
     */
    private fun updateChartSections(favoriteIdolIds: Set<Int>) {
        if (favoriteIdolIds.isEmpty()) {
            _chartSections.value = emptyList()
            return
        }

        val sections = mutableListOf<ChartSection>()

        CHART_CODES.forEach { chartCode ->
            val chartData = rankingCacheRepository.getChartData(chartCode) ?: return@forEach

            // 해당 차트에서 즐겨찾기 아이돌만 필터링
            val favoriteIdsInChart = chartData.rankItems
                .mapNotNull { it.id.toIntOrNull() }
                .filter { it in favoriteIdolIds }
                .toSet()

            if (favoriteIdsInChart.isEmpty()) return@forEach

            // 섹션 이름 결정
            val sectionName = when (chartCode) {
                "PR_S_F" -> "여자 개인"
                "PR_S_M" -> "남자 개인"
                "PR_G_F" -> "여자 그룹"
                "PR_G_M" -> "남자 그룹"
                "GLOBALS" -> context.getString(net.ib.mn.R.string.overall)
                else -> chartCode
            }

            sections.add(
                ChartSection(
                    chartCode = chartCode,
                    sectionName = sectionName,
                    favoriteIds = favoriteIdsInChart
                )
            )
        }

        _chartSections.value = sections
    }

    private fun onIdolClick(idolId: Int) {
        setEffect { MyFavoriteContract.Effect.NavigateToIdolDetail(idolId) }
    }

    private fun onSettingClick() {
        setEffect { MyFavoriteContract.Effect.NavigateToFavoriteSetting }
    }

    /**
     * 투표 성공 시 캐시 업데이트
     *
     * RankingCacheRepository.updateVoteAndRefreshCache()가 자동으로:
     * 1. DB 업데이트
     * 2. 캐시 업데이트
     * 3. UserCacheRepository.refreshMostFavoriteIdol() 호출
     *
     * mostFavoriteIdol Flow가 자동으로 UI 업데이트
     */
    private fun onVoteSuccess(idolId: Int, votedHeart: Long) {
        viewModelScope.launch {
            try {
                val chartCode = userCacheRepository.getMostIdolChartCode()

                if (chartCode != null) {
                    rankingCacheRepository.updateVoteAndRefreshCache(
                        chartCode = chartCode,
                        idolId = idolId,
                        voteCount = votedHeart
                    )
                } else {
                    // chartCode가 null인 경우 (비밀의 방 등)
                    val mostIdolId = userCacheRepository.getMostIdolId()
                    if (idolId == mostIdolId) {
                        userCacheRepository.updateMostFavoriteIdolHeart(votedHeart)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Failed to update vote cache: ${e.message}", e)
            }
        }
    }
}
