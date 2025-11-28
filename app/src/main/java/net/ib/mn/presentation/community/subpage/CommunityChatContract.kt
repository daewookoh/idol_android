package net.ib.mn.presentation.community.subpage

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.ChatRoomModel

/**
 * CommunityChatContract - 커뮤니티 채팅 탭 MVI Contract
 */
class CommunityChatContract {

    data class State(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val isLoadingMoreJoined: Boolean = false,
        val isLoadingMoreAll: Boolean = false,

        // 내가 참여한 채팅방
        val joinedRooms: List<ChatRoomModel> = emptyList(),
        val joinedTotalCount: Int = 0,
        val joinedNextUrl: String? = null,
        val joinedOrderBy: Int = ORDER_BY_RECENT,

        // 전체 채팅방
        val allRooms: List<ChatRoomModel> = emptyList(),
        val allTotalCount: Int = 0,
        val allNextUrl: String? = null,
        val allOrderBy: Int = ORDER_BY_RECENT,

        // 빈 상태
        val isEmpty: Boolean = false,

        // 에러
        val error: String? = null
    ) : UiState {
        companion object {
            const val ORDER_BY_RECENT = 0      // 최신순
            const val ORDER_BY_TALK_COUNT = 1  // 대화 많은 순
        }

        /**
         * 전체 채팅방에서 참여한 방을 제외한 목록
         */
        val filteredAllRooms: List<ChatRoomModel>
            get() {
                val joinedRoomIds = joinedRooms.map { it.roomId }.toSet()
                return allRooms.filter { it.roomId !in joinedRoomIds }
            }

        /**
         * 전체 채팅방 표시 개수 (참여방 제외)
         */
        val filteredAllTotalCount: Int
            get() = (allTotalCount - joinedTotalCount).coerceAtLeast(0)
    }

    sealed class Intent : UiIntent {
        data class LoadInitialData(val idolId: Int) : Intent()
        data class Refresh(val idolId: Int) : Intent()
        data class LoadMoreJoined(val idolId: Int) : Intent()
        data class LoadMoreAll(val idolId: Int) : Intent()
        data class JoinRoom(val roomId: Int, val room: ChatRoomModel) : Intent()
        data class LeaveRoom(val roomId: Int, val room: ChatRoomModel) : Intent()
        data class ChangeJoinedFilter(val orderBy: Int, val idolId: Int) : Intent()
        data class ChangeAllFilter(val orderBy: Int, val idolId: Int) : Intent()
        object CreateRoom : Intent()
    }

    sealed class Effect : UiEffect {
        data class ShowError(val message: String) : Effect()
        data class ShowToast(val message: String) : Effect()
        data class NavigateToChatRoom(
            val roomId: Int,
            val nickname: String?,
            val userId: Int?,
            val role: String?,
            val isAnonymity: Boolean,
            val title: String
        ) : Effect()
        object NavigateToCreateRoom : Effect()
        data class ShowLeaveConfirmDialog(val room: ChatRoomModel, val isOwner: Boolean) : Effect()
        object ShowLowLevelPopup : Effect()
        object ShowDifferentMostPopup : Effect()
    }
}
