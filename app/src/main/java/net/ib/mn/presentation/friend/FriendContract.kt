package net.ib.mn.presentation.friend

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.FriendModel

/**
 * Friend 화면의 MVI Contract.
 *
 * old 프로젝트의 FriendsActivity + FriendsViewModel 로직을 MVI 패턴으로 구현.
 * - 친구 목록 조회
 * - 친구 요청자 목록 조회
 * - 하트 보내기/받기
 * - 친구 요청 수락/거절
 * - 친구 초대
 *
 * Navigation 3 활용:
 * - 네비게이션은 Screen에서 LocalAppNavigator를 통해 직접 처리
 * - ViewModel은 비즈니스 로직(상태 관리, API 호출)에만 집중
 */
class FriendContract {

    /**
     * UI State for Friend screen.
     *
     * @property friends 친구 목록
     * @property requesters 친구 요청자 목록 (나한테 친구 요청을 한 사람들)
     * @property isLoading 로딩 중 여부
     * @property isRefreshing 새로고침 중 여부
     * @property error 에러 메시지
     * @property sendingHeartIds 하트 보내는 중인 친구 ID 목록
     */
    data class State(
        val friends: List<FriendModel> = emptyList(),
        val requesters: List<FriendModel> = emptyList(),
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val sendingHeartIds: Set<Int> = emptySet()
    ) : UiState

    /**
     * User intents (비즈니스 로직 관련 액션만).
     * 네비게이션은 Screen에서 LocalAppNavigator로 직접 처리.
     */
    sealed class Intent : UiIntent {
        /**
         * 화면 진입 시 친구 목록 로드
         */
        data object LoadFriends : Intent()

        /**
         * 새로고침
         */
        data object Refresh : Intent()

        /**
         * 친구에게 하트 보내기
         */
        data class SendHeart(val friendId: Int, val nickname: String) : Intent()

        /**
         * 모든 친구에게 하트 보내기
         */
        data object SendHeartToAll : Intent()

        /**
         * 하트 받기
         */
        data object ReceiveHeart : Intent()

        /**
         * 친구 요청 수락
         */
        data class AcceptFriendRequest(val userId: Int) : Intent()

        /**
         * 친구 요청 거절
         */
        data class DeclineFriendRequest(val userId: Int) : Intent()

        /**
         * 모든 친구 요청 수락
         */
        data object AcceptAllFriendRequests : Intent()

        /**
         * 친구 초대
         */
        data object Invite : Intent()
    }

    /**
     * Side effects (일회성 이벤트).
     * 네비게이션은 Screen에서 직접 처리하므로 여기서는 UI 관련 이벤트만.
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
         * 친구 초대 화면으로 이동
         */
        data class NavigateToInvite(val language: String, val token: String) : Effect()
    }
}
