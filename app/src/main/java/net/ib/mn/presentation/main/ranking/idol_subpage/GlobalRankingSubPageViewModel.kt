package net.ib.mn.presentation.main.ranking.idol_subpage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import net.ib.mn.ui.components.RankingItemData
import net.ib.mn.util.IdolImageUtil
import java.text.NumberFormat
import java.util.Locale

/**
 * Global (기적) 랭킹 ViewModel
 *
 * charts/ranks/ API 사용
 * 남녀 변경에 영향 받지 않음
 */
@HiltViewModel(assistedFactory = GlobalRankingSubPageViewModel.Factory::class)
class GlobalRankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @ApplicationContext private val context: Context,
    private val rankingRepository: RankingRepository,
    private val idolDao: IdolDao,
    private val broadcastManager: net.ib.mn.data.remote.udp.IdolBroadcastManager
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val items: List<RankingItemData>,
            val topIdol: IdolEntity? = null
        ) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var cachedData: List<RankingItemData>? = null
    private var topIdolCached: IdolEntity? = null
    private var cachedRanks: List<net.ib.mn.data.remote.dto.AggregateRankModel>? = null

    init {
        android.util.Log.d("GlobalRankingVM", "🆕 ViewModel created for chartCode: $chartCode")
        loadRankingData()

        // UDP 업데이트 이벤트 구독 (실시간 랭킹 업데이트)
        viewModelScope.launch {
            broadcastManager.updateEvent.collect { changedIds ->
                android.util.Log.d("GlobalRankingVM", "🔄 UDP update event received - ${changedIds.size} idols changed")

                // 캐시된 ranks 데이터가 있으면 재가공
                val ranks = cachedRanks
                if (ranks != null && ranks.isNotEmpty()) {
                    // 변경된 아이돌 중 현재 차트에 포함된 아이돌이 있는지 확인
                    val cachedIdolIds = ranks.map { it.idolId }
                    val hasRelevantChanges = changedIds.any { it in cachedIdolIds }

                    if (hasRelevantChanges) {
                        android.util.Log.d("GlobalRankingVM", "📊 Reprocessing ${ranks.size} ranks")
                        android.util.Log.d("GlobalRankingVM", "   → Changed IDs in this chart: ${changedIds.filter { it in cachedIdolIds }}")
                        android.util.Log.d("GlobalRankingVM", "   → DB에서 업데이트된 데이터 재조회 → 변경된 아이템만 리컴포지션")

                        launch(Dispatchers.IO) {
                            processRanksData(ranks)
                        }
                    } else {
                        android.util.Log.d("GlobalRankingVM", "⏭️ No relevant changes for this chart - skipping update")
                    }
                }
            }
        }
    }

    fun reloadIfNeeded() {
        if (cachedData != null) {
            android.util.Log.d("GlobalRankingVM", "✓ Using cached data")
            _uiState.value = UiState.Success(cachedData!!, topIdolCached)
        } else {
            loadRankingData()
        }
    }

    private fun loadRankingData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading

            android.util.Log.d("GlobalRankingVM", "========================================")
            android.util.Log.d("GlobalRankingVM", "[Global] Loading ranking data")
            android.util.Log.d("GlobalRankingVM", "  - chartCode: $chartCode")
            android.util.Log.d("GlobalRankingVM", "  - API: charts/ranks/")

            // charts/ranks/ API 호출
            rankingRepository.getChartRanks(chartCode).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        android.util.Log.d("GlobalRankingVM", "⏳ Loading...")
                    }
                    is ApiResult.Success -> {
                        android.util.Log.d("GlobalRankingVM", "✅ SUCCESS - Ranks count: ${result.data.size}")
                        cachedRanks = result.data
                        processRanksData(result.data)
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e("GlobalRankingVM", "❌ ERROR: ${result.message}")
                        _uiState.value = UiState.Error(result.message ?: "Error loading data")
                    }
                }
            }
        }
    }

    private suspend fun processRanksData(ranks: List<net.ib.mn.data.remote.dto.AggregateRankModel>) {
        try {
            // 모든 idol ID 추출
            val idolIds = ranks.map { it.idolId }

            // DB에서 idol 정보 가져오기
            val idols = idolDao.getIdolsByIds(idolIds)
            val idolMap = idols.associateBy { it.id }

            // 프로그레스 바 계산을 위한 최대/최소 점수
            val maxScore = ranks.maxOfOrNull { it.score.toLong() } ?: 0L
            val minScore = ranks.minOfOrNull { it.score.toLong() } ?: 0L

            // AggregateRankModel을 RankingItemData로 변환
            val rankItems = ranks.map { rank ->
                val idol = idolMap[rank.idolId]

                // name에서 이름과 그룹명 분리 (예: "디오_EXO" -> name="디오", groupName="EXO")
                val nameParts = rank.name.split("_")
                val actualName = nameParts.getOrNull(0) ?: rank.name
                val actualGroupName = nameParts.getOrNull(1)

                RankingItemData(
                    rank = rank.scoreRank,
                    name = actualName,
                    voteCount = formatScore(rank.score),
                    photoUrl = idol?.imageUrl,
                    id = rank.idolId.toString(),
                    groupName = actualGroupName,
                    miracleCount = idol?.miracleCount ?: 0,
                    fairyCount = idol?.fairyCount ?: 0,
                    angelCount = idol?.angelCount ?: 0,
                    rookieCount = idol?.rookieCount ?: 0,
                    heartCount = rank.score.toLong(),
                    maxHeartCount = maxScore,
                    minHeartCount = minScore,
                    top3ImageUrls = idol?.let { IdolImageUtil.getTop3ImageUrls(it) } ?: listOf(null, null, null),
                    top3VideoUrls = idol?.let { IdolImageUtil.getTop3VideoUrls(it) } ?: listOf(null, null, null)
                )
            }

            android.util.Log.d("GlobalRankingVM", "✅ Processed ${rankItems.size} items")

            // 1위 아이돌 정보 가져오기 (ExoTop3용)
            val topIdol = ranks.firstOrNull()?.let { topRank ->
                idolDao.getIdolById(topRank.idolId)
            }

            cachedData = rankItems
            topIdolCached = topIdol

            _uiState.value = UiState.Success(
                items = rankItems,
                topIdol = topIdol
            )
        } catch (e: Exception) {
            android.util.Log.e("GlobalRankingVM", "❌ Exception: ${e.message}", e)
            _uiState.value = UiState.Error(e.message ?: "Error")
        }
    }

    private fun formatScore(score: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(score)
    }

    @AssistedFactory
    interface Factory {
        fun create(chartCode: String): GlobalRankingSubPageViewModel
    }
}
