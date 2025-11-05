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
import kotlinx.coroutines.Job
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
 * OnePick (기적) 랭킹 ViewModel
 *
 * charts/ranks/ API 사용
 * 남녀 변경에 영향 받지 않음
 */
@HiltViewModel(assistedFactory = OnePickRankingSubPageViewModel.Factory::class)
class OnePickRankingSubPageViewModel @AssistedInject constructor(
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

    // UDP 구독 Job (화면에 보일 때만 활성화)
    private var udpSubscriptionJob: Job? = null

    // 화면 가시성 상태
    private var isScreenVisible = false

    init {
        android.util.Log.d("OnePickRankingVM", "🆕 ViewModel created for chartCode: $chartCode")
        loadRankingData()
    }

    /**
     * 화면이 보일 때 호출 - UDP 구독 시작 및 데이터 새로고침
     */
    fun onScreenVisible() {
        android.util.Log.d("OnePickRankingVM", "👁️ Screen became visible")
        isScreenVisible = true

        // DB에서 최신 데이터 로드
        val ranks = cachedRanks
        if (ranks != null && ranks.isNotEmpty()) {
            android.util.Log.d("OnePickRankingVM", "🔄 Refreshing data from DB (${ranks.size} items)")
            viewModelScope.launch(Dispatchers.IO) {
                processRanksData(ranks)
            }
        }

        // UDP 구독 시작
        startUdpSubscription()
    }

    /**
     * 화면이 사라질 때 호출 - UDP 구독 중지
     */
    fun onScreenHidden() {
        android.util.Log.d("OnePickRankingVM", "🙈 Screen hidden")
        isScreenVisible = false
        stopUdpSubscription()
    }

    /**
     * UDP 구독 시작
     */
    private fun startUdpSubscription() {
        // 이미 구독 중이면 중복 방지
        if (udpSubscriptionJob?.isActive == true) {
            android.util.Log.d("OnePickRankingVM", "⚠️ UDP already subscribed, skipping")
            return
        }

        android.util.Log.d("OnePickRankingVM", "📡 Starting UDP subscription")
        udpSubscriptionJob = viewModelScope.launch {
            broadcastManager.updateEvent.collect { changedIds ->
                // 화면이 보이지 않으면 무시
                if (!isScreenVisible) {
                    android.util.Log.d("OnePickRankingVM", "⏭️ Screen not visible, ignoring UDP update")
                    return@collect
                }

                android.util.Log.d("OnePickRankingVM", "🔄 UDP update event received - ${changedIds.size} idols changed")

                // 캐시된 ranks 데이터가 있으면 재가공
                val ranks = cachedRanks
                if (ranks != null && ranks.isNotEmpty()) {
                    // 변경된 아이돌 중 현재 차트에 포함된 아이돌이 있는지 확인
                    val cachedIdolIds = ranks.map { it.idolId }
                    val hasRelevantChanges = changedIds.any { it in cachedIdolIds }

                    if (hasRelevantChanges) {
                        android.util.Log.d("OnePickRankingVM", "📊 Reprocessing ${ranks.size} ranks")
                        android.util.Log.d("OnePickRankingVM", "   → Changed IDs in this chart: ${changedIds.filter { it in cachedIdolIds }}")
                        android.util.Log.d("OnePickRankingVM", "   → DB에서 업데이트된 데이터 재조회 → 변경된 아이템만 리컴포지션")

                        launch(Dispatchers.IO) {
                            processRanksData(ranks)
                        }
                    } else {
                        android.util.Log.d("OnePickRankingVM", "⏭️ No relevant changes for this chart - skipping update")
                    }
                }
            }
        }
    }

    /**
     * UDP 구독 중지
     */
    private fun stopUdpSubscription() {
        udpSubscriptionJob?.cancel()
        udpSubscriptionJob = null
        android.util.Log.d("OnePickRankingVM", "🛑 Stopped UDP subscription")
    }

    override fun onCleared() {
        super.onCleared()
        stopUdpSubscription()
        android.util.Log.d("OnePickRankingVM", "♻️ ViewModel cleared")
    }

    fun reloadIfNeeded() {
        if (cachedData != null) {
            android.util.Log.d("OnePickRankingVM", "✓ Using cached data")
            _uiState.value = UiState.Success(cachedData!!, topIdolCached)
        } else {
            loadRankingData()
        }
    }

    private fun loadRankingData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading

            android.util.Log.d("OnePickRankingVM", "========================================")
            android.util.Log.d("OnePickRankingVM", "[OnePick] Loading ranking data")
            android.util.Log.d("OnePickRankingVM", "  - chartCode: $chartCode")
            android.util.Log.d("OnePickRankingVM", "  - API: charts/ranks/")

            // charts/ranks/ API 호출
            rankingRepository.getChartRanks(chartCode).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        android.util.Log.d("OnePickRankingVM", "⏳ Loading...")
                    }
                    is ApiResult.Success -> {
                        android.util.Log.d("OnePickRankingVM", "✅ SUCCESS - Ranks count: ${result.data.size}")
                        cachedRanks = result.data
                        processRanksData(result.data)
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e("OnePickRankingVM", "❌ ERROR: ${result.message}")
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

            // AggregateRankModel을 RankingItemData로 변환
            val maxScore = ranks.maxOfOrNull { it.score.toLong() } ?: 0L
            val minScore = ranks.minOfOrNull { it.score.toLong() } ?: 0L

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

            android.util.Log.d("OnePickRankingVM", "✅ Processed ${rankItems.size} items")

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
            android.util.Log.e("OnePickRankingVM", "❌ Exception: ${e.message}", e)
            _uiState.value = UiState.Error(e.message ?: "Error")
        }
    }

    private fun formatScore(score: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(score)
    }

    @AssistedFactory
    interface Factory {
        fun create(chartCode: String): OnePickRankingSubPageViewModel
    }
}
