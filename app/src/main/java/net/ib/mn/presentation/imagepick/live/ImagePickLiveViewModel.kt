package net.ib.mn.presentation.imagepick.live

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
import net.ib.mn.util.link.LinkUtil
import javax.inject.Inject

/**
 * ImagePickLive ViewModel
 *
 * old 프로젝트: OnepickResultActivity
 *
 * 이미지픽 투표 결과/순위를 관리합니다.
 */
@HiltViewModel
class ImagePickLiveViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imagepickRepository: ImagepickRepository
) : BaseViewModel<ImagePickLiveContract.State, ImagePickLiveContract.Intent, ImagePickLiveContract.Effect>() {

    override fun createInitialState(): ImagePickLiveContract.State = ImagePickLiveContract.State()

    private var imagePickId: Int = -1

    override fun handleIntent(intent: ImagePickLiveContract.Intent) {
        when (intent) {
            is ImagePickLiveContract.Intent.LoadResult -> loadResult(intent.imagePickId)
            is ImagePickLiveContract.Intent.Share -> share()
            is ImagePickLiveContract.Intent.GoToVote -> goToVote()
            is ImagePickLiveContract.Intent.OnItemClick -> onItemClick(intent.idolId)
        }
    }

    private fun loadResult(id: Int) {
        imagePickId = id
        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            // getImagePickResult API를 사용하여 기본 정보와 순위 데이터를 함께 로드
            imagepickRepository.getImagePickResult(id).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val data = result.data

                        val periodText = DateTimeUtil.formatPeriodSpaced(data.createdAt, data.expiredAt)

                        // 투표 상태 파싱
                        val canVote = data.vote == "N"
                        val needsVideoAd = data.vote == "V"
                        val hasVotedToday = data.vote == "Y"
                        val isFinished = data.status == ImagePickModel.STATUS_FINISHED

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

                        // 투표수가 있는 후보만 필터링하고 정렬
                        val rankedItems = data.candidates
                            .filter { it.voteCount > 0 }
                            .sortedByDescending { it.voteCount }
                            .toMutableList()

                        // 순위 할당
                        assignRanks(rankedItems)

                        setState {
                            copy(
                                imagePick = imagePick,
                                periodText = periodText,
                                canVote = canVote,
                                needsVideoAd = needsVideoAd,
                                hasVotedToday = hasVotedToday,
                                isFinished = isFinished,
                                rankItems = rankedItems,
                                date = data.date,
                                isLoading = false
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                    }
                    is ApiResult.Loading -> { }
                }
            }
        }
    }

    /**
     * 순위 할당
     * 동점자 처리 포함
     */
    private fun assignRanks(items: MutableList<ImagePickIdolModel>) {
        if (items.isEmpty()) return

        var currentRank = 1
        val firstPlaceVote = items.first().voteCount
        val lastPlaceVote = items.last().voteCount

        for (i in items.indices) {
            val item = items[i]

            item.rank = when {
                i == 0 -> currentRank
                items[i - 1].voteCount == item.voteCount -> items[i - 1].rank
                else -> i + 1
            }

            item.firstPlaceVoteCount = firstPlaceVote
            item.lastPlaceVoteCount = lastPlaceVote
        }
    }

    private fun share() {
        val state = uiState.value
        val imagePick = state.imagePick ?: return
        val rankItems = state.rankItems

        // 딥링크 URL 생성
        val url = LinkUtil.getAppLinkUrl(
            context = context,
            params = listOf("onepick", imagePickId.toString())
        )

        val shareText = shareImagePickLive(imagePick, rankItems, url)
        setEffect { ImagePickLiveContract.Effect.ShareImagePick(shareText) }
    }

    /**
     * 진행중/종료 상태 공유 포맷
     * ThemePickResultViewModel과 동일한 형식
     */
    private fun shareImagePickLive(
        imagePick: ImagePickModel,
        rankItems: List<ImagePickIdolModel>,
        url: String
    ): String {
        val top1Name = rankItems.getOrNull(0)?.let { formatCandidateName(it) }.orEmpty()
        val top2Name = rankItems.getOrNull(1)?.let { formatCandidateName(it) }.orEmpty()
        val top3Name = rankItems.getOrNull(2)?.let { formatCandidateName(it) }.orEmpty()

        val rank1 = if (top1Name.isNotEmpty()) context.getString(R.string.rank_format, "1") else ""
        val rank2 = if (top2Name.isNotEmpty()) context.getString(R.string.rank_format, "2") else ""
        val rank3 = if (top3Name.isNotEmpty()) context.getString(R.string.rank_format, "3") else ""

        val msg = context.getString(
            R.string.share_image_pick,
            imagePick.title,
            rank1, "#$top1Name",
            rank2, "#$top2Name",
            rank3, "#$top3Name",
            url
        )

        return msg.trimNewlineWhiteSpace()
    }

    private fun formatCandidateName(candidate: ImagePickIdolModel): String {
        val name = candidate.idol?.name ?: return ""
        return name.replace("\\s+".toRegex(), "")
    }

    private fun String.trimNewlineWhiteSpace(): String {
        return this.replace(Regex("\\n\\s*\\n\\s*\\n+"), "\n\n").trim()
    }

    private fun goToVote() {
        val state = uiState.value

        when {
            state.hasVotedToday -> {
                setEffect { ImagePickLiveContract.Effect.ShowToast(context.getString(R.string.onepick_already_voted)) }
            }
            state.needsVideoAd -> {
                setEffect { ImagePickLiveContract.Effect.ShowVideoAd }
            }
            else -> {
                setEffect { ImagePickLiveContract.Effect.NavigateToVote(imagePickId) }
            }
        }
    }

    fun voteAfterAd() {
        setEffect { ImagePickLiveContract.Effect.NavigateToVote(imagePickId) }
    }

    private fun onItemClick(idolId: Int?) {
        if (idolId != null && idolId > 0) {
            setEffect { ImagePickLiveContract.Effect.NavigateToCommunity(idolId) }
        }
    }
}
