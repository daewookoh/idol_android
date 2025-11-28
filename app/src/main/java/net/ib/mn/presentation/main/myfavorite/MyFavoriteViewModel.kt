package net.ib.mn.presentation.main.myfavorite

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.repository.UserCacheRepository
import javax.inject.Inject

/**
 * My Favorite ViewModel (최적화 버전)
 *
 * Flow 기반 반응형 아키텍처:
 * - UserCacheRepository.mostFavoriteIdol Flow 구독으로 실시간 업데이트
 * - ChartRankingRepository.observeChartData로 차트별 데이터 자동 반영 (Room DB Flow)
 * - 불필요한 API 호출 제거, DB 기반 동작
 */
@HiltViewModel
class MyFavoriteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userCacheRepository: UserCacheRepository,
    private val chartDatabaseRepository: net.ib.mn.data.repository.ChartRankingRepository,
    val rankingRepository: net.ib.mn.domain.repository.RankingRepository
) : BaseViewModel<MyFavoriteContract.State, MyFavoriteContract.Intent, MyFavoriteContract.Effect>() {

    companion object {
        private const val TAG = "MyFavoriteVM"

        // 차트 코드 정의
        private val CHART_CODES = listOf(
            "PR_S_M",  // 남자 개인
            "PR_S_F",  // 여자 개인
            "PR_G_M",  // 남자 그룹
            "PR_G_F",  // 여자 그룹
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

    // 최애 아이돌 정보 (ChartRankingRepository Flow 직접 구독하여 MostFavoriteIdol로 변환)
    val mostFavoriteIdol: StateFlow<MyFavoriteContract.MostFavoriteIdol?> =
        combine(
            chartDatabaseRepository.mostFavoriteIdolRankingItem,
            userCacheRepository.mostIdolChartCode
        ) { rankingItem: net.ib.mn.ui.components.RankingItem?, chartCode: String? ->
            rankingItem?.let {
                MyFavoriteContract.MostFavoriteIdol(
                    idolId = it.id.toIntOrNull() ?: 0,
                    name = it.name,
                    top3ImageUrls = it.top3ImageUrls,
                    top3VideoUrls = it.top3VideoUrls,
                    rank = it.rank,
                    heart = it.heartCount,
                    chartCode = chartCode,
                    imageUrl = it.photoUrl,
                    fandomName = it.fandomName
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // 즐겨찾기 ID 변경 감지하여 자동으로 섹션 업데이트
        observeFavoriteChanges()

        // ✅ 앱 시작 시 mostIdol 차트를 즉시 로드하여 빠른 응답
        viewModelScope.launch {
            try {
                val mostIdolChartCode = userCacheRepository.getMostIdolChartCode()
                if (mostIdolChartCode != null) {
                    android.util.Log.d(TAG, "🚀 Init: Loading mostIdol chart: $mostIdolChartCode")
                    chartDatabaseRepository.refreshChart(mostIdolChartCode)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Failed to load mostIdol chart in init: ${e.message}", e)
            }
        }

        // mostPicksModel Flow를 State에 연결
        viewModelScope.launch {
            userCacheRepository.mostPicksModel.collect { model ->
                setState { copy(mostPicksModel = model) }
            }
        }
    }

    override fun createInitialState(): MyFavoriteContract.State {
        return MyFavoriteContract.State()
    }

    override fun handleIntent(intent: MyFavoriteContract.Intent) {
        when (intent) {
            is MyFavoriteContract.Intent.LoadFavorites -> loadFavoritesFromCache()
            is MyFavoriteContract.Intent.RefreshFavorites -> loadFavoritesFromCache()
            is MyFavoriteContract.Intent.OnSettingClick -> onSettingClick()
            is MyFavoriteContract.Intent.OnPageVisible -> onPageVisible()
            is MyFavoriteContract.Intent.OnScreenVisible -> {}
            is MyFavoriteContract.Intent.OnScreenHidden -> {}
            is MyFavoriteContract.Intent.OnVoteSuccess -> onVoteSuccess(intent.idolId, intent.votedHeart)
            is MyFavoriteContract.Intent.OnSupportBiasBarClick -> onSupportBiasBarClick(intent.id, intent.kind)
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
     * - 랭킹 DB 갱신 (Room DB = Single Source of Truth)
     *
     * ✅ 안전한 이유:
     *   - updateVoteAndRerank()가 DB를 먼저 업데이트
     *   - UDP도 DB를 먼저 업데이트
     *   - refreshChart()는 항상 최신 API 데이터를 기반으로 DB 재구성
     *   - Room DB = Single Source of Truth 원칙 준수
     */
    private fun refreshDataInBackground() {
        viewModelScope.launch {
            try {
                // 1. UserSelf 최신 데이터 가져오기 (최애 변경 확인)
                userCacheRepository.refreshUserData()

                // 2. 즐겨찾기 목록 최신화
                userCacheRepository.refreshFavoriteIdols()

                // 3. 랭킹 데이터 갱신 (Room DB 업데이트)
                // ✅ mostIdol의 차트를 먼저 갱신하여 빠른 응답
                val mostIdolChartCode = userCacheRepository.getMostIdolChartCode()
                if (mostIdolChartCode != null && mostIdolChartCode in CHART_CODES) {
                    android.util.Log.d(TAG, "🔄 Refreshing mostIdol chart first: $mostIdolChartCode")
                    chartDatabaseRepository.refreshChart(mostIdolChartCode)
                }

                // 나머지 차트 갱신
                CHART_CODES.forEach { chartCode ->
                    if (chartCode != mostIdolChartCode) {
                        chartDatabaseRepository.refreshChart(chartCode)
                    }
                }

                android.util.Log.d(TAG, "✅ Background refresh completed")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Background refresh failed: ${e.message}", e)
                // 에러가 나도 DB 데이터는 이미 보여주고 있으므로 사용자에게 에러 표시 안 함
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
    private suspend fun updateChartSections(favoriteIdolIds: Set<Int>) {
        if (favoriteIdolIds.isEmpty()) {
            _chartSections.value = emptyList()
            return
        }

        val sections = mutableListOf<ChartSection>()

        CHART_CODES.forEach { chartCode ->
            val chartData = chartDatabaseRepository.getChartData(chartCode) ?: return@forEach

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

    private fun onSettingClick() {
        setEffect { MyFavoriteContract.Effect.NavigateToFavoriteSetting }
    }

    /**
     * Support Bias Bar 클릭 시 웹페이지로 이동
     */
    private fun onSupportBiasBarClick(id: Int, kind: String) {
        val locale = java.util.Locale.getDefault().language
        val url = when (kind) {
            "themepick" -> "https://starpass.app/themepick/$id?locale=$locale"
            "heartpick" -> "https://starpass.app/heartpick/$id?locale=$locale"
            "miracle" -> "https://starpass.app/miracle?locale=$locale"
            "onepick" -> "https://starpass.app/onepick/$id?locale=$locale"
            else -> return
        }
        setEffect { MyFavoriteContract.Effect.NavigateToWebPage(url) }
    }

    /**
     * 투표 성공 시 차트 재정렬
     *
     * ChartRankingRepository.updateVoteAndRerank()가 자동으로:
     * 1. SharedPreference의 차트 랭킹 업데이트
     * 2. mostFavoriteIdolRankingItem Flow 업데이트
     *
     * mostFavoriteIdolRankingItem Flow가 자동으로 UI 업데이트
     */
    private fun onVoteSuccess(idolId: Int, votedHeart: Long) {
        viewModelScope.launch {
            try {
                val chartCode = userCacheRepository.getMostIdolChartCode()

                chartDatabaseRepository.updateVoteAndRerank(
                    idolId = idolId,
                    votedHeartCount = votedHeart,
                    chartCode = chartCode
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Failed to update vote in DB: ${e.message}", e)
            }
        }
    }
}
