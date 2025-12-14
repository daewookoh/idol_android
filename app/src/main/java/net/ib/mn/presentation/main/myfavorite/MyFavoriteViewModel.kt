package net.ib.mn.presentation.main.myfavorite

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.domain.repository.ConfigRepository
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
    private val userCacheRepository: UserCacheRepository,
    private val chartDatabaseRepository: net.ib.mn.data.repository.ChartRankingRepository,
    val rankingRepository: net.ib.mn.domain.repository.RankingRepository,
    private val configRepository: ConfigRepository
) : BaseViewModel<MyFavoriteContract.State, MyFavoriteContract.Intent, MyFavoriteContract.Effect>() {

    companion object {
        private val CHART_CODES = listOf("PR_S_M", "PR_S_F", "PR_G_M", "PR_G_F", "GLOBALS")
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
        observeFavoriteChanges()
        loadMostIdolChart()
        observeMostPicksModel()
    }

    private fun loadMostIdolChart() {
        viewModelScope.launch {
            runCatching {
                userCacheRepository.getMostIdolChartCode()?.let {
                    chartDatabaseRepository.refreshChart(it)
                }
            }
        }
    }

    private fun observeMostPicksModel() {
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

    private fun onPageVisible() {
        loadFavoritesFromCache()
        refreshDataInBackground()
    }

    private fun refreshDataInBackground() {
        viewModelScope.launch {
            runCatching {
                userCacheRepository.refreshUserData()
                userCacheRepository.refreshFavoriteIdols()

                val mostIdolChartCode = userCacheRepository.getMostIdolChartCode()
                if (mostIdolChartCode in CHART_CODES) {
                    chartDatabaseRepository.refreshChart(mostIdolChartCode!!)
                }

                CHART_CODES.filter { it != mostIdolChartCode }.forEach { chartCode ->
                    chartDatabaseRepository.refreshChart(chartCode)
                }
            }
        }
    }

    private fun observeFavoriteChanges() {
        viewModelScope.launch {
            userCacheRepository.favoriteIdolIds.collectLatest { favoriteIds ->
                updateChartSections(favoriteIds.toSet())
            }
        }
    }

    private fun loadFavoritesFromCache() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            runCatching {
                val favoriteIdolIds = userCacheRepository.getFavoriteIdolIds().toSet()
                updateChartSections(favoriteIdolIds)
                setState { copy(isLoading = false, favoriteIdols = emptyList(), error = null) }
            }.onFailure { e ->
                setState { copy(isLoading = false, error = e.message ?: "Unknown error") }
                setEffect { MyFavoriteContract.Effect.ShowError(e.message ?: "Unknown error") }
            }
        }
    }

    private suspend fun updateChartSections(favoriteIdolIds: Set<Int>) {
        if (favoriteIdolIds.isEmpty()) {
            _chartSections.value = emptyList()
            return
        }

        val codeToFullNameMap = buildCodeToFullNameMap()

        _chartSections.value = CHART_CODES.mapNotNull { chartCode ->
            val chartData = chartDatabaseRepository.getChartData(chartCode) ?: return@mapNotNull null
            val favoriteIdsInChart = chartData.rankItems
                .mapNotNull { it.id.toIntOrNull() }
                .filter { it in favoriteIdolIds }
                .toSet()

            if (favoriteIdsInChart.isEmpty()) return@mapNotNull null

            ChartSection(
                chartCode = chartCode,
                sectionName = codeToFullNameMap[chartCode] ?: chartCode,
                favoriteIds = favoriteIdsInChart
            )
        }
    }

    private fun buildCodeToFullNameMap(): Map<String, String> {
        val mainChartModel = configRepository.getMainChartModel() ?: return emptyMap()
        val allCharts = (mainChartModel.males.orEmpty() + mainChartModel.females.orEmpty())
        return allCharts.mapNotNull { info ->
            info.code?.let { code -> info.fullName?.let { fullName -> code to fullName } }
        }.toMap()
    }

    private fun onSettingClick() {
        setEffect { MyFavoriteContract.Effect.NavigateToFavoriteSetting }
    }

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

    private fun onVoteSuccess(idolId: Int, votedHeart: Long) {
        viewModelScope.launch {
            runCatching {
                chartDatabaseRepository.updateVoteAndRerank(
                    idolId = idolId,
                    votedHeartCount = votedHeart,
                    chartCode = userCacheRepository.getMostIdolChartCode()
                )
            }
        }
    }
}
