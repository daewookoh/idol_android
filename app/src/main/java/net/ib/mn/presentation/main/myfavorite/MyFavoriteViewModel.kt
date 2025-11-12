package net.ib.mn.presentation.main.myfavorite

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.FavoritesRepository
import net.ib.mn.domain.repository.RankingRepository
import net.ib.mn.domain.repository.UserRepository
import net.ib.mn.domain.repository.ConfigRepository
import javax.inject.Inject

/**
 * My Favorite ViewModel (간소화)
 *
 * UnifiedRankingSubPage를 재사용하는 방식으로 변경
 * 5개 차트 코드별로 즐겨찾기 필터링하여 표시
 */
@HiltViewModel
class MyFavoriteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val favoritesRepository: FavoritesRepository,
    val rankingRepository: RankingRepository,
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository,
    private val configRepository: ConfigRepository
) : BaseViewModel<MyFavoriteContract.State, MyFavoriteContract.Intent, MyFavoriteContract.Effect>() {

    private val logTag = "MyFavoriteVM"

    // 5개 차트 코드와 섹션 이름
    data class ChartSection(
        val chartCode: String,
        val sectionName: String,
        val favoriteIds: Set<Int> = emptySet()
    )

    private val _chartSections = MutableStateFlow<List<ChartSection>>(emptyList())
    val chartSections: StateFlow<List<ChartSection>> = _chartSections.asStateFlow()

    private val _topFavorite = MutableStateFlow<MyFavoriteContract.TopFavorite?>(null)
    val topFavorite: StateFlow<MyFavoriteContract.TopFavorite?> = _topFavorite.asStateFlow()

    override fun createInitialState(): MyFavoriteContract.State {
        return MyFavoriteContract.State()
    }

    override fun handleIntent(intent: MyFavoriteContract.Intent) {
        when (intent) {
            is MyFavoriteContract.Intent.LoadFavorites -> loadFavorites()
            is MyFavoriteContract.Intent.RefreshFavorites -> loadFavorites()
            is MyFavoriteContract.Intent.OnIdolClick -> onIdolClick(intent.idolId)
            is MyFavoriteContract.Intent.OnSettingClick -> onSettingClick()
            is MyFavoriteContract.Intent.OnPageVisible -> onPageVisible()
            is MyFavoriteContract.Intent.OnScreenVisible -> {} // UnifiedRankingSubPage에서 처리
            is MyFavoriteContract.Intent.OnScreenHidden -> {} // UnifiedRankingSubPage에서 처리
        }
    }

    init {
        // 초기 데이터는 onPageVisible에서 로드
    }

    /**
     * 페이지가 visible 될 때 호출
     * getUserSelf를 호출해서 most idol ID를 갱신하고, favorites 목록 로드
     */
    private fun onPageVisible() {
        android.util.Log.d(logTag, "📱 Page visible - refreshing user data")

        viewModelScope.launch {
            // getUserSelf 호출하여 most idol ID 갱신
            userRepository.getUserSelf().collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        result.data.objects.firstOrNull()?.most?.let { most ->
                            preferencesManager.setMostIdol(most.id, most.type, most.groupId)
                            android.util.Log.d(logTag, "💾 Updated most idol: id=${most.id}")
                        }
                        loadFavorites()
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e(logTag, "❌ getUserSelf error: ${result.message}")
                        loadFavorites() // 에러 발생해도 캐시된 데이터 표시
                    }
                    is ApiResult.Loading -> {}
                }
            }
        }
    }

    /**
     * 최애 목록 로드 (간소화 버전 - UnifiedRankingSubPage 재사용)
     *
     * 로직:
     * 1. 5개 차트 코드별로 charts/idol_ids API 호출
     * 2. Favorites API로 즐겨찾기 목록 가져오기
     * 3. 각 차트에 내 즐겨찾기 아이돌이 있는지 확인
     * 4. 노출할 차트 코드와 favoriteIds를 ChartSection으로 저장
     * 5. UI에서 UnifiedRankingSubPage에 favoriteIds 전달하여 필터링
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            try {
                val mostIdolId = preferencesManager.mostIdolId.firstOrNull()
                android.util.Log.d(logTag, "🎯 Most Idol ID: $mostIdolId")

                // Step 1: 5개 차트 코드별로 idol_ids 조회
                val chartCodes = listOf("PR_S_F", "PR_S_M", "PR_G_F", "PR_G_M", "GLOBALS")
                val chartIdolIdsMap = mutableMapOf<String, List<Int>>()

                android.util.Log.d(logTag, "📊 Fetching idol IDs for ${chartCodes.size} charts")

                chartCodes.forEach { chartCode ->
                    var chartResult: ApiResult<List<Int>>? = null
                    rankingRepository.getChartIdolIds(chartCode).collect { result ->
                        chartResult = result
                        if (result is ApiResult.Success || result is ApiResult.Error) {
                            return@collect
                        }
                    }

                    when (chartResult) {
                        is ApiResult.Success -> {
                            val ids = (chartResult as ApiResult.Success).data
                            chartIdolIdsMap[chartCode] = ids
                            android.util.Log.d(logTag, "  ✅ $chartCode: ${ids.size} idols")
                        }
                        is ApiResult.Error -> {
                            android.util.Log.e(logTag, "  ❌ $chartCode: ${(chartResult as ApiResult.Error).message}")
                            chartIdolIdsMap[chartCode] = emptyList()
                        }
                        else -> {
                            chartIdolIdsMap[chartCode] = emptyList()
                        }
                    }
                }

                // Step 2: Favorites API 호출
                var favoriteResult: ApiResult<List<net.ib.mn.data.remote.dto.FavoriteDto>>? = null
                favoritesRepository.getFavoritesSelf().collect { result ->
                    favoriteResult = result
                    if (result is ApiResult.Success || result is ApiResult.Error) {
                        return@collect
                    }
                }

                when (favoriteResult) {
                    is ApiResult.Success -> {
                        val favoriteDtos = (favoriteResult as ApiResult.Success).data
                        android.util.Log.d(logTag, "✅ Loaded ${favoriteDtos.size} favorites")

                        // 즐겨찾기 아이돌 ID Set 생성
                        val favoriteIdolIds = favoriteDtos.map { it.idol.id }.toSet()

                        // ChartCodeInfo 맵 생성 (섹션 이름 표시용)
                        val mainChartModel = configRepository.getMainChartModel()
                        val chartCodeInfoMap = buildMap<String, String> {
                            mainChartModel?.males?.forEach { info ->
                                info.code?.let { put(it, info.fullName ?: info.name ?: it) }
                            }
                            mainChartModel?.females?.forEach { info ->
                                info.code?.let { put(it, info.fullName ?: info.name ?: it) }
                            }
                            // GLOBALS는 직접 추가
                            put("GLOBALS", "글로벌")
                        }

                        // Step 3 & 4: 각 차트에 내 즐겨찾기 아이돌이 있는지 확인하여 ChartSection 생성
                        val sections = chartCodes.mapNotNull { chartCode ->
                            val idolIdsInChart = chartIdolIdsMap[chartCode] ?: emptyList()
                            val myFavoriteIdsInChart = idolIdsInChart.filter { id ->
                                favoriteIdolIds.contains(id)
                            }.toSet()

                            if (myFavoriteIdsInChart.isEmpty()) {
                                null
                            } else {
                                val sectionName = chartCodeInfoMap[chartCode] ?: chartCode
                                android.util.Log.d(logTag, "  📋 $chartCode ($sectionName): ${myFavoriteIdsInChart.size} favorites")

                                ChartSection(
                                    chartCode = chartCode,
                                    sectionName = sectionName,
                                    favoriteIds = myFavoriteIdsInChart
                                )
                            }
                        }

                        _chartSections.value = sections
                        android.util.Log.d(logTag, "✅ Visible chart sections: ${sections.size}")

                        // Most Idol TopFavorite 생성
                        val topFavoriteData = mostIdolId?.let { id ->
                            favoriteDtos.find { it.idol.id == id }
                        }?.let { dto ->
                            MyFavoriteContract.TopFavorite(
                                idolId = dto.idol.id,
                                name = dto.idol.name ?: "Unknown",
                                top3ImageUrls = listOf(dto.idol.imageUrl, dto.idol.imageUrl2, dto.idol.imageUrl3),
                                top3VideoUrls = emptyList(),
                                rank = null, // UnifiedRankingSubPage에서 계산
                                heart = dto.idol.heart
                            )
                        }

                        _topFavorite.value = topFavoriteData

                        setState {
                            copy(
                                isLoading = false,
                                favoriteIdols = emptyList(), // 더 이상 사용 안 함
                                topFavorite = topFavoriteData,
                                error = null
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        val errorMsg = (favoriteResult as ApiResult.Error).message ?: "Unknown error"
                        android.util.Log.e(logTag, "❌ Error: $errorMsg")
                        setState { copy(isLoading = false, error = errorMsg) }
                        setEffect { MyFavoriteContract.Effect.ShowError(errorMsg) }
                    }
                    else -> {
                        android.util.Log.e(logTag, "❌ Unexpected result state")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(logTag, "❌ Exception in loadFavorites", e)
                setState { copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    private fun onIdolClick(idolId: Int) {
        setEffect { MyFavoriteContract.Effect.NavigateToIdolDetail(idolId) }
    }

    private fun onSettingClick() {
        setEffect { MyFavoriteContract.Effect.NavigateToFavoriteSetting }
    }
}
