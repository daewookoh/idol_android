package net.ib.mn.presentation.overlay.themepick

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ThemePickIdol
import net.ib.mn.domain.model.ThemePickModel
import net.ib.mn.domain.repository.ThemepickRepository
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.link.LinkUtil
import net.ib.mn.util.logE
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * ThemePickDetail (테마픽 상세) 화면 ViewModel
 *
 * old 프로젝트: ThemePickRankActivity, ThemePickResultActivity
 *
 * 세 가지 상태를 모두 대응:
 * - PREPARING: 투표 예정
 * - PROGRESS: 투표 중
 * - FINISHED: 투표 종료
 */
@HiltViewModel
class ThemePickDetailViewModel @Inject constructor(
    private val themepickRepository: ThemepickRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : BaseViewModel<ThemePickDetailContract.State, ThemePickDetailContract.Intent, ThemePickDetailContract.Effect>() {

    private var currentThemePickId: Int = 0
    private var votedIdolId: Int? = null  // 투표 후 결과 다이얼로그용

    override fun createInitialState(): ThemePickDetailContract.State = ThemePickDetailContract.State()

    override fun handleIntent(intent: ThemePickDetailContract.Intent) {
        when (intent) {
            is ThemePickDetailContract.Intent.LoadThemePick -> loadThemePick(intent.themePickId)
            is ThemePickDetailContract.Intent.Share -> shareThemePick()
            is ThemePickDetailContract.Intent.SelectCandidate -> selectCandidate(intent.candidate)
            is ThemePickDetailContract.Intent.DeselectCandidate -> deselectCandidate()
            is ThemePickDetailContract.Intent.Vote -> vote()
            is ThemePickDetailContract.Intent.GoToResult -> goToResult()
            is ThemePickDetailContract.Intent.ToggleNotification -> toggleNotification()
            is ThemePickDetailContract.Intent.ToggleRewardExpand -> toggleRewardExpand()
            is ThemePickDetailContract.Intent.RefreshAfterVote -> refreshAfterVote()
            is ThemePickDetailContract.Intent.VoteAfterAd -> voteAfterAd()
        }
    }

    private fun loadThemePick(themePickId: Int) {
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
                            ThemePickDetailContract.Effect.ShowToast(
                                result.message ?: context.getString(R.string.desc_failed_to_connect_internet)
                            )
                        }
                    }
                    is ApiResult.Loading -> {
                        // 이미 isLoading = true 설정됨
                    }
                }
            }
        }
    }

    private fun processThemePickData(themePick: ThemePickModel, isRefreshForVote: Boolean = false) {
        // 상태 결정
        val status = when (themePick.status) {
            ThemePickModel.STATUS_PREPARING -> ThemePickDetailContract.ThemePickStatus.PREPARING
            ThemePickModel.STATUS_PROGRESS -> ThemePickDetailContract.ThemePickStatus.PROGRESS
            else -> ThemePickDetailContract.ThemePickStatus.FINISHED
        }

        // 후보 정렬 및 순위 계산
        val processedThemePick = if (status != ThemePickDetailContract.ThemePickStatus.PREPARING) {
            sortAndRankCandidates(themePick)
        } else {
            // 예정 상태에서는 랜덤 셔플
            themePick.copy(candidates = themePick.candidates?.let { ArrayList(it.shuffled()) })
        }

        // D-Day/기간 계산
        val dDayText = calculateDDayText(processedThemePick, status)
        val periodText = calculatePeriodText(processedThemePick)

        // 이미지 URL에 secureUrl 적용
        val secureImageUrl = processedThemePick.imageUrl.toSecureUrl()

        setState {
            copy(
                themePick = processedThemePick,
                status = status,
                dDayText = dDayText,
                periodText = periodText,
                secureImageUrl = secureImageUrl,
                isNotifyEnabled = processedThemePick.alarm,
                canVote = processedThemePick.canVote(),
                needsVideoAd = processedThemePick.needsVideoAd(),
                hasVotedToday = processedThemePick.hasVotedToday(),
                isLoading = false,
                selectedCandidate = null  // 로드 시 선택 초기화
            )
        }

        // 투표 후 새로고침이면 결과 다이얼로그 표시
        if (isRefreshForVote && votedIdolId != null) {
            showVoteCompleteDialog(processedThemePick)
        }
    }

    /**
     * 후보 정렬 및 순위 계산
     * old 프로젝트: ThemePickRankActivity.loadRank()의 정렬 로직
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
     * D-Day 텍스트 계산
     */
    private fun calculateDDayText(themePick: ThemePickModel, status: ThemePickDetailContract.ThemePickStatus): String {
        return when (status) {
            ThemePickDetailContract.ThemePickStatus.PREPARING -> {
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val beginDate = dateFormat.parse(themePick.beginAt) ?: return context.getString(R.string.upcoming)
                    val now = System.currentTimeMillis()
                    val diff = beginDate.time - now
                    val days = diff / (1000 * 60 * 60 * 24)
                    context.getString(R.string.vote_dday, days.toInt())
                } catch (e: Exception) {
                    context.getString(R.string.upcoming)
                }
            }
            ThemePickDetailContract.ThemePickStatus.FINISHED -> {
                context.getString(R.string.vote_finish)
            }
            ThemePickDetailContract.ThemePickStatus.PROGRESS -> {
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val expiredDate = dateFormat.parse(themePick.expiredAt) ?: return "D-Day"
                    val now = System.currentTimeMillis()
                    val diff = expiredDate.time - now

                    when {
                        diff < 0 -> context.getString(R.string.vote_finish)
                        diff < 86400000 -> {
                            val hours = diff / 3600000
                            val minutes = (diff % 3600000) / 60000
                            val seconds = (diff % 60000) / 1000
                            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                        }
                        else -> "D-${diff / 86400000}"
                    }
                } catch (e: Exception) {
                    "D-Day"
                }
            }
        }
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

        val shareText = when (currentState.status) {
            ThemePickDetailContract.ThemePickStatus.PREPARING -> {
                shareThemePickPrelaunch(themePick, url)
            }
            else -> {
                shareThemePickResult(themePick, url)
            }
        }

        setEffect { ThemePickDetailContract.Effect.ShareThemePick(shareText) }
    }

    /**
     * 예정 상태 공유 포맷
     */
    private fun shareThemePickPrelaunch(themePick: ThemePickModel, url: String): String {
        val prizeName = themePick.prize?.name.orEmpty()

        // TODO: 최애 아이돌 정보 가져오기
        val mostText = context.getString(R.string.share_onepick_upcoming_nobias)
        val groupText = ""

        return context.getString(
            R.string.share_themepick_upcoming,
            themePick.title,
            prizeName,
            mostText,
            groupText,
            url
        ).trimNewlineWhiteSpace()
    }

    /**
     * 진행중/종료 상태 공유 포맷
     */
    private fun shareThemePickResult(themePick: ThemePickModel, url: String): String {
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
     * 후보 선택
     */
    private fun selectCandidate(candidate: ThemePickIdol) {
        // 이미 선택된 후보를 다시 클릭하면 선택 해제
        if (currentState.selectedCandidate?.id == candidate.id) {
            deselectCandidate()
            return
        }

        setState { copy(selectedCandidate = candidate) }
    }

    /**
     * 후보 선택 해제
     */
    private fun deselectCandidate() {
        setState { copy(selectedCandidate = null) }
    }

    /**
     * 투표하기
     */
    private fun vote() {
        val themePick = currentState.themePick ?: return
        val selectedCandidate = currentState.selectedCandidate ?: return

        // 투표 상태 체크
        when {
            currentState.hasVotedToday -> {
                setEffect { ThemePickDetailContract.Effect.ShowAlreadyVotedDialog }
                return
            }
            themePick.isFinished() -> {
                setEffect { ThemePickDetailContract.Effect.ShowToast(context.getString(R.string.gaon_final_guide)) }
                return
            }
            currentState.needsVideoAd -> {
                // 광고 시청 필요
                setEffect { ThemePickDetailContract.Effect.ShowVideoAd }
                return
            }
        }

        // 투표 실행 - selectedCandidate.id 사용 (ThemePickIdol.id = 후보 ID, themepick_idol_id로 전송)
        executeVote(themePick.id, selectedCandidate.id, themePick.vote)
    }

    /**
     * 실제 투표 API 호출
     */
    private fun executeVote(themePickId: Int, idolId: Int, voteType: String) {
        setState { copy(isVoting = true) }
        votedIdolId = idolId

        viewModelScope.launch {
            themepickRepository.vote(themePickId, idolId, voteType).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        if (result.data) {
                            // 투표 성공 - 0.5초 후 데이터 새로고침
                            kotlinx.coroutines.delay(500)
                            refreshAfterVote()
                        } else {
                            setState { copy(isVoting = false) }
                            setEffect { ThemePickDetailContract.Effect.ShowToast(context.getString(R.string.error_abnormal_exception)) }
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isVoting = false) }
                        setEffect {
                            ThemePickDetailContract.Effect.ShowToast(
                                result.message ?: context.getString(R.string.desc_failed_to_connect_internet)
                            )
                        }
                    }
                    is ApiResult.Loading -> {
                        // 이미 isVoting = true 설정됨
                    }
                }
            }
        }
    }

    /**
     * 투표 후 데이터 새로고침
     */
    private fun refreshAfterVote() {
        viewModelScope.launch {
            themepickRepository.getThemePick(currentThemePickId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        processThemePickData(result.data, isRefreshForVote = true)
                        setState { copy(isVoting = false) }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isVoting = false) }
                        logE("ThemePickDetailVM", "Failed to refresh after vote: ${result.message}")
                    }
                    is ApiResult.Loading -> {
                        // 로딩 중
                    }
                }
            }
        }
    }

    /**
     * 투표 완료 다이얼로그 표시
     */
    private fun showVoteCompleteDialog(themePick: ThemePickModel) {
        val votedCandidate = themePick.candidates?.find { it.id == votedIdolId } ?: return

        // 아이돌/노래 타입에 따른 이름 포맷
        val name = if (themePick.type == "I") {
            if (votedCandidate.subtitle.isEmpty()) {
                votedCandidate.title
            } else {
                "${votedCandidate.title}_${votedCandidate.subtitle}"
            }
        } else {
            if (votedCandidate.subtitle.isEmpty()) {
                votedCandidate.title
            } else {
                "${votedCandidate.title} - ${votedCandidate.subtitle}"
            }
        }

        // 1위와의 득표차 계산
        val firstPlaceVote = themePick.candidates?.firstOrNull()?.vote ?: 0L
        val voteGap = firstPlaceVote - votedCandidate.vote

        setEffect {
            ThemePickDetailContract.Effect.ShowVoteCompleteDialog(
                candidateName = name,
                rank = votedCandidate.rank,
                voteGapFromFirst = voteGap
            )
        }

        votedIdolId = null  // 다이얼로그 표시 후 초기화
    }

    /**
     * 현재 순위 보기
     */
    private fun goToResult() {
        val themePick = currentState.themePick ?: return

        if (themePick.count == 0) {
            // 참여자 없음
            setEffect { ThemePickDetailContract.Effect.ShowNoParticipantsDialog }
        } else {
            setEffect { ThemePickDetailContract.Effect.NavigateToResult(themePick.id) }
        }
    }

    /**
     * 알림 설정 토글
     */
    private fun toggleNotification() {
        if (currentState.isNotifyEnabled) return

        val themePickId = currentState.themePick?.id ?: return

        viewModelScope.launch {
            themepickRepository.postOpenNotification(themePickId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        if (result.data) {
                            setState { copy(isNotifyEnabled = true) }
                            setEffect { ThemePickDetailContract.Effect.ShowNotifyEnabledToast }
                        }
                    }
                    is ApiResult.Error -> {
                        setEffect {
                            ThemePickDetailContract.Effect.ShowToast(
                                result.message ?: context.getString(R.string.desc_failed_to_connect_internet)
                            )
                        }
                    }
                    is ApiResult.Loading -> {
                        // 로딩 중
                    }
                }
            }
        }
    }

    /**
     * 리워드 펼치기/접기 토글
     */
    private fun toggleRewardExpand() {
        setState { copy(isRewardExpanded = !isRewardExpanded) }
    }

    /**
     * 광고 시청 완료 후 투표
     * old 프로젝트: ThemePickRankActivity.videoAdLauncher 콜백
     */
    private fun voteAfterAd() {
        val themePick = currentState.themePick ?: return
        val selectedCandidate = currentState.selectedCandidate ?: return

        // 광고 시청 후 투표 실행 - voteType은 "see_videoad" (V)
        // selectedCandidate.id 사용 (ThemePickIdol.id = 후보 ID, themepick_idol_id로 전송)
        executeVote(
            themePick.id,
            selectedCandidate.id,
            ThemePickModel.VOTE_SEE_VIDEOAD
        )
    }
}
