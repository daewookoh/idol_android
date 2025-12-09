package net.ib.mn.presentation.community.chat.room

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.data.remote.socket.ChatSocketManager
import net.ib.mn.domain.model.ChatMemberModel
import net.ib.mn.domain.model.ChatMessageModel
import net.ib.mn.domain.model.ChatRoomInfoModel

/**
 * ChatRoomContract - 채팅방 메인 화면 MVI Contract
 */
class ChatRoomContract {

    data class State(
        // 채팅방 정보
        val roomId: Int = 0,
        val roomTitle: String = "",
        val isAnonymous: Boolean = false,
        val roomInfo: ChatRoomInfoModel? = null,

        // 유저 정보
        val userId: Int = 0,
        val userNickname: String = "",
        val userRole: String = "",

        // 연결 상태
        val connectionState: ChatSocketManager.ConnectionState = ChatSocketManager.ConnectionState.DISCONNECTED,
        val isConnecting: Boolean = false,
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,

        // 메시지 목록
        val messages: List<ChatMessageModel> = emptyList(),
        val hasMoreMessages: Boolean = true,

        // 입력
        val inputText: String = "",
        val isSending: Boolean = false,

        // UI 상태
        val showScrollToBottom: Boolean = false,
        val unreadCount: Int = 0,
        val showInfoDrawer: Boolean = false,
        val showEmoticonSheet: Boolean = false,
        val isUploadingImage: Boolean = false,

        // 멤버 목록 (info drawer용)
        val members: List<ChatMemberModel> = emptyList(),

        // CDN URL (이모티콘 URL 생성용)
        val cdnUrl: String = "",

        // 신고 여부 (채팅방)
        val isReportedRoom: Boolean = false,

        // 에러
        val error: String? = null
    ) : UiState {

        /**
         * 전송 버튼 활성화 여부
         */
        val canSend: Boolean
            get() = inputText.isNotBlank() && !isSending && connectionState == ChatSocketManager.ConnectionState.AUTH_COMPLETE

        /**
         * 연결됨 여부
         */
        val isConnected: Boolean
            get() = connectionState == ChatSocketManager.ConnectionState.AUTH_COMPLETE

        /**
         * 방장 여부
         */
        val isOwner: Boolean
            get() = userRole == "O"

        /**
         * 관리자 여부
         */
        val isAdmin: Boolean
            get() = userRole == "A"
    }

    sealed class Intent : UiIntent {
        // 초기화
        data class Initialize(
            val roomId: Int,
            val nickname: String?,
            val userId: Int?,
            val role: String?,
            val isAnonymity: Boolean,
            val title: String
        ) : Intent()

        // 연결
        object Connect : Intent()
        object Disconnect : Intent()
        object Reconnect : Intent()

        // 메시지
        data class UpdateInputText(val text: String) : Intent()
        object SendMessage : Intent()
        object LoadMoreMessages : Intent()
        data class DeleteMessage(val message: ChatMessageModel) : Intent()
        data class ReportMessage(val message: ChatMessageModel) : Intent()
        data class CopyMessage(val message: ChatMessageModel) : Intent()

        // UI
        object ScrollToBottom : Intent()
        data class UpdateScrollPosition(val isAtBottom: Boolean, val shouldShowButton: Boolean) : Intent()
        object ToggleInfoDrawer : Intent()
        object CloseInfoDrawer : Intent()
        object ShowEmoticonSheet : Intent()
        object HideEmoticonSheet : Intent()
        data class SendEmoticon(val emoticonId: Int) : Intent()
        object ShowGallery : Intent()
        data class SendImage(val imageBytes: ByteArray) : Intent() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as SendImage
                return imageBytes.contentEquals(other.imageBytes)
            }
            override fun hashCode(): Int = imageBytes.contentHashCode()
        }

        // 채팅방 관리
        object LeaveRoom : Intent()
        object DeleteRoom : Intent()
        object ReportRoom : Intent()
    }

    sealed class Effect : UiEffect {
        data class ShowError(val message: String) : Effect()
        object ScrollToBottom : Effect()
        object NavigateBack : Effect()
        object NavigateBackWithRefresh : Effect()
        data class ShowDeleteConfirmDialog(val message: ChatMessageModel) : Effect()
        data class ShowReportConfirmDialog(val message: ChatMessageModel) : Effect()
        object ShowLeaveConfirmDialog : Effect()
        object ShowDeleteRoomConfirmDialog : Effect()
        data class ShowReportRoomConfirmDialog(val reportHeart: Int) : Effect()
        data class ShowReportRoomAlreadyReported(val message: String) : Effect()
        data class CopyToClipboard(val text: String) : Effect()
        data class ShowImage(val imageUrl: String) : Effect()
        object ShowGalleryPicker : Effect()
    }
}
