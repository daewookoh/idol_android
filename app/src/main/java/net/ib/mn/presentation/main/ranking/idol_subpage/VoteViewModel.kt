package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.data.remote.dto.VoteResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.usecase.GetUserSelfUseCase
import net.ib.mn.domain.usecase.VoteIdolUseCase
import javax.inject.Inject

/**
 * 투표 ViewModel
 *
 * 투표 관련 비즈니스 로직 처리:
 * 1. 사용자 하트 정보 로드
 * 2. 아이돌 하트 투표
 * 3. 투표 성공/실패 처리
 */
@HiltViewModel
class VoteViewModel @Inject constructor(
    private val getUserSelfUseCase: GetUserSelfUseCase,
    private val voteIdolUseCase: VoteIdolUseCase,
    private val preferencesManager: net.ib.mn.data.local.PreferencesManager
) : ViewModel() {

    // 사용자 총 하트 (에버하트 + 데일리하트)
    var totalHeart by mutableLongStateOf(0L)
        private set

    // 사용자 데일리하트 (무료 하트)
    var freeHeart by mutableLongStateOf(0L)
        private set

    /**
     * 사용자 하트 정보 로드
     *
     * old 프로젝트의 VoteDialogFragment onCreate에서 하트 정보 가져오는 로직과 동일
     * 캐시된 데이터를 사용 (old 프로젝트와 동일)
     */
    fun loadUserHearts(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            getUserSelfUseCase().collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val user = result.data.objects.firstOrNull()
                        if (user != null) {
                            val strong = user.strongHeart ?: 0L
                            val weak = user.weakHeart ?: 0L
                            totalHeart = strong + weak
                            freeHeart = weak

                        } else {
                            totalHeart = 0L
                            freeHeart = 0L
                        }

                        onComplete()
                    }
                    is ApiResult.Error -> {
                        // HTTP 304 (캐시 유효) 처리
                        if (result.code == 304 && result.data != null) {
                            val userInfo = result.data as? net.ib.mn.data.local.UserInfo
                            if (userInfo != null) {
                                val strong = userInfo.strongHeart ?: 0L
                                val weak = userInfo.weakHeart ?: 0L
                                totalHeart = strong + weak
                                freeHeart = weak

                            } else {
                                totalHeart = 0L
                                freeHeart = 0L
                            }
                        } else {
                            totalHeart = 0L
                            freeHeart = 0L
                        }
                        onComplete()
                    }
                    is ApiResult.Loading -> {
                    }
                }
            }
        }
    }

    /**
     * 아이돌 하트 투표
     *
     * old 프로젝트의 VoteDialogFragment doVote() 메서드와 동일
     *
     * @param idolId 아이돌 ID
     * @param heart 투표할 하트 개수
     * @param onSuccess 투표 성공 시 콜백
     * @param onError 투표 실패 시 콜백
     */
    suspend fun voteIdol(
        idolId: Int,
        heart: Long,
        onSuccess: (VoteResponse) -> Unit,
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


        voteIdolUseCase(idolId, heart).collect { result ->
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
