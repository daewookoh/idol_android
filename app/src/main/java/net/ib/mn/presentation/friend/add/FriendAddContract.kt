package net.ib.mn.presentation.friend.add

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.FriendUser

/**
 * FriendAdd (뉴프렌즈) 화면의 MVI Contract.
 *
 * old 프로젝트의 NewFriendsActivity 로직을 MVI 패턴으로 구현.
 * - 뉴프렌즈 신청/취소
 * - 뉴프렌즈 추천 목록 조회
 * - 친구 요청 보내기
 *
 * Navigation 3 활용:
 * - 네비게이션은 Screen에서 LocalAppNavigator를 통해 직접 처리
 * - ViewModel은 비즈니스 로직(상태 관리, API 호출)에만 집중
 */
class FriendAddContract {

    /**
     * UI State for FriendAdd screen.
     *
     * @property recommendedUsers 뉴프렌즈 추천 목록
     * @property isLoading 로딩 중 여부
     * @property isNewFriendsApplied 뉴프렌즈 신청 여부 (true: 신청됨, false: 미신청)
     * @property isAllowFriendReq 친구 요청 허용 여부
     * @property myUserId 본인 유저 ID (목록에서 본인 제외용)
     * @property sendingRequestIds 친구 요청 보내는 중인 유저 ID 목록
     * @property error 에러 메시지
     */
    data class State(
        val recommendedUsers: List<FriendUser> = emptyList(),
        val isLoading: Boolean = false,
        val isNewFriendsApplied: Boolean = false,
        val isAllowFriendReq: Boolean = true,
        val myUserId: Int = 0,
        val sendingRequestIds: Set<Int> = emptySet(),
        val error: String? = null
    ) : UiState

    /**
     * User intents (비즈니스 로직 관련 액션만).
     * 네비게이션은 Screen에서 LocalAppNavigator로 직접 처리.
     */
    sealed class Intent : UiIntent {
        /**
         * 화면 진입 시 초기 데이터 로드
         */
        data object LoadInitialData : Intent()

        /**
         * 뉴프렌즈 추천 목록 새로고침
         */
        data object RefreshRecommendedUsers : Intent()

        /**
         * 뉴프렌즈 신청/취소 토글
         */
        data object ToggleNewFriendsApply : Intent()

        /**
         * 친구 요청 보내기
         */
        data class SendFriendRequest(val userId: Int) : Intent()
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
    }
}
