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
import net.ib.mn.data.remote.dto.AggregateRankModel
import net.ib.mn.data.remote.dto.toEntity
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
    private val configRepository: ConfigRepository,
    private val idolDao: net.ib.mn.data.local.dao.IdolDao,
    private val broadcastManager: net.ib.mn.data.remote.udp.IdolBroadcastManager,
    private val rankingCacheRepository: net.ib.mn.data.repository.RankingCacheRepository
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

    // MostFavoriteIdol 실시간 업데이트를 위한 Flow
    private val _mostFavoriteIdolFlow = MutableStateFlow<MyFavoriteContract.MostFavoriteIdol?>(null)
    val mostFavoriteIdolFlow: StateFlow<MyFavoriteContract.MostFavoriteIdol?> = _mostFavoriteIdolFlow.asStateFlow()

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
            is MyFavoriteContract.Intent.OnVoteSuccess -> onVoteSuccess(intent.idolId, intent.votedHeart)
        }
    }

    init {
        // 초기 데이터는 onPageVisible에서 로드

        // UDP updateEvent 구독하여 MostFavoriteIdol 실시간 업데이트
        viewModelScope.launch {
            broadcastManager.updateEvent.collect { changedIds ->
                android.util.Log.d(logTag, "🔄 UDP update event received - ${changedIds.size} idols changed")

                val mostIdolId = preferencesManager.mostIdolId.firstOrNull()

                // 변경된 아이돌 중 mostIdolId가 있으면 업데이트
                if (mostIdolId != null && changedIds.contains(mostIdolId)) {
                    android.util.Log.d(logTag, "📊 MostIdol changed, updating...")
                    updateMostFavoriteIdolFromDb(mostIdolId)
                }
            }
        }

        // chartSections가 변경되면 해당 차트의 랭킹에서 순위 업데이트
        viewModelScope.launch {
            _chartSections.collect { sections ->
                val mostIdolId = preferencesManager.mostIdolId.firstOrNull()
                val mostChartCode = preferencesManager.mostIdolChartCode.firstOrNull()

                if (mostIdolId != null && mostChartCode != null && sections.isNotEmpty()) {
                    // 차트 로딩이 완료되면 순위 업데이트
                    android.util.Log.d(logTag, "📊 Chart sections loaded, updating rank for mostIdol")
                    updateMostFavoriteIdolFromDb(mostIdolId)
                }
            }
        }
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
                            // chartCodes에서 Award/DF 코드 제외
                            val chartCode = most.chartCodes
                                ?.firstOrNull { !it.startsWith("AW_") && !it.startsWith("DF_") }
                                ?: most.chartCodes?.firstOrNull()

                            preferencesManager.setMostIdol(
                                idolId = most.id,
                                chartCode = chartCode,
                                category = most.category
                            )
                            android.util.Log.d(logTag, "💾 Updated most idol: id=${most.id}, chartCode=$chartCode, category=${most.category}")

                            // Most 아이돌 데이터를 로컬 DB에 upsert
                            val idolEntity = most.toEntity()
                            idolDao.upsert(idolEntity)
                            android.util.Log.d(logTag, "💾 Most idol upserted to local DB: id=${most.id}, name=${most.name}")
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
                            // GLOBALS는 기존 다국어 문자열 사용 (overall = "Overall" / "종합")
                            put("GLOBALS", context.getString(net.ib.mn.R.string.overall))
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

                        // MostFavoriteIdol 생성 - mostIdolId와 mostChartCode 기반
                        val mostFavoriteIdol = createMostFavoriteIdol(
                            mostIdolId = mostIdolId,
                            chartIdolIdsMap = chartIdolIdsMap
                        )

                        setState {
                            copy(
                                isLoading = false,
                                favoriteIdols = emptyList(), // 더 이상 사용 안 함
                                mostFavoriteIdol = mostFavoriteIdol,
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

    /**
     * MostFavoriteIdol 생성 (초기 로딩용)
     *
     * localDB에서 이름과 투표수만 가져오고 순위는 null로 설정
     * (순위는 UDP 업데이트 시 계산됨)
     *
     * @param mostIdolId SharedPreference의 mostIdolId
     * @param chartIdolIdsMap 차트별 idol IDs 맵 (사용하지 않음, 호환성 유지)
     * @return MostFavoriteIdol 또는 null
     */
    private suspend fun createMostFavoriteIdol(
        mostIdolId: Int?,
        chartIdolIdsMap: Map<String, List<Int>>
    ): MyFavoriteContract.MostFavoriteIdol? {
        if (mostIdolId == null) {
            android.util.Log.w(logTag, "⚠️ mostIdolId is null, cannot create MostFavoriteIdol")
            return null
        }

        // mostChartCode 가져오기
        val mostChartCode = preferencesManager.mostIdolChartCode.firstOrNull()

        android.util.Log.d(logTag, "🎯 Creating MostFavoriteIdol (initial): idolId=$mostIdolId, chartCode=$mostChartCode")

        // localDB에서 아이돌 기본 정보만 가져오기
        val idolEntity = idolDao.getIdolById(mostIdolId)
        if (idolEntity == null) {
            android.util.Log.e(logTag, "❌ mostIdolId=$mostIdolId not found in localDB")
            return null
        }

        android.util.Log.d(logTag, "✅ MostFavoriteIdol created (initial): name=${idolEntity.name}, heart=${idolEntity.heart}, rank=null")

        return MyFavoriteContract.MostFavoriteIdol(
            idolId = mostIdolId,
            name = idolEntity.name,
            top3ImageUrls = listOf(idolEntity.imageUrl, idolEntity.imageUrl2, idolEntity.imageUrl3),
            top3VideoUrls = emptyList(),
            rank = null,  // 초기 로딩 시에는 순위 계산 안 함
            heart = idolEntity.heart,
            chartCode = mostChartCode,
            imageUrl = idolEntity.imageUrl
        )
    }

    /**
     * MostFavoriteIdol을 localDB에서 업데이트
     *
     * UDP로 변경 이벤트를 받으면 localDB에서 최신 정보를 가져와 업데이트
     * (UnifiedRankingSubPageViewModel의 queryIdolsByIdsFromDb와 동일한 방식)
     *
     * @param mostIdolId 최애 아이돌 ID
     */
    private suspend fun updateMostFavoriteIdolFromDb(mostIdolId: Int) {
        android.util.Log.d(logTag, "🔄 Updating MostFavoriteIdol from DB: idolId=$mostIdolId")

        // localDB에서 아이돌 정보 가져오기 (UDP로 이미 업데이트된 최신 데이터)
        val idolEntity = idolDao.getIdolById(mostIdolId)
        if (idolEntity == null) {
            android.util.Log.e(logTag, "❌ mostIdolId=$mostIdolId not found in localDB")
            return
        }

        // SECRET_ROOM_IDOL_ID는 순위 없이 투표 수만 업데이트
        if (mostIdolId == net.ib.mn.util.Constants.SECRET_ROOM_IDOL_ID) {
            android.util.Log.d(logTag, "🔒 SECRET_ROOM: updating vote count only (no rank)")
            val updatedIdol = MyFavoriteContract.MostFavoriteIdol(
                idolId = mostIdolId,
                name = idolEntity.name,
                top3ImageUrls = listOf(idolEntity.imageUrl, idolEntity.imageUrl2, idolEntity.imageUrl3),
                top3VideoUrls = emptyList(),
                rank = null,  // 비밀의방은 순위 없음
                heart = idolEntity.heart,
                chartCode = null,  // 비밀의방은 chartCode 없음
                imageUrl = idolEntity.imageUrl
            )
            setState { copy(mostFavoriteIdol = updatedIdol) }
            android.util.Log.d(logTag, "✅ SECRET_ROOM updated: heart=${idolEntity.heart}")
            return
        }

        val mostChartCode = preferencesManager.mostIdolChartCode.firstOrNull()
        if (mostChartCode == null) {
            android.util.Log.w(logTag, "⚠️ mostChartCode is null")
            return
        }

        // 랭킹 계산을 위해 해당 차트의 모든 아이돌 가져오기
        val allIdolsInChart = when (mostChartCode) {
            "GLOBALS" -> {
                // GLOBALS는 category로 필터링
                val category = preferencesManager.mostIdolCategory.firstOrNull()
                if (category != null) {
                    idolDao.getByCategory(category)
                } else {
                    idolDao.getViewableIdols()
                }
            }
            else -> {
                // 특정 차트: type과 category로 필터링
                val type = if (mostChartCode.contains("_S_")) "S" else "G"
                val category = if (mostChartCode.contains("_M")) "M" else "F"
                idolDao.getIdolByTypeAndCategory(type, category)
            }
        }

        // heart 기준으로 정렬하여 순위 계산
        val sortedIdols = allIdolsInChart.sortedByDescending { it.heart }
        val rank = sortedIdols.indexOfFirst { it.id == mostIdolId } + 1

        android.util.Log.d(logTag, "✅ MostFavoriteIdol updated from DB: rank=$rank, heart=${idolEntity.heart}")

        val updatedIdol = MyFavoriteContract.MostFavoriteIdol(
            idolId = mostIdolId,
            name = idolEntity.name,
            top3ImageUrls = listOf(idolEntity.imageUrl, idolEntity.imageUrl2, idolEntity.imageUrl3),
            top3VideoUrls = emptyList(),
            rank = if (rank > 0) rank else null,
            heart = idolEntity.heart,
            chartCode = mostChartCode,
            imageUrl = idolEntity.imageUrl
        )

        // State 업데이트
        setState { copy(mostFavoriteIdol = updatedIdol) }
    }

    private fun onIdolClick(idolId: Int) {
        setEffect { MyFavoriteContract.Effect.NavigateToIdolDetail(idolId) }
    }

    private fun onSettingClick() {
        setEffect { MyFavoriteContract.Effect.NavigateToFavoriteSetting }
    }

    /**
     * 투표 성공 시 즉시 데이터 업데이트
     *
     * localDB 업데이트 후 MostFavoriteIdol 즉시 갱신
     */
    private fun onVoteSuccess(idolId: Int, votedHeart: Long) {
        viewModelScope.launch {
            android.util.Log.d(logTag, "💗 Vote success for idol $idolId: +$votedHeart hearts")

            // localDB의 투표 수 업데이트
            try {
                val idol = idolDao.getIdolById(idolId)
                if (idol != null) {
                    val newHeart = idol.heart + votedHeart
                    idolDao.updateIdolHeart(idolId, newHeart)
                    android.util.Log.d(logTag, "✅ DB updated: idol=$idolId, newHeart=$newHeart")
                } else {
                    android.util.Log.w(logTag, "⚠️ Idol not found in DB: idol=$idolId")
                }
            } catch (e: Exception) {
                android.util.Log.e(logTag, "❌ Failed to update DB: ${e.message}", e)
            }

            // MostFavoriteIdol이 투표한 아이돌인 경우 즉시 갱신
            val mostIdolId = preferencesManager.mostIdolId.firstOrNull()
            if (mostIdolId == idolId) {
                updateMostFavoriteIdolFromDb(idolId)
                android.util.Log.d(logTag, "✅ MostFavoriteIdol updated immediately after vote")
            }
        }
    }
}
