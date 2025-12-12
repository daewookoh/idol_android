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
import net.ib.mn.util.RankingUtil
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
    private var tooltipTimerJob: Job? = null
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
            is HeartPickDetailContract.Intent.UpdateIdolVote -> updateIdolVote(intent.idolId, intent.addedVote)
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
                        val processedHeartPick = if (status != HeartPickDetailContract.HeartPickStatus.PRELAUNCH) {
                            RankingUtil.sortAndRankHeartPickModel(heartPick)
                        } else {
                            heartPick
                        }

                        // D-Day/기간 계산
                        val dDayText = calculateDDayText(processedHeartPick, status)
                        val periodText = calculatePeriodText(processedHeartPick)

                        // 배너 URL에 secureUrl 적용
                        val secureBannerUrl = processedHeartPick.bannerUrl.toSecureUrl()

                        // 나의 최애 아이돌 찾기
                        val mostIdolId = preferencesManager.getMostIdolId()
                        val myIdol = processedHeartPick.heartPickIdols?.find { it.idolId == mostIdolId }
                        val myIdolPosition = if (myIdol != null) {
                            processedHeartPick.heartPickIdols?.indexOf(myIdol) ?: -1
                        } else {
                            -1
                        }

                        setState {
                            copy(
                                heartPick = processedHeartPick,
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
                            startCountdownIfNeeded(processedHeartPick)
                        }

                        // 알림 설정 조회 (예정 상태일 때만)
                        if (status == HeartPickDetailContract.HeartPickStatus.PRELAUNCH) {
                            loadNotificationSetting(heartPickId)
                        }

                        // 툴팁 타이머 시작 (투표중/종료 상태일 때, 3초 후 숨김)
                        if (status != HeartPickDetailContract.HeartPickStatus.PRELAUNCH) {
                            startTooltipTimer()
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
        viewModelScope.launch {
            heartpickRepository.getOpenHeartPickNotification(heartPickId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        setState { copy(isNotifyEnabled = result.data) }
                    }
                    is ApiResult.Error -> {
                        // 에러 시 기본값 false 유지
                        logE("HeartPickDetailVM", "Failed to load notification setting: ${result.message}")
                    }
                    is ApiResult.Loading -> {
                        // 로딩 중
                    }
                }
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
     * 툴팁 타이머 시작 (3초 후 숨김)
     * old 프로젝트: HeartPickActivity.onTimer()
     */
    private fun startTooltipTimer() {
        tooltipTimerJob?.cancel()
        tooltipTimerJob = viewModelScope.launch {
            delay(3000)
            setState { copy(showTooltip = false) }
        }
    }

    /**
     * 하트픽 공유하기
     *
     * - PRELAUNCH (개설 예정): old HeartPickPrelaunchActivity.shareHeartPick()과 동일
     *   포맷: R.string.share_heartpick_upcoming
     *   💖하트로 사랑을 보내는 하트픽 예고💖
     *   [{title}]
     *   🎁[{prizeName}]
     *   과연 1위를 차지할 주인공은 누구? 👀
     *
     *   📌 지금 바로 {mostName} {groupName} 응원 준비하기
     *   🔗{url}
     *
     * - VOTING/VOTE_FINISHED: old HeartPickViewModel.shareHeartPick()과 동일
     *   포맷: 💖나의 최애픽! 하트픽💖 ...
     */
    private fun shareHeartPick() {
        val heartPick = currentState.heartPick ?: return
        val idols = heartPick.heartPickIdols

        if (idols.isNullOrEmpty()) return

        // 딥링크 URL 생성
        val url = LinkUtil.getAppLinkUrl(
            context = context,
            params = listOf("heartpick", heartPick.id.toString())
        )

        val shareText = if (currentState.status == HeartPickDetailContract.HeartPickStatus.PRELAUNCH) {
            // 개설 예정 공유 포맷 (old HeartPickPrelaunchActivity와 동일)
            shareHeartPickPrelaunch(heartPick, url)
        } else {
            // 투표중/종료 공유 포맷 (old HeartPickViewModel과 동일)
            shareHeartPickVoting(heartPick, url)
        }

        setEffect { HeartPickDetailContract.Effect.ShareHeartPick(shareText) }
    }

    /**
     * 개설 예정 공유 포맷 (old HeartPickPrelaunchActivity.shareHeartPick()과 동일)
     */
    private fun shareHeartPickPrelaunch(heartPick: HeartPickModel, url: String): String {
        // myIdol은 이미 loadHeartPick()에서 state에 저장되어 있음
        val existMost = currentState.myIdol
        val mostText = existMost?.title
            ?: context.getString(R.string.share_onepick_upcoming_nobias)
        val groupText = existMost?.subtitle.orEmpty()

        // prize 이름이 null이면 빈 문자열
        val prizeName = heartPick.prize?.name.orEmpty()

        return context.getString(
            R.string.share_heartpick_upcoming,
            heartPick.title,
            prizeName,
            mostText,
            groupText,
            url
        ).trimNewlineWhiteSpace()
    }

    /**
     * 투표중/종료 공유 포맷 (old HeartPickViewModel.shareHeartPick()과 동일)
     *
     * R.string.heartpick_share_msg 사용:
     * 💖My Pick! Heart Pick💖
     * %s (title)
     * [#%s (appName) #HeartPick Ranking]
     *
     * %s (rank1) %s (name1)
     * %s (rank2) %s (name2)
     * %s (rank3) %s (name3)
     *
     * Give a special gift to your bias💖
     */
    private fun shareHeartPickVoting(heartPick: HeartPickModel, url: String): String {
        val idols = heartPick.heartPickIdols ?: return ""
        val appName = context.getString(R.string.app_name)

        // 순위별 이름 구성 (이름_그룹 형식)
        val top1Name = idols.getOrNull(0)?.let { formatIdolName(it.title, it.subtitle) }.orEmpty()
        val top2Name = idols.getOrNull(1)?.let { formatIdolName(it.title, it.subtitle) }.orEmpty()
        val top3Name = idols.getOrNull(2)?.let { formatIdolName(it.title, it.subtitle) }.orEmpty()

        // 순위 텍스트 (rank_format: "%s위" / "%s")
        val rank1 = if (top1Name.isNotEmpty()) context.getString(R.string.rank_format, "1") else ""
        val rank2 = if (top2Name.isNotEmpty()) context.getString(R.string.rank_format, "2") else ""
        val rank3 = if (top3Name.isNotEmpty()) context.getString(R.string.rank_format, "3") else ""

        val msg = context.getString(
            R.string.heartpick_share_msg,
            heartPick.title,
            appName,
            rank1, top1Name,
            rank2, top2Name,
            rank3, top3Name
        )

        return (msg + url).trimNewlineWhiteSpace()
    }

    /**
     * 연속된 줄바꿈과 공백 정리
     */
    private fun String.trimNewlineWhiteSpace(): String {
        return this.replace(Regex("\\n\\s*\\n\\s*\\n+"), "\n\n").trim()
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

        val heartPickId = currentState.heartPick?.id ?: return

        viewModelScope.launch {
            heartpickRepository.postOpenHeartPickNotification(heartPickId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        setState { copy(isNotifyEnabled = true) }
                        setEffect { HeartPickDetailContract.Effect.ShowNotifyEnabledToast }
                    }
                    is ApiResult.Error -> {
                        setEffect {
                            HeartPickDetailContract.Effect.ShowToast(
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

    /**
     * 특정 아이돌의 투표수 업데이트 (투표 성공 시 실시간 반영)
     *
     * DB 데이터를 건드리지 않고 UI에서만 투표수를 업데이트합니다.
     *
     * @param idolId 아이돌 ID
     * @param addedVote 추가된 투표수
     */
    private fun updateIdolVote(idolId: Int, addedVote: Int) {
        val currentHeartPick = currentState.heartPick ?: return
        val currentIdols = currentHeartPick.heartPickIdols ?: return

        // 해당 아이돌의 투표수 업데이트
        val updatedIdols = ArrayList(currentIdols.map { idol ->
            if (idol.idolId == idolId) {
                idol.copy(vote = idol.vote + addedVote)
            } else {
                idol
            }
        })

        // RankingUtil을 사용하여 정렬 및 순위 계산
        val updatedHeartPick = RankingUtil.sortAndRankHeartPickModel(
            currentHeartPick.copy(
                heartPickIdols = updatedIdols,
                vote = currentHeartPick.vote + addedVote
            )
        )

        // 나의 최애 아이돌 위치 재계산
        val sortedIdols = updatedHeartPick.heartPickIdols
        val myIdol = sortedIdols?.find { it.idolId == currentState.myIdol?.idolId }
        val myIdolPosition = if (myIdol != null) {
            sortedIdols?.indexOf(myIdol) ?: -1
        } else {
            -1
        }

        setState {
            copy(
                heartPick = updatedHeartPick,
                myIdol = myIdol,
                myIdolPosition = myIdolPosition
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        tooltipTimerJob?.cancel()
    }
}
