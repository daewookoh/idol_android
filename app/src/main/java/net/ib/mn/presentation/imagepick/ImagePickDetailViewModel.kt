package net.ib.mn.presentation.imagepick

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.base.BaseViewModel
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ImagePickIdolModel
import net.ib.mn.domain.model.ImagePickModel
import net.ib.mn.domain.repository.ImagepickRepository
import net.ib.mn.util.DateTimeUtil
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * ImagePickDetail ViewModel
 *
 * old 프로젝트: OnepickMatchActivity, OnepickResultActivity
 *
 * 토너먼트 형식의 이미지픽 투표 로직을 관리합니다.
 * - 예선: 3x3 그리드에서 이미지 선택 → 라운드 진행
 * - 결승: 예선 통과자들 중에서 최종 선택
 * - 투표 제출: API로 투표 결과 전송
 */
@HiltViewModel
class ImagePickDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imagepickRepository: ImagepickRepository
) : BaseViewModel<ImagePickDetailContract.State, ImagePickDetailContract.Intent, ImagePickDetailContract.Effect>() {

    override fun createInitialState(): ImagePickDetailContract.State = ImagePickDetailContract.State()

    private var imagePickId: Int = -1
    private var allCandidates: List<ImagePickIdolModel> = emptyList()
    private var qualifyingRounds: List<List<ImagePickIdolModel>> = emptyList()

    override fun handleIntent(intent: ImagePickDetailContract.Intent) {
        when (intent) {
            is ImagePickDetailContract.Intent.LoadImagePick -> loadImagePick(intent.imagePickId)
            is ImagePickDetailContract.Intent.ToggleNotification -> toggleNotification()
            is ImagePickDetailContract.Intent.StartVote -> startVote()
            is ImagePickDetailContract.Intent.SelectImage -> selectImage(intent.candidate)
            is ImagePickDetailContract.Intent.SubmitVote -> submitVote()
            is ImagePickDetailContract.Intent.VoteAfterAd -> voteAfterAd()
            is ImagePickDetailContract.Intent.Share -> share()
            is ImagePickDetailContract.Intent.GoToResult -> goToResult()
        }
    }

    private fun loadImagePick(id: Int) {
        imagePickId = id
        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            // getImagePickResult API를 사용하여 기본 정보와 후보 목록을 함께 로드
            imagepickRepository.getImagePickResult(id).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val data = result.data

                        // 상태 결정
                        val status = when (data.status) {
                            ImagePickModel.STATUS_PREPARING -> ImagePickDetailContract.ImagePickStatus.PREPARING
                            ImagePickModel.STATUS_PROGRESS -> ImagePickDetailContract.ImagePickStatus.PROGRESS
                            else -> ImagePickDetailContract.ImagePickStatus.FINISHED
                        }

                        val periodText = DateTimeUtil.formatPeriodSpaced(data.createdAt, data.expiredAt)

                        // 투표 상태 파싱
                        val canVote = data.vote == "N"
                        val needsVideoAd = data.vote == "V"
                        val hasVotedToday = data.vote == "Y"

                        // ImagePickModel 생성 (공유 등에서 사용)
                        val imagePick = ImagePickModel(
                            title = data.title ?: "",
                            subtitle = data.subtitle ?: "",
                            description = "",
                            status = data.status ?: ImagePickModel.STATUS_PROGRESS,
                            vote = data.vote,
                            count = data.count ?: 0,
                            createdAt = data.createdAt ?: "",
                            expiredAt = data.expiredAt ?: "",
                            hashTag = "",
                            resourceUri = "/onepick/$id/",
                            voteType = "",
                            alarm = data.alarm
                        )

                        allCandidates = data.candidates
                        val dimension = data.dimension

                        // 토너먼트 라운드 준비
                        if (status != ImagePickDetailContract.ImagePickStatus.PREPARING) {
                            prepareTournamentRounds(allCandidates, dimension)
                        }

                        setState {
                            copy(
                                imagePick = imagePick,
                                status = status,
                                periodText = periodText,
                                canVote = canVote,
                                needsVideoAd = needsVideoAd,
                                hasVotedToday = hasVotedToday,
                                voteType = data.vote,
                                isNotifyEnabled = data.alarm,
                                candidates = data.candidates,
                                date = data.date,
                                dimension = dimension,
                                isLoading = false
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                    }
                    is ApiResult.Loading -> {
                        // Loading 상태는 이미 설정됨
                    }
                }
            }
        }
    }

    /**
     * 토너먼트 라운드 준비
     * 후보들을 dimension x dimension 그리드로 나누어 라운드 구성
     */
    private fun prepareTournamentRounds(candidates: List<ImagePickIdolModel>, dimension: Int) {
        val sizeOfMatch = dimension * dimension
        val totalCandidates = candidates.size
        val remainder = totalCandidates % sizeOfMatch

        val totalRounds = if (remainder == 0) {
            totalCandidates / sizeOfMatch
        } else {
            (totalCandidates / sizeOfMatch) + 1
        }

        // 라운드별로 후보 분배
        val rounds = mutableListOf<List<ImagePickIdolModel>>()
        var index = 0

        for (round in 0 until totalRounds) {
            val roundCandidates = mutableListOf<ImagePickIdolModel>()
            for (i in 0 until sizeOfMatch) {
                if (index < totalCandidates) {
                    roundCandidates.add(candidates[index])
                    index++
                } else {
                    // 빈 후보 추가 (그리드 채우기용)
                    roundCandidates.add(ImagePickIdolModel(0, null, null, 0, 0))
                }
            }
            rounds.add(roundCandidates)
        }

        qualifyingRounds = rounds

        // 첫 번째 라운드 설정
        if (rounds.isNotEmpty()) {
            setState {
                copy(
                    currentRoundCandidates = rounds[0],
                    totalRounds = totalRounds,
                    currentRoundIndex = 0,
                    tournamentRound = ImagePickDetailContract.TournamentRound.QUALIFYING
                )
            }
        }
    }

    private fun toggleNotification() {
        if (uiState.value.isNotifyEnabled) return

        viewModelScope.launch {
            imagepickRepository.setImagePickAlarm(imagePickId).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        setState { copy(isNotifyEnabled = true) }
                        setEffect { ImagePickDetailContract.Effect.ShowNotifyEnabledToast }
                    }
                    is ApiResult.Error -> {
                        setEffect { ImagePickDetailContract.Effect.ShowToast(result.message ?: "") }
                    }
                    is ApiResult.Loading -> { }
                }
            }
        }
    }

    private fun startVote() {
        val state = uiState.value

        when {
            state.hasVotedToday -> {
                setEffect { ImagePickDetailContract.Effect.ShowAlreadyVotedDialog }
            }
            state.needsVideoAd -> {
                setEffect { ImagePickDetailContract.Effect.ShowVideoAd }
            }
            else -> {
                // 투표 시작 - 첫 번째 라운드로
                if (qualifyingRounds.isNotEmpty()) {
                    setState {
                        copy(
                            currentRoundCandidates = qualifyingRounds[0],
                            currentRoundIndex = 0,
                            selectedPicks = emptyList(),
                            qualifyingWinners = emptyList(),
                            tournamentRound = ImagePickDetailContract.TournamentRound.QUALIFYING
                        )
                    }
                }
            }
        }
    }

    /**
     * 이미지 선택 (토너먼트에서)
     */
    private fun selectImage(candidate: ImagePickIdolModel) {
        if (candidate.isEmpty) return

        val state = uiState.value
        val currentPicks = state.selectedPicks.toMutableList()
        currentPicks.add(candidate.idol?.id ?: candidate.id)

        val winners = state.qualifyingWinners.toMutableList()
        winners.add(candidate)

        val nextRoundIndex = state.currentRoundIndex + 1

        when {
            // 예선 진행 중
            state.tournamentRound == ImagePickDetailContract.TournamentRound.QUALIFYING &&
                    nextRoundIndex < state.totalRounds -> {
                // 다음 예선 라운드로
                setState {
                    copy(
                        currentRoundCandidates = qualifyingRounds[nextRoundIndex],
                        currentRoundIndex = nextRoundIndex,
                        selectedPicks = currentPicks,
                        qualifyingWinners = winners
                    )
                }
            }
            // 예선 종료 → 결승으로
            state.tournamentRound == ImagePickDetailContract.TournamentRound.QUALIFYING &&
                    nextRoundIndex >= state.totalRounds -> {
                // 결승 라운드 준비
                val finalCandidates = prepareFinalRound(winners)
                setState {
                    copy(
                        tournamentRound = ImagePickDetailContract.TournamentRound.FINAL,
                        currentRoundCandidates = finalCandidates,
                        currentRoundIndex = 0,
                        selectedPicks = currentPicks,
                        qualifyingWinners = winners
                    )
                }
            }
            // 결승에서 최종 선택
            state.tournamentRound == ImagePickDetailContract.TournamentRound.FINAL -> {
                // 투표 제출
                setState { copy(selectedPicks = currentPicks) }
                submitVote()
            }
        }
    }

    /**
     * 결승 라운드 준비
     */
    private fun prepareFinalRound(winners: List<ImagePickIdolModel>): List<ImagePickIdolModel> {
        val finalDimension = ceil(sqrt(winners.size.toDouble())).toInt()
        val finalGridSize = finalDimension * finalDimension

        val finalCandidates = winners.toMutableList()

        // 그리드 채우기
        while (finalCandidates.size < finalGridSize) {
            finalCandidates.add(ImagePickIdolModel(0, null, null, 0, 0))
        }

        // dimension 업데이트
        setState { copy(dimension = finalDimension) }

        return finalCandidates
    }

    private fun submitVote() {
        val state = uiState.value
        val voteIds = state.selectedPicks.joinToString(",")

        if (voteIds.isEmpty()) {
            setEffect { ImagePickDetailContract.Effect.ShowToast(context.getString(R.string.error_abnormal_exception)) }
            return
        }

        setState { copy(isVoting = true) }

        viewModelScope.launch {
            imagepickRepository.voteImagePick(
                id = imagePickId,
                voteIds = voteIds,
                voteType = state.voteType
            ).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        setState { copy(isVoting = false, hasVotedToday = true) }

                        // 마지막으로 선택한 후보 정보
                        val finalPick = state.qualifyingWinners.lastOrNull()
                        val candidateName = finalPick?.idol?.name ?: ""

                        setEffect {
                            ImagePickDetailContract.Effect.ShowVoteCompleteDialog(
                                candidateName = candidateName,
                                rank = 1  // 내 선택이므로 항상 1위로 표시
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isVoting = false) }
                        setEffect { ImagePickDetailContract.Effect.ShowToast(result.message ?: "") }
                    }
                    is ApiResult.Loading -> { }
                }
            }
        }
    }

    private fun voteAfterAd() {
        // 광고 시청 후 투표 타입 변경
        setState { copy(voteType = "V", needsVideoAd = false) }
        startVote()
    }

    private fun share() {
        val imagePick = uiState.value.imagePick ?: return

        val shareText = "[${imagePick.title}]"

        setEffect { ImagePickDetailContract.Effect.ShareImagePick(shareText) }
    }

    private fun goToResult() {
        val candidates = uiState.value.candidates
        if (candidates.isEmpty() || candidates.all { it.voteCount == 0L }) {
            setEffect { ImagePickDetailContract.Effect.ShowNoParticipantsDialog }
        } else {
            setEffect { ImagePickDetailContract.Effect.NavigateToResult(imagePickId) }
        }
    }
}
