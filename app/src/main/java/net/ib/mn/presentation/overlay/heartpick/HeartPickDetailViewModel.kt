package net.ib.mn.presentation.overlay.heartpick

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.R
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.HeartPickIdol
import net.ib.mn.domain.model.HeartPickModel
import net.ib.mn.domain.repository.HeartpickRepository
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.link.LinkUtil
import net.ib.mn.util.logE
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * HeartPickDetail (하트픽 상세) 화면 ViewModel
 *
 * old 프로젝트: HeartPickActivity, HeartPickPrelaunchActivity
 *
 * 세 가지 상태를 모두 대응:
 * - PRELAUNCH: 투표 예정
 * - VOTING: 투표 중
 * - VOTE_FINISHED: 투표 종료
 */
@HiltViewModel
class HeartPickDetailViewModel @Inject constructor(
    private val heartpickRepository: HeartpickRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : BaseViewModel<HeartPickDetailContract.State, HeartPickDetailContract.Intent, HeartPickDetailContract.Effect>() {

    private var timerJob: Job? = null
    private var currentHeartPickId: Int = 0

    override fun createInitialState(): HeartPickDetailContract.State = HeartPickDetailContract.State()

    override fun handleIntent(intent: HeartPickDetailContract.Intent) {
        when (intent) {
            is HeartPickDetailContract.Intent.LoadHeartPick -> loadHeartPick(intent.heartPickId)
            is HeartPickDetailContract.Intent.Share -> shareHeartPick()
            is HeartPickDetailContract.Intent.GoToCommunity -> goToCommunity(intent.idolId)
            is HeartPickDetailContract.Intent.GoToComment -> goToComment()
            is HeartPickDetailContract.Intent.Vote -> vote(intent.idol)
            is HeartPickDetailContract.Intent.ToggleNotification -> toggleNotification()
            is HeartPickDetailContract.Intent.ToggleRewardExpand -> toggleRewardExpand()
            is HeartPickDetailContract.Intent.UpdateCommentCount -> updateCommentCount(intent.newCount)
            is HeartPickDetailContract.Intent.IncrementCommentCount -> incrementCommentCount()
            is HeartPickDetailContract.Intent.DecrementCommentCount -> decrementCommentCount()
        }
    }

    private fun loadHeartPick(heartPickId: Int) {
        if (currentState.isLoading && currentState.heartPick != null) return

        currentHeartPickId = heartPickId
        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            heartpickRepository.getHeartPick(heartPickId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val heartPick = result.data

                        // 상태 결정
                        val status = when (heartPick.status) {
                            HeartPickModel.STATUS_PRELAUNCH -> HeartPickDetailContract.HeartPickStatus.PRELAUNCH
                            HeartPickModel.STATUS_VOTING -> HeartPickDetailContract.HeartPickStatus.VOTING
                            else -> HeartPickDetailContract.HeartPickStatus.VOTE_FINISHED
                        }

                        // 순위 및 득표차 계산 (투표중/종료 상태일 때만)
                        if (status != HeartPickDetailContract.HeartPickStatus.PRELAUNCH) {
                            calculateRankAndDiff(heartPick)
                        }

                        // D-Day/기간 계산
                        val dDayText = calculateDDayText(heartPick, status)
                        val periodText = calculatePeriodText(heartPick)

                        // 배너 URL에 secureUrl 적용
                        val secureBannerUrl = heartPick.bannerUrl.toSecureUrl()

                        // 나의 최애 아이돌 찾기
                        val mostIdolId = preferencesManager.getMostIdolId()
                        val myIdol = heartPick.heartPickIdols?.find { it.idolId == mostIdolId }
                        val myIdolPosition = if (myIdol != null) {
                            heartPick.heartPickIdols?.indexOf(myIdol) ?: -1
                        } else {
                            -1
                        }

                        setState {
                            copy(
                                heartPick = heartPick,
                                status = status,
                                dDayText = dDayText,
                                periodText = periodText,
                                secureBannerUrl = secureBannerUrl,
                                myIdol = myIdol,
                                myIdolPosition = myIdolPosition,
                                isLoading = false
                            )
                        }

                        // 카운트다운 타이머 시작 (투표중 상태일 때만)
                        if (status == HeartPickDetailContract.HeartPickStatus.VOTING) {
                            startCountdownIfNeeded(heartPick)
                        }

                        // 알림 설정 조회 (예정 상태일 때만)
                        if (status == HeartPickDetailContract.HeartPickStatus.PRELAUNCH) {
                            loadNotificationSetting(heartPickId)
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        setEffect {
                            HeartPickDetailContract.Effect.ShowToast(
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

    private fun loadNotificationSetting(heartPickId: Int) {
        // TODO: Repository에 getAlarmSetting 메소드 추가 필요
        // 현재는 기본값 false 유지
    }

    /**
     * 순위 및 득표차 계산
     */
    private fun calculateRankAndDiff(heartPick: HeartPickModel) {
        val idols = heartPick.heartPickIdols ?: return
        if (idols.size <= 1) {
            idols.firstOrNull()?.apply {
                rank = 1
                diffVote = 0
            }
            return
        }

        var currentRank = 1
        var previousVote = idols.firstOrNull()?.vote ?: 0
        var difference = 0

        for (index in idols.indices) {
            val idol = idols[index]
            if (idol.vote == previousVote) {
                idol.rank = currentRank
                idol.diffVote = difference
            } else {
                currentRank = index + 1
                idol.rank = currentRank
                difference = previousVote - idol.vote
                idol.diffVote = difference
                previousVote = idol.vote
            }
        }
    }

    /**
     * D-Day 텍스트 계산
     */
    private fun calculateDDayText(heartPick: HeartPickModel, status: HeartPickDetailContract.HeartPickStatus): String {
        return when (status) {
            HeartPickDetailContract.HeartPickStatus.PRELAUNCH -> {
                context.getString(R.string.upcoming)
            }
            HeartPickDetailContract.HeartPickStatus.VOTE_FINISHED -> {
                context.getString(R.string.vote_finish)
            }
            HeartPickDetailContract.HeartPickStatus.VOTING -> {
                try {
                    val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val now = Calendar.getInstance().time
                    val endDate = dateTimeFormat.parse(heartPick.endAt) ?: return "D-Day"

                    val diff = endDate.time - now.time
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
    private fun calculatePeriodText(heartPick: HeartPickModel): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

            val beginDate = inputFormat.parse(heartPick.beginAt)
            val endDate = inputFormat.parse(heartPick.endAt)

            if (beginDate != null && endDate != null) {
                "${outputFormat.format(beginDate)} ~ ${outputFormat.format(endDate)}"
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 카운트다운 타이머 시작 (24시간 미만일 때)
     */
    private fun startCountdownIfNeeded(heartPick: HeartPickModel) {
        timerJob?.cancel()

        try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val now = Calendar.getInstance().time
            val endDate = dateTimeFormat.parse(heartPick.endAt) ?: return

            val diff = endDate.time - now.time
            if (diff in 1 until 86400000) {
                startCountdownTimer(diff)
            }
        } catch (e: Exception) {
            logE("HeartPickDetailVM", "Failed to start countdown", e)
        }
    }

    private fun startCountdownTimer(initialDiff: Long) {
        timerJob = viewModelScope.launch(Dispatchers.IO) {
            var remainingTime = initialDiff
            while (remainingTime > 0) {
                val hours = remainingTime / 3600000
                val minutes = (remainingTime % 3600000) / 60000
                val seconds = (remainingTime % 60000) / 1000

                withContext(Dispatchers.Main) {
                    setState {
                        copy(dDayText = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds))
                    }
                }

                delay(1000)
                remainingTime -= 1000
            }

            // 타이머 종료 시 종료 상태로 변경
            withContext(Dispatchers.Main) {
                setState {
                    copy(
                        dDayText = context.getString(R.string.vote_finish),
                        status = HeartPickDetailContract.HeartPickStatus.VOTE_FINISHED
                    )
                }
            }
        }
    }

    /**
     * old 프로젝트 HeartPickViewModel.shareHeartPick() 과 동일한 포맷
     *
     * 포맷: 💖나의 최애픽! 하트픽💖
     *       {제목}
     *       [#{앱이름} #하트픽 순위]
     *
     *       1위 {이름}_{그룹}
     *       2위 {이름}_{그룹}
     *       3위 {이름}_{그룹}
     *
     *       내 최애에게 특별한 선물하러 가기💖
     *       {딥링크}
     */
    private fun shareHeartPick() {
        val heartPick = currentState.heartPick ?: return
        val idols = heartPick.heartPickIdols

        if (idols.isNullOrEmpty()) return

        val appName = context.getString(R.string.app_name)

        // 순위별 이름 구성 (이름_그룹 형식)
        val top1Name = idols.getOrNull(0)?.let { formatIdolName(it.title, it.subtitle) } ?: ""
        val top2Name = idols.getOrNull(1)?.let { formatIdolName(it.title, it.subtitle) } ?: ""
        val top3Name = idols.getOrNull(2)?.let { formatIdolName(it.title, it.subtitle) } ?: ""

        // 순위 텍스트 (1위, 2위, 3위)
        val rank1 = if (top1Name.isNotEmpty()) "1위" else ""
        val rank2 = if (top2Name.isNotEmpty()) "2위" else ""
        val rank3 = if (top3Name.isNotEmpty()) "3위" else ""

        // 딥링크 URL 생성 (old: LinkStatus.HEARTPICK.status = "heartpick")
        val url = LinkUtil.getAppLinkUrl(
            context = context,
            params = listOf("heartpick", heartPick.id.toString())
        )

        // 공유 메시지 구성
        val shareText = buildString {
            append("💖나의 최애픽! 하트픽💖\n")
            append(heartPick.title)
            append("\n")
            append("[#$appName #하트픽 순위]\n\n")

            if (rank1.isNotEmpty()) append("$rank1 $top1Name\n")
            if (rank2.isNotEmpty()) append("$rank2 $top2Name\n")
            if (rank3.isNotEmpty()) append("$rank3 $top3Name\n")

            append("\n내 최애에게 특별한 선물하러 가기💖\n")
            append(url)
        }.trim()

        setEffect { HeartPickDetailContract.Effect.ShareHeartPick(shareText) }
    }

    /**
     * 아이돌 이름 포맷 (이름_그룹 또는 이름만)
     */
    private fun formatIdolName(title: String, subtitle: String): String {
        return if (subtitle.isEmpty()) {
            title.replace("\\s+".toRegex(), "")
        } else {
            "${title}_${subtitle}".replace("\\s+".toRegex(), "")
        }
    }

    private fun goToCommunity(idolId: Int) {
        val idol = currentState.heartPick?.heartPickIdols?.find { it.idolId == idolId }
        val groupId = idol?.groupId ?: 0
        setEffect { HeartPickDetailContract.Effect.NavigateToCommunity(idolId, groupId) }
    }

    private fun goToComment() {
        val heartPickId = currentState.heartPick?.id ?: return
        setEffect { HeartPickDetailContract.Effect.NavigateToComment(heartPickId) }
    }

    private fun vote(idol: HeartPickIdol) {
        val heartPickId = currentState.heartPick?.id ?: return
        setEffect { HeartPickDetailContract.Effect.ShowVoteDialog(idol, heartPickId) }
    }

    private fun toggleNotification() {
        if (currentState.isNotifyEnabled) return

        // TODO: Repository에 postAlarmSetting 메소드 추가 필요
        // 현재는 로컬 상태만 변경
        setState { copy(isNotifyEnabled = true) }
        setEffect { HeartPickDetailContract.Effect.ShowNotifyEnabledToast }
    }

    private fun toggleRewardExpand() {
        setState { copy(isRewardExpanded = !isRewardExpanded) }
    }

    /**
     * 댓글 수 업데이트
     * CommentOnlyScreen에서 댓글 작성/삭제 후 호출
     */
    private fun updateCommentCount(newCount: Int) {
        val currentHeartPick = currentState.heartPick ?: return
        val updatedHeartPick = currentHeartPick.copy(numComments = newCount)
        setState { copy(heartPick = updatedHeartPick) }
    }

    /**
     * 댓글 수 1 증가 (댓글 작성 시)
     */
    private fun incrementCommentCount() {
        val currentHeartPick = currentState.heartPick ?: return
        val updatedHeartPick = currentHeartPick.copy(numComments = currentHeartPick.numComments + 1)
        setState { copy(heartPick = updatedHeartPick) }
    }

    /**
     * 댓글 수 1 감소 (댓글 삭제 시)
     */
    private fun decrementCommentCount() {
        val currentHeartPick = currentState.heartPick ?: return
        val newCount = maxOf(0, currentHeartPick.numComments - 1)
        val updatedHeartPick = currentHeartPick.copy(numComments = newCount)
        setState { copy(heartPick = updatedHeartPick) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
