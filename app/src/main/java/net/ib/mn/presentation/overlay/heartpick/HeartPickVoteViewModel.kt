package net.ib.mn.presentation.overlay.heartpick

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.remote.dto.HeartPickVoteResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.HeartpickRepository
import javax.inject.Inject

/**
 * 하트픽 투표 ViewModel
 *
 * old 프로젝트의 HeartPickVoteDialogViewModel과 동일
 *
 * 일반 아이돌 투표(VoteViewModel)와 다른 점:
 * - heartpick/vote/ 엔드포인트 사용
 * - heartpick_id, heartpick_idol_id 파라미터 전달
 * - 응답: bonus_heart, voted 필드
 */
@HiltViewModel
class HeartPickVoteViewModel @Inject constructor(
    private val heartpickRepository: HeartpickRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    // 사용자 총 하트 (에버하트 + 데일리하트)
    var totalHeart by mutableLongStateOf(0L)
        private set

    // 사용자 데일리하트 (무료 하트)
    var freeHeart by mutableLongStateOf(0L)
        private set

    /**
     * 사용자 하트 정보 로드
     */
    fun loadUserHearts(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val userInfo = preferencesManager.userInfo.first()
            if (userInfo != null) {
                val strong = userInfo.strongHeart ?: 0L
                val weak = userInfo.weakHeart ?: 0L
                totalHeart = strong + weak
                freeHeart = weak
            } else {
                totalHeart = 0L
                freeHeart = 0L
            }
            onComplete()
        }
    }

    /**
     * 하트픽 투표
     *
     * @param heartPickId 하트픽 ID
     * @param heartPickIdolId 하트픽 아이돌 ID (HeartPickIdol.id)
     * @param heart 투표할 하트 개수
     * @param onSuccess 투표 성공 시 콜백
     * @param onError 투표 실패 시 콜백
     */
    suspend fun voteHeartPick(
        heartPickId: Int,
        heartPickIdolId: Int,
        heart: Long,
        onSuccess: (HeartPickVoteResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        if (heart <= 0) {
            onError("투표할 하트 개수를 입력해주세요")
            return
        }

        if (heart > totalHeart) {
            onError("보유한 하트보다 많이 투표할 수 없습니다")
            return
        }

        heartpickRepository.voteHeartPick(
            heartPickId = heartPickId,
            heartPickIdolId = heartPickIdolId,
            number = heart
        ).collect { result ->
            when (result) {
                is ApiResult.Success -> {
                    // 투표 성공 후 하트 차감
                    totalHeart -= heart
                    if (freeHeart > 0) {
                        val usedFreeHeart = minOf(heart, freeHeart)
                        freeHeart -= usedFreeHeart
                    }

                    val newStrongHeart = totalHeart - freeHeart

                    // DataStore 캐시 업데이트
                    preferencesManager.updateUserHearts(newStrongHeart, freeHeart)

                    onSuccess(result.data)
                }
                is ApiResult.Error -> {
                    onError(result.message ?: "투표에 실패했습니다")
                }
                is ApiResult.Loading -> {
                }
            }
        }
    }
}
