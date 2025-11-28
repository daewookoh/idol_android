package net.ib.mn.presentation.community.subpage

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ChatRoomModel
import net.ib.mn.domain.repository.ChatRepository
import net.ib.mn.util.LocaleUtil
import javax.inject.Inject

private const val CHAT_MIN_LEVEL = 5
private const val PAGE_SIZE = 30

@HiltViewModel
class CommunityChatViewModel @Inject constructor(
    application: Application,
    private val chatRepository: ChatRepository,
    private val preferencesManager: PreferencesManager
) : BaseViewModel<CommunityChatContract.State, CommunityChatContract.Intent, CommunityChatContract.Effect>() {

    private var joinedOffset = 0
    private var allOffset = 0
    private var currentIdolId = 0
    private val locale = LocaleUtil.getSystemLanguage(application).split("_")[0]

    override fun createInitialState() = CommunityChatContract.State()

    override fun handleIntent(intent: CommunityChatContract.Intent) {
        when (intent) {
            is CommunityChatContract.Intent.LoadInitialData -> loadInitialData(intent.idolId)
            is CommunityChatContract.Intent.Refresh -> refresh(intent.idolId)
            is CommunityChatContract.Intent.LoadMoreJoined -> loadMoreJoined(intent.idolId)
            is CommunityChatContract.Intent.LoadMoreAll -> loadMoreAll(intent.idolId)
            is CommunityChatContract.Intent.JoinRoom -> joinRoom(intent.roomId, intent.room)
            is CommunityChatContract.Intent.LeaveRoom -> confirmLeaveRoom(intent.room)
            is CommunityChatContract.Intent.ChangeJoinedFilter -> changeJoinedFilter(intent.orderBy, intent.idolId)
            is CommunityChatContract.Intent.ChangeAllFilter -> changeAllFilter(intent.orderBy, intent.idolId)
            is CommunityChatContract.Intent.CreateRoom -> handleCreateRoom()
        }
    }

    private fun loadInitialData(idolId: Int) {
        currentIdolId = idolId
        resetOffsets()
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            loadJoinedRooms(idolId, isInitial = true)
        }
    }

    private fun refresh(idolId: Int) {
        currentIdolId = idolId
        resetOffsets()
        viewModelScope.launch {
            setState { copy(isRefreshing = true, error = null) }
            loadJoinedRooms(idolId, isInitial = true)
        }
    }

    private fun resetOffsets() {
        joinedOffset = 0
        allOffset = 0
    }

    private fun loadJoinedRooms(idolId: Int, isInitial: Boolean) {
        viewModelScope.launch {
            chatRepository.getJoinedChatRooms(
                idolId = idolId,
                locale = locale,
                orderBy = uiState.value.joinedOrderBy,
                limit = PAGE_SIZE,
                offset = joinedOffset
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> Unit
                    is ApiResult.Success -> {
                        val response = result.data
                        val newRooms = if (isInitial) response.rooms else uiState.value.joinedRooms + response.rooms

                        setState {
                            copy(
                                joinedRooms = newRooms,
                                joinedTotalCount = response.totalCount,
                                joinedNextUrl = response.nextUrl,
                                isLoadingMoreJoined = false
                            )
                        }

                        if (isInitial || response.nextUrl == null) {
                            loadAllRooms(idolId, isInitial = true)
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false, isRefreshing = false, isLoadingMoreJoined = false, error = result.message) }
                        setEffect { CommunityChatContract.Effect.ShowError(result.message ?: "Failed to load") }
                    }
                }
            }
        }
    }

    private fun loadAllRooms(idolId: Int, isInitial: Boolean) {
        viewModelScope.launch {
            chatRepository.getAllChatRooms(
                idolId = idolId,
                locale = locale,
                orderBy = uiState.value.allOrderBy,
                limit = PAGE_SIZE,
                offset = allOffset
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> Unit
                    is ApiResult.Success -> {
                        val response = result.data
                        val newRooms = if (isInitial) response.rooms else uiState.value.allRooms + response.rooms
                        val isEmpty = uiState.value.joinedRooms.isEmpty() && newRooms.isEmpty()

                        setState {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                isLoadingMoreAll = false,
                                allRooms = newRooms,
                                allTotalCount = response.totalCount,
                                allNextUrl = response.nextUrl,
                                isEmpty = isEmpty
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false, isRefreshing = false, isLoadingMoreAll = false, error = result.message) }
                        setEffect { CommunityChatContract.Effect.ShowError(result.message ?: "Failed to load") }
                    }
                }
            }
        }
    }

    private fun loadMoreJoined(idolId: Int) {
        if (uiState.value.isLoadingMoreJoined || uiState.value.joinedNextUrl == null) return
        joinedOffset += PAGE_SIZE
        setState { copy(isLoadingMoreJoined = true) }
        loadJoinedRooms(idolId, isInitial = false)
    }

    private fun loadMoreAll(idolId: Int) {
        if (uiState.value.isLoadingMoreAll || uiState.value.allNextUrl == null) return
        allOffset += PAGE_SIZE
        setState { copy(isLoadingMoreAll = true) }
        loadAllRooms(idolId, isInitial = false)
    }

    private fun joinRoom(roomId: Int, room: ChatRoomModel) {
        viewModelScope.launch {
            chatRepository.joinChatRoom(roomId).collect { result ->
                when (result) {
                    is ApiResult.Loading -> setState { copy(isLoading = true) }
                    is ApiResult.Success -> {
                        setState { copy(isLoading = false) }
                        val response = result.data
                        if (response.success) {
                            setEffect {
                                CommunityChatContract.Effect.NavigateToChatRoom(
                                    roomId = roomId,
                                    nickname = response.nickname,
                                    userId = response.userId,
                                    role = room.role,
                                    isAnonymity = room.isAnonymity,
                                    title = room.title
                                )
                            }
                        } else {
                            setEffect { CommunityChatContract.Effect.ShowError(response.message ?: "Failed to join") }
                            refresh(currentIdolId)
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false) }
                        setEffect { CommunityChatContract.Effect.ShowError(result.message ?: "Failed to join") }
                    }
                }
            }
        }
    }

    private fun confirmLeaveRoom(room: ChatRoomModel) {
        setEffect { CommunityChatContract.Effect.ShowLeaveConfirmDialog(room = room, isOwner = room.isOwner) }
    }

    fun leaveRoom(roomId: Int) {
        viewModelScope.launch {
            chatRepository.leaveChatRoom(roomId).collect { result ->
                when (result) {
                    is ApiResult.Loading -> setState { copy(isLoading = true) }
                    is ApiResult.Success -> {
                        setState { copy(isLoading = false) }
                        if (result.data.success) {
                            setEffect { CommunityChatContract.Effect.ShowToast("채팅방을 나갔습니다.") }
                            refresh(currentIdolId)
                        } else {
                            setEffect { CommunityChatContract.Effect.ShowError(result.data.message ?: "Failed to leave") }
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false) }
                        setEffect { CommunityChatContract.Effect.ShowError(result.message ?: "Failed to leave") }
                    }
                }
            }
        }
    }

    private fun changeJoinedFilter(orderBy: Int, idolId: Int) {
        resetOffsets()
        setState { copy(joinedOrderBy = orderBy, joinedRooms = emptyList(), allRooms = emptyList()) }
        loadJoinedRooms(idolId, isInitial = true)
    }

    private fun changeAllFilter(orderBy: Int, idolId: Int) {
        allOffset = 0
        setState { copy(allOrderBy = orderBy, allRooms = emptyList()) }
        loadAllRooms(idolId, isInitial = true)
    }

    private fun handleCreateRoom() {
        viewModelScope.launch {
            val userLevel = preferencesManager.getUserLevel()
            val mostIdolId = preferencesManager.getMostIdolId()

            when {
                userLevel < CHAT_MIN_LEVEL -> setEffect { CommunityChatContract.Effect.ShowLowLevelPopup }
                mostIdolId != currentIdolId -> setEffect { CommunityChatContract.Effect.ShowDifferentMostPopup }
                else -> setEffect { CommunityChatContract.Effect.NavigateToCreateRoom }
            }
        }
    }
}
