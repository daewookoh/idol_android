package net.ib.mn.presentation.friend.request

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.FriendModel

/**
 * FriendRequest 화면의 MVI Contract.
 *
 * old 프로젝트의 FriendsRequestActivity + AcceptFriendsRequestFragment + CancelFriendsRequestFragment 로직을 MVI 패턴으로 구현.
 * - 받은 친구 요청 목록 (requesters) - 수락/거절
 * - 보낸 친구 요청 목록 (waiters) - 취소
 */
class FriendRequestContract {

    /**
     * UI State for FriendRequest screen.
     *
     * @property requesters 나한테 친구 요청을 한 사람들 (받은 요청)
     * @property waiters 내가 친구 요청을 한 사람들 (보낸 요청)
     * @property isLoading 로딩 중 여부
     * @property error 에러 메시지
     * @property selectedTab 현재 선택된 탭 (0: 받은 요청, 1: 보낸 요청)
     */
    data class State(
        val requesters: List<FriendModel> = emptyList(),
        val waiters: List<FriendModel> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedTab: Int = 1  // 기본 탭: 보낸 요청 (2번째 탭)
    ) : UiState

    /**
     * User intents
     */
    sealed class Intent : UiIntent {
        /**
         * 화면 진입 시 데이터 로드
         */
        data object LoadData : Intent()

        /**
         * 탭 변경
         */
        data class SelectTab(val tabIndex: Int) : Intent()

        /**
         * 친구 요청 수락
         */
        data class AcceptRequest(val userId: Int) : Intent()

        /**
         * 친구 요청 거절
         */
        data class DeclineRequest(val userId: Int) : Intent()

        /**
         * 모든 친구 요청 수락
         */
        data object AcceptAllRequests : Intent()

        /**
         * 보낸 친구 요청 취소
         */
        data class CancelRequest(val userId: Int) : Intent()
    }

    /**
     * Side effects
     */
    sealed class Effect : UiEffect {
        /**
         * 토스트 메시지 표시
         */
        data class ShowToast(val message: String) : Effect()

        /**
         * 다이얼로그 표시
         */
        data class ShowDialog(val title: String?, val message: String) : Effect()

        /**
         * 결과 반환 및 화면 종료
         */
        data class FinishWithResult(val resultCode: Int) : Effect()
    }
}
