package net.ib.mn.presentation.awards.subpage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.data.remote.dto.AwardChartsModel
import net.ib.mn.data.remote.dto.AwardModel
import net.ib.mn.data.remote.dto.AwardRankIdol
import net.ib.mn.data.remote.dto.AwardRankItem
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.AwardsRepository
import net.ib.mn.domain.repository.ConfigRepository
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.util.IdolImageUtil
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * AwardsCumulativeViewModel - 어워즈 누적순위 ViewModel
 *
 * old: AwardsAggregatedViewModel + AwardsAggregatedFragment 로직 통합
 */
@HiltViewModel
class AwardsCumulativeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configRepository: ConfigRepository,
    private val awardsRepository: AwardsRepository,
    private val idolRepository: IdolRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    // UI 상태 (순위 리스트용 - 탭 변경시에만 리렌더)
    private val _uiState = MutableStateFlow<AwardsCumulativeUiState>(AwardsCumulativeUiState.Loading)
    val uiState: StateFlow<AwardsCumulativeUiState> = _uiState.asStateFlow()

    // 선택된 카테고리 (chart)
    private val _selectedChartIndex = MutableStateFlow(0)
    val selectedChartIndex: StateFlow<Int> = _selectedChartIndex.asStateFlow()

    // 탭별 랭킹 데이터 캐시 (chartCode -> rankItems)
    private val rankDataCache = mutableMapOf<String, List<AwardRankItem>>()

    // AwardModel (캐시)
    val awardModel: StateFlow<AwardModel?> = configRepository.observeAwardModel()

    // CDN URL (이미지 로딩용)
    val cdnUrl: StateFlow<String> = preferencesManager.cdnUrl
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // votable 상태: "B" = 준비중, "Y" = 진행중, "A" = 종료/결과
    val votable: String
        get() = configRepository.getVotable()

    // 투표 기간
    private val awardBegin: Date?
        get() = parseDate(configRepository.getAwardBegin())

    private val awardEnd: Date?
        get() = parseDate(configRepository.getAwardEnd())

    init {
        loadRankData()
    }

    /**
     * 순위 데이터 로드
     * @param forceRefresh true면 캐시 무시하고 새로 로드
     */
    fun loadRankData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val award = awardModel.value
            val charts = award?.charts

            if (charts.isNullOrEmpty()) {
                _uiState.value = AwardsCumulativeUiState.Empty(
                    message = context.getString(R.string.label_award_aggregated_no_data)
                )
                return@launch
            }

            val selectedChart = charts.getOrNull(_selectedChartIndex.value) ?: charts.first()
            val chartCode = selectedChart.code ?: ""
            val event = award.name

            // 캐시된 데이터가 있으면 바로 표시 (API 호출 안함)
            val cachedData = rankDataCache[chartCode]
            if (!forceRefresh && cachedData != null) {
                logD("AwardsCumulative", "Using cached data for chartCode=$chartCode")
                _uiState.value = AwardsCumulativeUiState.Success(
                    rankItems = cachedData,
                    charts = charts,
                    selectedChartIndex = _selectedChartIndex.value
                )
                return@launch
            }

            // 캐시가 없어도 로딩바 안돌림 - 기존 데이터 유지
            // 첫 로드시에만 로딩 표시
            val currentState = _uiState.value
            if (currentState is AwardsCumulativeUiState.Loading ||
                currentState is AwardsCumulativeUiState.Error ||
                currentState is AwardsCumulativeUiState.Empty) {
                _uiState.value = AwardsCumulativeUiState.Loading
            }

            logD("AwardsCumulative", "Loading rank data: chartCode=$chartCode, event=$event")

            awardsRepository.getAwardRank(chartCode, event).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        // 로딩은 이미 위에서 처리됨
                    }
                    is ApiResult.Error -> {
                        logE("AwardsCumulative", "API Error: ${result.error}")
                        _uiState.value = AwardsCumulativeUiState.Error(
                            message = result.error.message ?: context.getString(R.string.no_data)
                        )
                    }
                    is ApiResult.Success -> {
                        val body = result.data

                        if (body.success && !body.data.isNullOrEmpty()) {
                            // idol_id 리스트 수집
                            val idolIds = body.data.mapNotNull { it.idolId.takeIf { id -> id > 0 } }

                            // DB에서 아이돌 정보 조회
                            val idolMap: Map<Int, IdolEntity> = if (idolIds.isNotEmpty()) {
                                try {
                                    idolRepository.getIdolsByIds(idolIds)
                                        .associateBy { it.id }
                                } catch (e: Exception) {
                                    logE("AwardsCumulative", "Failed to load idols: ${e.message}")
                                    emptyMap()
                                }
                            } else {
                                emptyMap()
                            }

                            // 동점자 처리 및 아이돌 정보 매핑
                            val currentCdnUrl = cdnUrl.value
                            val rankItems = body.data.mapIndexed { index, item ->
                                val idolEntity: IdolEntity? = idolMap[item.idolId]
                                item.copy().apply {
                                    rank = if (index > 0 && body.data[index - 1].score == item.score) {
                                        body.data[index - 1].rank
                                    } else {
                                        index
                                    }
                                    // 아이돌 정보 매핑
                                    this.idol = idolEntity?.let { entity ->
                                        AwardRankIdol(
                                            id = entity.id,
                                            name = entity.name,
                                            nameEn = entity.nameEn,
                                            group = null,
                                            groupEn = null,
                                            imageUrl = IdolImageUtil.getTrendImageUrl(
                                                cdnUrl = currentCdnUrl,
                                                trendId = item.id
                                            ),
                                            sourceApp = entity.sourceApp
                                        )
                                    }
                                }
                            }

                            // 캐시에 저장
                            rankDataCache[chartCode] = rankItems

                            _uiState.value = AwardsCumulativeUiState.Success(
                                rankItems = rankItems,
                                charts = charts,
                                selectedChartIndex = _selectedChartIndex.value
                            )
                        } else {
                            _uiState.value = AwardsCumulativeUiState.Empty(
                                message = context.getString(R.string.label_award_aggregated_no_data),
                                charts = charts,
                                selectedChartIndex = _selectedChartIndex.value
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 카테고리(차트) 선택
     * 캐시된 데이터가 있으면 바로 표시
     */
    fun selectChart(index: Int) {
        if (_selectedChartIndex.value != index) {
            _selectedChartIndex.value = index
            loadRankData() // 캐시된 데이터 있으면 바로 표시
        }
    }

    /**
     * 투표 기간 문자열 (상단 헤더용)
     * 예: "투표기간 : 2024. 1. 1. ~ 2024. 1. 31."
     */
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
     * 집계 기간 문자열 (순위 리스트 헤더용)
     * votable == "Y"면 시작일~어제까지, 그 외에는 시작일~종료일
     */
    fun getAggregationPeriodText(): String {
        val begin = awardBegin ?: return ""
        val end = if (votable == "Y") {
            Calendar.getInstance().apply {
                add(Calendar.DATE, -1)
            }.time
        } else {
            awardEnd ?: return ""
        }
        return String.format(
            "%1s ~ %2s %3s",
            formatDate(begin),
            formatDate(end),
            context.getString(R.string.label_header_result)
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

/**
 * UI State
 */
sealed class AwardsCumulativeUiState {
    data object Loading : AwardsCumulativeUiState()

    data class Success(
        val rankItems: List<AwardRankItem>,
        val charts: List<AwardChartsModel>,
        val selectedChartIndex: Int
    ) : AwardsCumulativeUiState()

    data class Empty(
        val message: String,
        val charts: List<AwardChartsModel>? = null,
        val selectedChartIndex: Int = 0
    ) : AwardsCumulativeUiState()

    data class Error(
        val message: String
    ) : AwardsCumulativeUiState()
}
