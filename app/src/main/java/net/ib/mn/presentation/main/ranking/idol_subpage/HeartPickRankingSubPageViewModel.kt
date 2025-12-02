package net.ib.mn.presentation.main.ranking.idol_subpage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.HeartPickModel
import net.ib.mn.domain.repository.HeartpickRepository
import net.ib.mn.ui.components.HeartPickState
import net.ib.mn.ui.components.IdolRankInfo
import net.ib.mn.util.IdolImageUtil
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.NumberFormatUtil

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

    private fun loadHeartPickList() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading


            // heartpick/ API 호출
            heartpickRepository.getHeartPickList(offset = 0, limit = 100).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                    }
                    is ApiResult.Success -> {
                        processHeartPickData(result.data)
                    }
                    is ApiResult.Error -> {
                        _uiState.value = UiState.Error(result.message ?: result.exception.message ?: "Error loading data")
                    }
                }
            }
        }
    }

    private fun processHeartPickData(heartPicks: List<HeartPickModel>) {
        try {
            val cardDataList = heartPicks.map { heartPick ->
                val state = when (heartPick.status) {
                    0 -> HeartPickState.UPCOMING  // 진행예정
                    1 -> HeartPickState.ACTIVE     // 진행중
                    else -> HeartPickState.ENDED   // 종료
                }

                val dDay = calculateDDay(heartPick.endAt, heartPick.status)

                val totalVote = heartPick.vote

                val firstPlaceIdol = if (state != HeartPickState.UPCOMING && heartPick.heartPickIdols?.isNotEmpty() == true) {
                    val first = heartPick.heartPickIdols[0]
                    val percentage = if (totalVote > 0) {
                        (100.0f * first.vote.toFloat() / totalVote.toFloat()).roundToInt()
                    } else 0

                    IdolRankInfo(
                        name = first.title,
                        groupName = first.subtitle,
                        photoUrl = first.imageUrl.toSecureUrl(),
                        voteCount = NumberFormatUtil.formatWithComma(first.vote),
                        percentage = percentage
                    )
                } else null

                val otherIdols = if (state != HeartPickState.UPCOMING && heartPick.heartPickIdols != null && heartPick.heartPickIdols.size > 1) {
                    heartPick.heartPickIdols.drop(1).take(10).map { idol ->
                        val percentage = if (totalVote > 0) {
                            (100.0f * idol.vote.toFloat() / totalVote.toFloat()).roundToInt()
                        } else 0

                        IdolRankInfo(
                            name = idol.title,
                            groupName = idol.subtitle,
                            photoUrl = idol.imageUrl.toSecureUrl(),
                            voteCount = NumberFormatUtil.formatWithComma(idol.vote),
                            percentage = percentage
                        )
                    }
                } else emptyList()

                // 상태에 따라 날짜 형식 다르게 처리
                // ACTIVE: "Until 2024.01.01" / "2024.01.01까지" 형식
                // ENDED: "2024.01.01 ~ 2024.01.02" 형식
                val periodDate = when (state) {
                    HeartPickState.ACTIVE -> formatEndDateWithUntil(heartPick.endAt)
                    else -> formatPeriodDate(heartPick.beginAt, heartPick.endAt)
                }
                val (openDate, openPeriod) = if (state == HeartPickState.UPCOMING) {
                    calculateOpenDate(heartPick.beginAt) to formatPeriod(heartPick.beginAt, heartPick.endAt)
                } else {
                    "" to ""
                }

                // 언어별 배너 URL 적용 및 HTTPS로 변환
                val localizedBannerUrl = IdolImageUtil.getLocalizedBannerUrl(context, heartPick.bannerUrl)
                val secureUrl = localizedBannerUrl.toSecureUrl()

                val cardData = HeartPickCardData(
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
                    isNew = false  // TODO: 신규 판별 로직 추가
                )

                cardData
            }


            cachedData = cardDataList

            _uiState.value = UiState.Success(cardDataList)
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Error")
        }
    }

    private fun calculateDDay(endAt: String, status: Int): String {
        return try {
            if (status == 0) {
                context.getString(net.ib.mn.R.string.upcoming)
            } else if (status == 2) {
                context.getString(net.ib.mn.R.string.vote_finish)
            } else {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val now = Calendar.getInstance().time
                val endDate = dateFormat.parse(endAt)

                if (endDate != null) {
                    val diff = endDate.time - now.time
                    val days = diff / (1000 * 60 * 60 * 24)

                    when {
                        diff < 0 -> context.getString(net.ib.mn.R.string.vote_finish)
                        diff < 86400000 -> {
                            val hours = diff / (1000 * 60 * 60)
                            val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
                            String.format(Locale.US, "%02d:%02d", hours, minutes)
                        }
                        else -> "D-$days"
                    }
                } else {
                    "D-Day"
                }
            }
        } catch (e: Exception) {
            "D-Day"
        }
    }

    private fun calculateOpenDate(beginAt: String): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val now = Calendar.getInstance().time
            val beginDate = dateFormat.parse(beginAt)

            if (beginDate != null) {
                val diff = beginDate.time - now.time
                val days = diff / (1000 * 60 * 60 * 24)

                if (days > 0) {
                    "Vote Open D-$days"
                } else {
                    "Vote Open"
                }
            } else {
                "Vote Open"
            }
        } catch (e: Exception) {
            "Vote Open"
        }
    }

    /**
     * 종료일을 "Until 2024.01.01" / "2024.01.01까지" 형식으로 포맷
     * strings.xml의 finish_at 문자열 사용
     */
    private fun formatEndDateWithUntil(endAt: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

            val endDate = inputFormat.parse(endAt)
            if (endDate != null) {
                val dateStr = outputFormat.format(endDate)
                context.getString(net.ib.mn.R.string.finish_at, dateStr)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatPeriodDate(beginAt: String, endAt: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

            val beginDate = inputFormat.parse(beginAt)
            val endDate = inputFormat.parse(endAt)

            if (beginDate != null && endDate != null) {
                "${outputFormat.format(beginDate)} ~ ${outputFormat.format(endDate)}"
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatPeriod(beginAt: String, endAt: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

            val beginDate = inputFormat.parse(beginAt)
            val endDate = inputFormat.parse(endAt)

            if (beginDate != null && endDate != null) {
                "${outputFormat.format(beginDate)} ~ ${outputFormat.format(endDate)}"
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
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
