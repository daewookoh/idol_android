package net.ib.mn.presentation.awards.subpage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.data.manager.AwardsRankingManager
import net.ib.mn.data.remote.dto.AwardChartsModel
import net.ib.mn.data.remote.dto.AwardIdolItem
import net.ib.mn.data.remote.dto.AwardModel
import net.ib.mn.data.remote.dto.AwardRankIdol
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.AwardsRepository
import net.ib.mn.domain.repository.ConfigRepository
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * AwardsDailyViewModel - 어워즈 실시간(Daily) 순위 ViewModel
 *
 * 기능:
 * 1. 입장 시 모든 차트의 idol_ids 병렬 로드
 * 2. AwardsRankingManager에 데이터 저장
 * 3. 2초마다 랭킹 자동 갱신 (Manager에서 처리)
 */
@HiltViewModel
class AwardsDailyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configRepository: ConfigRepository,
    private val awardsRepository: AwardsRepository,
    private val idolRepository: IdolRepository,
    private val awardsRankingManager: AwardsRankingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AwardsDailyUiState>(AwardsDailyUiState.Loading)
    val uiState: StateFlow<AwardsDailyUiState> = _uiState.asStateFlow()

    private val _selectedChartIndex = MutableStateFlow(0)
    val selectedChartIndex: StateFlow<Int> = _selectedChartIndex.asStateFlow()

    // 차트 목록 캐시
    private var charts: List<AwardChartsModel> = emptyList()

    // 랭킹 Flow collect Job
    private var rankingCollectJob: Job? = null

    // 모든 차트 로드 완료 여부
    private var allChartsLoaded = false

    val awardModel: StateFlow<AwardModel?> = configRepository.observeAwardModel()

    val votable: String
        get() = configRepository.getVotable()

    private val awardBegin: Date?
        get() = parseDate(configRepository.getAwardBegin())

    private val awardEnd: Date?
        get() = parseDate(configRepository.getAwardEnd())

    init {
        loadAllCharts()
    }

    /**
     * 모든 차트의 데이터를 병렬로 로드
     */
    private fun loadAllCharts() {
        viewModelScope.launch {
            val award = awardModel.value
            charts = award?.charts ?: emptyList()

            if (charts.isEmpty()) {
                _uiState.value = AwardsDailyUiState.Empty(
                    message = context.getString(R.string.label_award_aggregated_no_data)
                )
                return@launch
            }

            _uiState.value = AwardsDailyUiState.Loading
            logD("AwardsDaily", "Loading all ${charts.size} charts in parallel")

            // 모든 차트 데이터 병렬 로드
            val loadJobs = charts.mapNotNull { chart ->
                chart.code?.let { chartCode ->
                    async { loadChartData(chartCode) }
                }
            }

            // 모든 로드 완료 대기
            loadJobs.awaitAll()
            allChartsLoaded = true

            logD("AwardsDaily", "All charts loaded, starting ranking flow for selected chart")

            // 선택된 차트의 랭킹 Flow 구독 시작
            startCollectingSelectedChartRanking()
        }
    }

    /**
     * 단일 차트 데이터 로드
     */
    private suspend fun loadChartData(chartCode: String) {
        logD("AwardsDaily", "Loading chart: $chartCode")

        try {
            awardsRepository.getAwardIdols(chartCode, "heart,top3").first { result ->
                when (result) {
                    is ApiResult.Loading -> false
                    is ApiResult.Error -> {
                        logE("AwardsDaily", "API Error for $chartCode: ${result.error}")
                        true
                    }
                    is ApiResult.Success -> {
                        val body = result.data
                        if (body.success && !body.objects.isNullOrEmpty()) {
                            val idolIds = body.objects.mapNotNull { it.id.takeIf { id -> id > 0 } }
                            logD("AwardsDaily", "Chart $chartCode loaded: ${idolIds.size} idols")

                            // idol 정보 로드
                            val idolMap: Map<Int, IdolEntity> = if (idolIds.isNotEmpty()) {
                                try {
                                    idolRepository.getIdolsByIds(idolIds).associateBy { it.id }
                                } catch (e: Exception) {
                                    logE("AwardsDaily", "Failed to load idols for $chartCode: ${e.message}")
                                    emptyMap()
                                }
                            } else {
                                emptyMap()
                            }

                            // idol 정보 매핑
                            val itemsWithIdol = body.objects.map { item ->
                                val idolEntity = idolMap[item.id]
                                item.copy().apply {
                                    idol = idolEntity?.let { entity ->
                                        AwardRankIdol(
                                            id = entity.id,
                                            name = entity.name,
                                            nameEn = entity.nameEn,
                                            group = null,
                                            groupEn = null,
                                            imageUrl = entity.imageUrl,
                                            sourceApp = entity.sourceApp
                                        )
                                    }
                                }
                            }

                            // AwardsRankingManager에 저장 (1초마다 자동 정렬)
                            awardsRankingManager.updateChartData(chartCode, itemsWithIdol)
                        }
                        true
                    }
                }
            }
        } catch (e: Exception) {
            logE("AwardsDaily", "Exception loading $chartCode: ${e.message}")
        }
    }

    /**
     * 선택된 차트의 랭킹 Flow 구독
     */
    private fun startCollectingSelectedChartRanking() {
        val selectedChart = charts.getOrNull(_selectedChartIndex.value)
        val chartCode = selectedChart?.code ?: return

        rankingCollectJob?.cancel()
        rankingCollectJob = viewModelScope.launch {
            logD("AwardsDaily", "Subscribing to ranking flow: $chartCode")

            awardsRankingManager.getChartRankingFlow(chartCode).collect { rankItems ->
                if (rankItems.isNotEmpty()) {
                    logD("AwardsDaily", "Ranking updated: $chartCode, count=${rankItems.size}")
                    _uiState.value = AwardsDailyUiState.Success(
                        rankItems = rankItems,
                        charts = charts,
                        selectedChartIndex = _selectedChartIndex.value
                    )
                } else if (allChartsLoaded) {
                    _uiState.value = AwardsDailyUiState.Empty(
                        message = context.getString(R.string.label_award_aggregated_no_data),
                        charts = charts,
                        selectedChartIndex = _selectedChartIndex.value
                    )
                }
            }
        }
    }

    /**
     * 차트 선택 변경
     */
    fun selectChart(index: Int) {
        if (_selectedChartIndex.value != index) {
            _selectedChartIndex.value = index
            startCollectingSelectedChartRanking()
        }
    }

    /**
     * 데이터 새로고침
     */
    fun refresh() {
        allChartsLoaded = false
        loadAllCharts()
    }

    /**
     * 투표 성공 시 heart 업데이트
     * 로컬 DB 업데이트 후 즉시 리랭킹
     */
    fun onVoteSuccess(idolId: Int, votedHeart: Long) {
        val selectedChart = charts.getOrNull(_selectedChartIndex.value)
        val chartCode = selectedChart?.code ?: return

        // 현재 heart 값에 votedHeart 추가
        val currentItems = awardsRankingManager.getChartRankingFlow(chartCode).value
        val currentHeart = currentItems.find { it.id == idolId }?.heart ?: 0L
        val newHeart = currentHeart + votedHeart

        logD("AwardsDaily", "Vote success: idolId=$idolId, votedHeart=$votedHeart, newHeart=$newHeart")
        awardsRankingManager.updateHeart(chartCode, idolId, newHeart)
    }

    fun getVotingPeriodText(): String {
        val begin = awardBegin ?: return ""
        val end = awardEnd ?: return ""
        return String.format(
            context.getString(R.string.gaon_voting_period),
            formatDate(begin),
            formatDate(end)
        )
    }

    /**
     * 집계 기간 바 텍스트
     */
    fun getAggregationPeriodText(): String {
        val now = Date()
        return String.format(
            context.getString(R.string.gaon_current_title),
            formatDate(now)
        )
    }

    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrEmpty()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = seoulTimeZone
            }.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDate(date: Date): String {
        val formatter = if (LocaleUtil.getAppLocale(context) == Locale.KOREA) {
            DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(context))
        } else {
            DateFormat.getDateInstance(DateFormat.DATE_FIELD, LocaleUtil.getAppLocale(context))
        }
        return SimpleDateFormat(
            (formatter as SimpleDateFormat).toLocalizedPattern(),
            LocaleUtil.getAppLocale(context)
        ).apply {
            timeZone = seoulTimeZone
        }.format(date)
    }

    companion object {
        private val seoulTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Seoul")
    }
}

sealed class AwardsDailyUiState {
    data object Loading : AwardsDailyUiState()

    data class Success(
        val rankItems: List<AwardIdolItem>,
        val charts: List<AwardChartsModel>,
        val selectedChartIndex: Int
    ) : AwardsDailyUiState()

    data class Empty(
        val message: String,
        val charts: List<AwardChartsModel>? = null,
        val selectedChartIndex: Int = 0
    ) : AwardsDailyUiState()

    data class Error(
        val message: String
    ) : AwardsDailyUiState()
}
