package net.ib.mn.presentation.community.subpage

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.ChatDatabase
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.dao.ChatRoomDao
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ChatRoomModel
import net.ib.mn.domain.repository.ChatRepository
import net.ib.mn.util.LocaleUtil
import javax.inject.Inject

private const val CHAT_MIN_LEVEL = 5
private const val PAGE_SIZE = 30

@HiltViewModel
class CommunityChatViewModel @Inject constructor(
    private val application: Application,
    private val chatRepository: ChatRepository,
    private val preferencesManager: PreferencesManager
) : BaseViewModel<CommunityChatContract.State, CommunityChatContract.Intent, CommunityChatContract.Effect>() {

    private var joinedOffset = 0
    private var allOffset = 0
    private var currentIdolId = 0
    private val locale = LocaleUtil.getSystemLanguage(application).split("_")[0]

    private var chatRoomDao: ChatRoomDao? = null

    override fun createInitialState() = CommunityChatContract.State()

    private suspend fun getChatRoomDao(): ChatRoomDao {
        if (chatRoomDao == null) {
            val accountId = preferencesManager.getUserIdSync()
            if (accountId > 0) {
                chatRoomDao = ChatDatabase.getInstance(application, accountId).chatRoomDao()
            }
        }
        return chatRoomDao!!
    }

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

            // 로컬 DB에서 먼저 로드 (빠른 표시)
            try {
                val dao = getChatRoomDao()
                val cachedJoinedRooms = withContext(Dispatchers.IO) {
                    dao.getJoinedRoomsByIdolId(idolId)
                }
                if (cachedJoinedRooms.isNotEmpty()) {
                    setState {
                        copy(
                            joinedRooms = cachedJoinedRooms,
                            joinedTotalCount = cachedJoinedRooms.size,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                // 로컬 DB 오류는 무시하고 API에서 로드
            }

            // API에서 최신 데이터 로드
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

                        // 로컬 DB에 캐싱
                        cacheRoomsToLocalDb(response.rooms, isJoinedList = true)

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

    private fun cacheRoomsToLocalDb(rooms: List<ChatRoomModel>, isJoinedList: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = getChatRoomDao()
                dao.upsertRooms(rooms, isJoinedList)
            } catch (e: Exception) {
                // 캐싱 오류는 무시
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

                        // 로컬 DB에 캐싱 (전체 채팅방은 isJoinedList = false)
                        cacheRoomsToLocalDb(response.rooms, isJoinedList = false)

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
        // 익명방이든 일반방이든 join API를 호출해서 userId를 받아야 함
        // (이미 참여한 방이라도 userId 응답을 받을 수 있음)
        viewModelScope.launch {
            chatRepository.joinChatRoom(roomId).collect { result ->
                when (result) {
                    is ApiResult.Loading -> setState { copy(isLoading = true) }
                    is ApiResult.Success -> {
                        setState { copy(isLoading = false) }
                        val response = result.data

                        // userId 결정: API 응답에서 받은 값 > room에 저장된 값 > null
                        // (old 프로젝트: 이미 참여한 방은 chatRoomListModel.userId 사용)
                        val effectiveUserId = when {
                            response.userId != null && response.userId != 0 -> response.userId
                            room.userId != 0 -> room.userId
                            else -> null
                        }

                        if (response.success) {
                            // 로컬 DB에서 joined 상태로 표시
                            markRoomAsJoinedInLocalDb(roomId)
                        }

                        // success 여부와 관계없이 입장 허용 (이미 참여한 채팅방일 수 있음)
                        setEffect {
                            CommunityChatContract.Effect.NavigateToChatRoom(
                                roomId = roomId,
                                nickname = response.nickname ?: room.nickName,
                                userId = effectiveUserId,
                                role = room.role,
                                isAnonymity = room.isAnonymousRoom,
                                title = room.title
                            )
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

    private fun markRoomAsJoinedInLocalDb(roomId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = getChatRoomDao()
                dao.markAsJoined(roomId)
            } catch (e: Exception) {
                // 오류 무시
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
                            // 로컬 DB에서 left 상태로 표시
                            markRoomAsLeftInLocalDb(roomId)

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

    private fun markRoomAsLeftInLocalDb(roomId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = getChatRoomDao()
                dao.markAsLeft(roomId)
            } catch (e: Exception) {
                // 오류 무시
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
