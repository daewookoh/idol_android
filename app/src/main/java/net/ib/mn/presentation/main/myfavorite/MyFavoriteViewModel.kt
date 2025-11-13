package net.ib.mn.presentation.main.myfavorite

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.repository.RankingCacheRepository
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.util.IdolImageUtil
import javax.inject.Inject

/**
 * My Favorite ViewModel (새로 작성)
 *
 * UserCacheRepository와 RankingCacheRepository를 활용하여
 * 즐겨찾기 아이돌과 최애 아이돌의 랭킹 데이터를 제공
 *
 * 주요 기능:
 * 1. UserCacheRepository에서 favoriteIdolIds와 mostIdolId를 읽어옴
 * 2. RankingCacheRepository에서 카테고리별 랭킹 데이터를 가져옴
 * 3. 카테고리별로 즐겨찾기 아이돌을 필터링하여 표시
 */
@HiltViewModel
class MyFavoriteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userCacheRepository: UserCacheRepository,
    val rankingCacheRepository: RankingCacheRepository,
    val rankingRepository: net.ib.mn.domain.repository.RankingRepository,
    private val userRepository: net.ib.mn.domain.repository.UserRepository,
    private val favoritesRepository: net.ib.mn.domain.repository.FavoritesRepository,
    private val idolDao: IdolDao
) : BaseViewModel<MyFavoriteContract.State, MyFavoriteContract.Intent, MyFavoriteContract.Effect>() {

    companion object {
        private const val TAG = "MyFavoriteVM"

        // 차트 코드 정의 (RankingCacheRepository에 저장된 순서)
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

    // 최애 아이돌 정보 (UserCacheRepository에서 실시간 구독)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val mostFavoriteIdol: StateFlow<MyFavoriteContract.MostFavoriteIdol?> =
        combine(
            userCacheRepository.mostIdolId,
            userCacheRepository.mostIdolCategory,
            userCacheRepository.mostIdolChartCode
        ) { mostIdolId, category, chartCode ->
            Triple(mostIdolId, category, chartCode)
        }.flatMapLatest { (mostIdolId, category, chartCode) ->
            flow {
                if (mostIdolId == null) {
                    android.util.Log.w(TAG, "⚠️ mostIdolId is null")
                    emit(null)
                    return@flow
                }

                // RankingCacheRepository에서 해당 차트의 데이터를 가져와서 최애 아이돌 정보 추출
                val chartData = chartCode?.let { rankingCacheRepository.getChartData(it) }
                val rankItem = chartData?.rankItems?.find { it.id == mostIdolId.toString() }

                if (rankItem != null) {
                    // 랭킹 캐시에서 찾은 경우
                    android.util.Log.d(TAG, "✅ MostFavoriteIdol from cache: id=$mostIdolId, name=${rankItem.name}, rank=${rankItem.rank}")

                    emit(MyFavoriteContract.MostFavoriteIdol(
                        idolId = mostIdolId,
                        name = rankItem.name,
                        top3ImageUrls = rankItem.top3ImageUrls,
                        top3VideoUrls = rankItem.top3VideoUrls,
                        rank = rankItem.rank,
                        heart = rankItem.heartCount,
                        chartCode = chartCode,
                        imageUrl = rankItem.photoUrl
                    ))
                } else {
                    // 랭킹 캐시에 없으면 로컬 DB에서 가져오기
                    android.util.Log.d(TAG, "⚠️ Rank item not found in cache, fetching from DB: id=$mostIdolId")
                    val idolEntity = idolDao.getIdolById(mostIdolId)

                    if (idolEntity != null) {
                        android.util.Log.d(TAG, "✅ MostFavoriteIdol from DB: id=$mostIdolId, name=${idolEntity.name}")

                        emit(MyFavoriteContract.MostFavoriteIdol(
                            idolId = mostIdolId,
                            name = idolEntity.name,
                            top3ImageUrls = IdolImageUtil.getTop3ImageUrls(idolEntity),
                            top3VideoUrls = IdolImageUtil.getTop3VideoUrls(idolEntity),
                            rank = null,
                            heart = idolEntity.heart,
                            chartCode = chartCode,
                            imageUrl = idolEntity.imageUrl
                        ))
                    } else {
                        android.util.Log.e(TAG, "❌ MostFavoriteIdol not found: id=$mostIdolId")
                        emit(null)
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    override fun createInitialState(): MyFavoriteContract.State {
        return MyFavoriteContract.State()
    }

    override fun handleIntent(intent: MyFavoriteContract.Intent) {
        when (intent) {
            is MyFavoriteContract.Intent.LoadFavorites -> loadFavorites()
            is MyFavoriteContract.Intent.RefreshFavorites -> loadFavorites()
            is MyFavoriteContract.Intent.OnIdolClick -> onIdolClick(intent.idolId)
            is MyFavoriteContract.Intent.OnSettingClick -> onSettingClick()
            is MyFavoriteContract.Intent.OnPageVisible -> loadFavorites()
            is MyFavoriteContract.Intent.OnScreenVisible -> {}
            is MyFavoriteContract.Intent.OnScreenHidden -> {}
            is MyFavoriteContract.Intent.OnVoteSuccess -> {}
        }
    }

    /**
     * 즐겨찾기 목록 로드
     *
     * 로직:
     * 1. getUserSelf와 getFavoriteSelf API를 병렬로 호출하여 최신 데이터 갱신
     * 2. UserCacheRepository에서 favoriteIdolIds 가져오기
     * 3. RankingCacheRepository에서 각 차트별 데이터 가져오기
     * 4. 각 차트에서 즐겨찾기 아이돌만 필터링
     * 5. 즐겨찾기 아이돌이 있는 차트만 섹션으로 추가
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            android.util.Log.d(TAG, "========================================")
            android.util.Log.d(TAG, "📋 Loading favorites with API refresh...")
            android.util.Log.d(TAG, "========================================")

            setState { copy(isLoading = true, error = null) }

            try {
                // Step 1: getUserSelf와 getFavoriteSelf를 병렬로 호출
                android.util.Log.d(TAG, "🔄 Fetching latest data from API (parallel)...")

                async {
                    android.util.Log.d(TAG, "  📡 Calling getUserSelf API...")
                    userRepository.loadAndSaveUserSelf()
                }

                async {
                    android.util.Log.d(TAG, "  📡 Calling getFavoritesSelf API...")
                    favoritesRepository.loadAndSaveFavoriteSelf()
                }

                // Step 2: UserCacheRepository에서 favoriteIdolIds 가져오기
                val favoriteIdolIds = userCacheRepository.getFavoriteIdolIds()
                android.util.Log.d(TAG, "✅ Favorite idol IDs: ${favoriteIdolIds.size} idols")
                android.util.Log.d(TAG, "   IDs: $favoriteIdolIds")

                if (favoriteIdolIds.isEmpty()) {
                    android.util.Log.w(TAG, "⚠️ No favorite idols found")
                    setState {
                        copy(
                            isLoading = false,
                            favoriteIdols = emptyList(),
                            error = null
                        )
                    }
                    _chartSections.value = emptyList()
                    return@launch
                }

                // Step 3: 각 차트별로 데이터 가져오기 및 필터링
                val sections = mutableListOf<ChartSection>()

                CHART_CODES.forEach { chartCode ->
                    val chartData = rankingCacheRepository.getChartData(chartCode)

                    if (chartData == null) {
                        android.util.Log.w(TAG, "⚠️ $chartCode: No cache data")
                        return@forEach
                    }

                    // 해당 차트에서 즐겨찾기 아이돌만 필터링
                    val favoriteIdsInChart = chartData.rankItems
                        .filter { item ->
                            val idolId = item.id.toIntOrNull()
                            idolId != null && favoriteIdolIds.contains(idolId)
                        }
                        .mapNotNull { it.id.toIntOrNull() }
                        .toSet()

                    if (favoriteIdsInChart.isEmpty()) {
                        android.util.Log.d(TAG, "  $chartCode: No favorites (skipping)")
                        return@forEach
                    }

                    // 섹션 이름 결정
                    val sectionName = when (chartCode) {
                        "PR_S_F" -> "여자 개인"
                        "PR_S_M" -> "남자 개인"
                        "PR_G_F" -> "여자 그룹"
                        "PR_G_M" -> "남자 그룹"
                        "GLOBALS" -> context.getString(net.ib.mn.R.string.overall)
                        else -> chartCode
                    }

                    android.util.Log.d(TAG, "  ✅ $chartCode ($sectionName): ${favoriteIdsInChart.size} favorites")

                    sections.add(
                        ChartSection(
                            chartCode = chartCode,
                            sectionName = sectionName,
                            favoriteIds = favoriteIdsInChart
                        )
                    )
                }

                _chartSections.value = sections

                android.util.Log.d(TAG, "========================================")
                android.util.Log.d(TAG, "✅ Loaded ${sections.size} chart sections")
                android.util.Log.d(TAG, "========================================")

                setState {
                    copy(
                        isLoading = false,
                        favoriteIdols = emptyList(), // UI에서 chartSections 사용
                        error = null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error loading favorites: ${e.message}", e)
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

    private fun onIdolClick(idolId: Int) {
        setEffect { MyFavoriteContract.Effect.NavigateToIdolDetail(idolId) }
    }

    private fun onSettingClick() {
        setEffect { MyFavoriteContract.Effect.NavigateToFavoriteSetting }
    }
}
