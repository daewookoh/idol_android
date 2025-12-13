package net.ib.mn.presentation.themepick.live

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.base.BaseViewModel
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ThemePickIdol
import net.ib.mn.domain.model.ThemePickModel
import net.ib.mn.domain.repository.ThemepickRepository
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.link.LinkUtil
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * ThemePickLive (테마픽 순위 결과) 화면 ViewModel
 *
 * old 프로젝트: ThemePickLiveActivity
 */
@HiltViewModel
class ThemePickLiveViewModel @Inject constructor(
    private val themepickRepository: ThemepickRepository,
    @ApplicationContext private val context: Context
) : BaseViewModel<ThemePickLiveContract.State, ThemePickLiveContract.Intent, ThemePickLiveContract.Effect>() {

    private var currentThemePickId: Int = 0

    override fun createInitialState(): ThemePickLiveContract.State = ThemePickLiveContract.State()

    override fun handleIntent(intent: ThemePickLiveContract.Intent) {
        when (intent) {
            is ThemePickLiveContract.Intent.LoadResult -> loadResult(intent.themePickId)
            is ThemePickLiveContract.Intent.Share -> shareThemePick()
            is ThemePickLiveContract.Intent.ToggleRewardExpand -> toggleRewardExpand()
            is ThemePickLiveContract.Intent.GoToVote -> goToVote()
            is ThemePickLiveContract.Intent.OnItemClick -> onItemClick(intent.idolId)
        }
    }

    private fun loadResult(themePickId: Int) {
        if (currentState.isLoading && currentState.themePick != null) return

        currentThemePickId = themePickId
        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            themepickRepository.getThemePick(themePickId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val themePick = result.data
                        processThemePickData(themePick)
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        setEffect {
                            ThemePickLiveContract.Effect.ShowToast(
                                result.message ?: context.getString(R.string.desc_failed_to_connect_internet)
                            )
                        }
                    }
                    is ApiResult.Loading -> {
                        // Already isLoading = true
                    }
                }
            }
        }
    }

    private fun processThemePickData(themePick: ThemePickModel) {
        // 후보 정렬 및 순위 계산
        val processedThemePick = sortAndRankCandidates(themePick)

        // 기간 텍스트 계산
        val periodText = calculatePeriodText(processedThemePick)

        // 이미지 URL에 secureUrl 적용
        val secureImageUrl = processedThemePick.imageUrl.toSecureUrl()

        // 랭킹 아이템 생성
        val rankItems = processedThemePick.candidates ?: emptyList()

        setState {
            copy(
                themePick = processedThemePick,
                rankItems = rankItems,
                periodText = periodText,
                secureImageUrl = secureImageUrl,
                canVote = processedThemePick.canVote(),
                needsVideoAd = processedThemePick.needsVideoAd(),
                hasVotedToday = processedThemePick.hasVotedToday(),
                isFinished = processedThemePick.isFinished(),
                isLoading = false
            )
        }
    }

    /**
     * 후보 정렬 및 순위 계산
     * old 프로젝트: ThemePickLiveActivity.loadRank()의 정렬 로직
     */
    private fun sortAndRankCandidates(themePick: ThemePickModel): ThemePickModel {
        val candidates = themePick.candidates ?: return themePick

        // 투표수 내림차순, 동점이면 이름순으로 정렬
        val sortedCandidates = ArrayList(candidates.sortedWith { o1, o2 ->
            when {
                o1.vote > o2.vote -> -1
                o1.vote < o2.vote -> 1
                else -> o1.title.compareTo(o2.title)
            }
        })

        // 동점자 처리하여 순위 부여
        var rank = 0
        sortedCandidates.forEachIndexed { index, candidate ->
            rank = when {
                index == 0 -> 1
                sortedCandidates[index - 1].vote == candidate.vote -> sortedCandidates[index - 1].rank
                else -> index + 1
            }
            candidate.rank = rank

            // 1위와의 득표차 계산
            val firstPlaceVote = sortedCandidates.firstOrNull()?.vote ?: 0L
            candidate.diffVote = firstPlaceVote - candidate.vote
            candidate.firstPlaceVote = firstPlaceVote
            candidate.lastPlaceVote = sortedCandidates.lastOrNull()?.vote ?: 1L
        }

        return themePick.copy(candidates = sortedCandidates)
    }

    /**
     * 기간 텍스트 계산
     */
    private fun calculatePeriodText(themePick: ThemePickModel): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

            val beginDate = inputFormat.parse(themePick.beginAt)
            val expiredDate = inputFormat.parse(themePick.expiredAt)

            if (beginDate != null && expiredDate != null) {
                "${outputFormat.format(beginDate)} ~ ${outputFormat.format(expiredDate)}"
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 테마픽 공유하기
     */
    private fun shareThemePick() {
        val themePick = currentState.themePick ?: return

        // 딥링크 URL 생성
        val url = LinkUtil.getAppLinkUrl(
            context = context,
            params = listOf("themepick", themePick.id.toString())
        )

        val shareText = shareThemePickLive(themePick, url)
        setEffect { ThemePickLiveContract.Effect.ShareThemePick(shareText) }
    }

    /**
     * 진행중/종료 상태 공유 포맷
     */
    private fun shareThemePickLive(themePick: ThemePickModel, url: String): String {
        val candidates = themePick.candidates ?: return ""

        val top1Name = candidates.getOrNull(0)?.let { formatCandidateName(it) }.orEmpty()
        val top2Name = candidates.getOrNull(1)?.let { formatCandidateName(it) }.orEmpty()
        val top3Name = candidates.getOrNull(2)?.let { formatCandidateName(it) }.orEmpty()

        val rank1 = if (top1Name.isNotEmpty()) context.getString(R.string.rank_format, "1") else ""
        val rank2 = if (top2Name.isNotEmpty()) context.getString(R.string.rank_format, "2") else ""
        val rank3 = if (top3Name.isNotEmpty()) context.getString(R.string.rank_format, "3") else ""

        val msg = context.getString(
            R.string.share_themepick,
            themePick.title,
            rank1, "#$top1Name",
            rank2, "#$top2Name",
            rank3, "#$top3Name",
            url
        )

        return msg.trimNewlineWhiteSpace()
    }

    private fun formatCandidateName(candidate: ThemePickIdol): String {
        return if (candidate.subtitle.isEmpty()) {
            candidate.title.replace("\\s+".toRegex(), "")
        } else {
            "${candidate.title}_${candidate.subtitle}".replace("\\s+".toRegex(), "")
        }
    }

    private fun String.trimNewlineWhiteSpace(): String {
        return this.replace(Regex("\\n\\s*\\n\\s*\\n+"), "\n\n").trim()
    }

    /**
     * 리워드 펼치기/접기 토글
     */
    private fun toggleRewardExpand() {
        setState { copy(isRewardExpanded = !isRewardExpanded) }
    }

    /**
     * 투표하기 버튼 클릭
     */
    private fun goToVote() {
        val themePick = currentState.themePick ?: return
        setEffect { ThemePickLiveContract.Effect.NavigateToVote(themePick.id) }
    }

    /**
     * 아이템 클릭 (커뮤니티 이동)
     */
    private fun onItemClick(idolId: Int?) {
        if (idolId != null) {
            setEffect { ThemePickLiveContract.Effect.NavigateToCommunity(idolId) }
        }
    }
}
