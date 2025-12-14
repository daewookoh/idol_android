package net.ib.mn.presentation.main.ranking.idol_subpage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.HeartPickIdol
import net.ib.mn.domain.model.HeartPickModel
import net.ib.mn.domain.repository.HeartpickRepository
import net.ib.mn.ui.components.HeartPickState
import net.ib.mn.ui.components.IdolRankInfo
import net.ib.mn.util.DateTimeUtil
import net.ib.mn.util.IdolImageUtil
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.NumberFormatUtil
import net.ib.mn.util.RankingUtil

/**
 * HeartPick 랭킹 ViewModel
 *
 * heartpick/ API 사용
 */
@HiltViewModel(assistedFactory = HeartPickRankingSubPageViewModel.Factory::class)
class HeartPickRankingSubPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @ApplicationContext private val context: Context,
    private val heartpickRepository: HeartpickRepository
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val items: List<HeartPickCardData>
        ) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var cachedData: List<HeartPickCardData>? = null

    init {
        loadHeartPickList()
    }

    fun reloadIfNeeded() {
        if (cachedData != null) {
            _uiState.value = UiState.Success(cachedData!!)
        } else {
            loadHeartPickList()
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            heartpickRepository.getHeartPickList(offset = 0, limit = 100).collect { result ->
                when (result) {
                    is ApiResult.Loading -> Unit
                    is ApiResult.Success -> {
                        processHeartPickData(result.data)
                        _isRefreshing.value = false
                    }
                    is ApiResult.Error -> {
                        _uiState.value = UiState.Error(result.message ?: result.exception.message ?: "Error loading data")
                        _isRefreshing.value = false
                    }
                }
            }
        }
    }

    private fun loadHeartPickList() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading

            heartpickRepository.getHeartPickList(offset = 0, limit = 100).collect { result ->
                when (result) {
                    is ApiResult.Loading -> Unit
                    is ApiResult.Success -> processHeartPickData(result.data)
                    is ApiResult.Error -> {
                        _uiState.value = UiState.Error(result.message ?: result.exception.message ?: "Error loading data")
                    }
                }
            }
        }
    }

    private fun processHeartPickData(heartPicks: List<HeartPickModel>) {
        try {
            val cardDataList = heartPicks.mapIndexed { index, heartPick ->
                val state = when (heartPick.status) {
                    0 -> HeartPickState.UPCOMING  // 진행예정
                    1 -> HeartPickState.ACTIVE     // 진행중
                    else -> HeartPickState.ENDED   // 종료
                }

                val dDay = DateTimeUtil.calculateDDay(context, heartPick.endAt, heartPick.status)

                val totalVote = heartPick.vote

                // RankingUtil을 사용하여 정렬 및 순위 계산
                val sortedIdols = heartPick.heartPickIdols?.let {
                    RankingUtil.sortAndRankHeartPickIdols(it)
                }

                fun HeartPickIdol.toIdolRankInfo(): IdolRankInfo {
                    val percentage = if (totalVote > 0) {
                        (100.0f * vote.toFloat() / totalVote.toFloat()).roundToInt()
                    } else 0
                    return IdolRankInfo(
                        name = title,
                        groupName = subtitle,
                        photoUrl = imageUrl.toSecureUrl(),
                        voteCount = context.getString(R.string.vote_count_format, NumberFormatUtil.formatWithComma(vote)),
                        voteCountRaw = vote.toLong(),
                        percentage = percentage,
                        rank = rank  // RankingUtil에서 계산된 동점자 순위
                    )
                }

                val firstPlaceIdol = if (state != HeartPickState.UPCOMING && sortedIdols?.isNotEmpty() == true) {
                    sortedIdols[0].toIdolRankInfo()
                } else null

                val otherIdols = if (state != HeartPickState.UPCOMING && sortedIdols != null && sortedIdols.size > 1) {
                    sortedIdols.drop(1).take(10).map { it.toIdolRankInfo() }
                } else emptyList()

                // 상태에 따라 날짜 형식 다르게 처리
                // ACTIVE: "Until 2024.01.01" / "2024.01.01까지" 형식
                // ENDED: "2024.01.01 ~ 2024.01.02" 형식
                val periodDate = when (state) {
                    HeartPickState.ACTIVE -> DateTimeUtil.formatEndDateWithUntil(context, heartPick.endAt)
                    else -> DateTimeUtil.formatPeriod(heartPick.beginAt, heartPick.endAt)
                }
                val (openDate, openPeriod) = if (state == HeartPickState.UPCOMING) {
                    DateTimeUtil.calculateOpenDDay(context, heartPick.beginAt) to DateTimeUtil.formatPeriod(heartPick.beginAt, heartPick.endAt)
                } else {
                    "" to ""
                }

                // 언어별 배너 URL 적용 및 HTTPS로 변환
                val localizedBannerUrl = IdolImageUtil.getLocalizedBannerUrl(context, heartPick.bannerUrl)
                val secureUrl = localizedBannerUrl.toSecureUrl()

                // 신규 카드 여부: 첫 번째 아이템이면서 48시간 이내에 시작된 경우
                val isNew = index == 0 && DateTimeUtil.isWithin48Hours(heartPick.beginAt)

                HeartPickCardData(
                    id = heartPick.id,
                    state = state,
                    title = heartPick.title,
                    subTitle = heartPick.subtitle,
                    backgroundImageUrl = secureUrl,
                    dDay = dDay,
                    firstPlaceIdol = firstPlaceIdol,
                    otherIdols = otherIdols,
                    heartVoteCount = NumberFormatUtil.formatWithComma(heartPick.vote),
                    commentCount = NumberFormatUtil.formatWithComma(heartPick.numComments),
                    periodDate = periodDate,
                    openDate = openDate,
                    openPeriod = openPeriod,
                    isNew = isNew
                )
            }

            cachedData = cardDataList

            _uiState.value = UiState.Success(cardDataList)
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Error")
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(chartCode: String): HeartPickRankingSubPageViewModel
    }
}

data class HeartPickCardData(
    val id: Int,
    val state: HeartPickState,
    val title: String,
    val subTitle: String,
    val backgroundImageUrl: String,
    val dDay: String,
    val firstPlaceIdol: IdolRankInfo?,
    val otherIdols: List<IdolRankInfo>,
    val heartVoteCount: String,
    val commentCount: String,
    val periodDate: String,
    val openDate: String,
    val openPeriod: String,
    val isNew: Boolean
)
