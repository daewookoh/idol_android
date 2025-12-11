package net.ib.mn.presentation.friend.delete

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.FriendModel
import java.util.Date

/**
 * FriendDelete (친구 삭제) 화면의 MVI Contract.
 *
 * old 프로젝트의 FriendDeleteActivity 로직을 MVI 패턴으로 구현.
 * - 친구 목록 조회
 * - 정렬 기능 (이름순, 로그인 시간순, 하트순)
 * - 선택한 친구 삭제
 */
class FriendDeleteContract {

    /**
     * 정렬 기준
     */
    enum class SortType {
        LOGIN_TIME,  // 로그인 시간순 (기본)
        NAME,        // 이름순
        HEART        // 하트순
    }

    /**
     * UI State for FriendDelete screen.
     *
     * @property friends 친구 목록
     * @property selectedIds 삭제 선택된 친구 ID 목록
     * @property sortType 현재 정렬 기준
     * @property isLoading 로딩 중 여부
     * @property isDeleting 삭제 중 여부
     * @property error 에러 메시지
     */
    data class State(
        val friends: List<FriendDeleteItem> = emptyList(),
        val selectedIds: Set<Int> = emptySet(),
        val sortType: SortType = SortType.LOGIN_TIME,
        val isLoading: Boolean = false,
        val isDeleting: Boolean = false,
        val error: String? = null
    ) : UiState

    /**
     * 친구 삭제용 아이템 (giveHeart 포함)
     */
    data class FriendDeleteItem(
        val friendModel: FriendModel,
        val giveHeart: Int = 0,
        val lastAct: Date? = null
    )

    /**
     * User intents (비즈니스 로직 관련 액션만).
     */
    sealed class Intent : UiIntent {
        /**
         * 화면 진입 시 초기 데이터 로드
         */
        data object LoadFriends : Intent()

        /**
         * 친구 선택/해제 토글
         */
        data class ToggleSelection(val userId: Int) : Intent()

        /**
         * 정렬 기준 변경
         */
        data class ChangeSortType(val sortType: SortType) : Intent()

        /**
         * 선택한 친구 삭제 요청
         */
        data object DeleteSelectedFriends : Intent()

        /**
         * 삭제 확인 후 실제 삭제 실행
         */
        data object ConfirmDelete : Intent()
    }

    /**
     * Side effects (일회성 이벤트).
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
         * 삭제 확인 다이얼로그 표시
         */
        data object ShowDeleteConfirmDialog : Effect()

        /**
         * 삭제 완료 다이얼로그 표시
         */
        data object ShowDeleteCompleteDialog : Effect()
    }
}
