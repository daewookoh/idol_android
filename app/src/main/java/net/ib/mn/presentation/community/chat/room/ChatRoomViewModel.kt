package net.ib.mn.presentation.community.chat.room

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.ChatDatabase
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.local.dao.ChatMessageDao
import net.ib.mn.data.remote.socket.ChatSocketManager
import net.ib.mn.data.remote.socket.SocketEvent
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ChatMessageModel
import net.ib.mn.domain.repository.ChatRepository
import net.ib.mn.R
import net.ib.mn.util.IdolImageUtil
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.logD
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

private const val MESSAGE_PAGE_SIZE = 30

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val application: Application,
    private val chatRepository: ChatRepository,
    private val preferencesManager: PreferencesManager,
    private val configRepository: net.ib.mn.domain.repository.ConfigRepository
) : BaseViewModel<ChatRoomContract.State, ChatRoomContract.Intent, ChatRoomContract.Effect>() {

    private var socketManager: ChatSocketManager? = null
    private var chatMessageDao: ChatMessageDao? = null
    private var chatMemberDao: net.ib.mn.data.local.dao.ChatMemberDao? = null
    private var accountId: Int = 0

    override fun createInitialState() = ChatRoomContract.State()

    override fun handleIntent(intent: ChatRoomContract.Intent) {
        when (intent) {
            is ChatRoomContract.Intent.Initialize -> initialize(intent)
            is ChatRoomContract.Intent.Connect -> connect()
            is ChatRoomContract.Intent.Disconnect -> disconnect()
            is ChatRoomContract.Intent.Reconnect -> reconnect()
            is ChatRoomContract.Intent.UpdateInputText -> updateInputText(intent.text)
            is ChatRoomContract.Intent.SendMessage -> sendMessage()
            is ChatRoomContract.Intent.LoadMoreMessages -> loadMoreMessages()
            is ChatRoomContract.Intent.DeleteMessage -> deleteMessage(intent.message)
            is ChatRoomContract.Intent.ReportMessage -> reportMessage(intent.message)
            is ChatRoomContract.Intent.CopyMessage -> copyMessage(intent.message)
            is ChatRoomContract.Intent.ScrollToBottom -> setEffect { ChatRoomContract.Effect.ScrollToBottom }
            is ChatRoomContract.Intent.UpdateScrollPosition -> updateScrollPosition(intent.isAtBottom, intent.shouldShowButton)
            is ChatRoomContract.Intent.ToggleInfoDrawer -> toggleInfoDrawer()
            is ChatRoomContract.Intent.CloseInfoDrawer -> setState { copy(showInfoDrawer = false) }
            is ChatRoomContract.Intent.ShowEmoticonSheet -> setState { copy(showEmoticonSheet = true) }
            is ChatRoomContract.Intent.HideEmoticonSheet -> setState { copy(showEmoticonSheet = false) }
            is ChatRoomContract.Intent.SendEmoticon -> sendEmoticon(intent.emoticonId)
            is ChatRoomContract.Intent.SendImage -> sendImage(intent.imageBytes)
            is ChatRoomContract.Intent.LeaveRoom -> leaveRoom()
            is ChatRoomContract.Intent.DeleteRoom -> deleteRoom()
            is ChatRoomContract.Intent.ReportRoom -> reportRoom()
            is ChatRoomContract.Intent.ShowGallery -> showGallery()
        }
    }

    private fun initialize(intent: ChatRoomContract.Intent.Initialize) {
        viewModelScope.launch {
            accountId = preferencesManager.getUserIdSync()
            val cdnUrl = preferencesManager.getCdnUrlSync() ?: ""

            setState {
                copy(
                    roomId = intent.roomId,
                    roomTitle = intent.title,
                    isAnonymous = intent.isAnonymity,
                    userId = intent.userId ?: accountId,
                    userNickname = intent.nickname ?: "",
                    userRole = intent.role ?: "N",
                    cdnUrl = cdnUrl,
                    isLoading = true
                )
            }

            // 로컬 DB 초기화
            initializeLocalDb()

            // 로컬 캐시에서 메시지 로드
            loadCachedMessages(intent.roomId)

            // 멤버 목록 미리 로드
            loadMembers()

            // 채팅방 신고 여부 확인
            checkRoomReported()

            // 채팅방 정보 조회 및 소켓 연결
            fetchRoomInfoAndConnect(intent.roomId)
        }
    }

    private suspend fun initializeLocalDb() {
        if (accountId > 0) {
            val db = ChatDatabase.getInstance(application, accountId)
            chatMessageDao = db.chatMessageDao()
            chatMemberDao = db.chatMemberDao()
        }
    }

    private suspend fun loadCachedMessages(roomId: Int) {
        try {
            val dao = chatMessageDao ?: return
            val cachedMessages = withContext(Dispatchers.IO) {
                dao.getMessages(roomId, MESSAGE_PAGE_SIZE, 0)
            }
            if (cachedMessages.isNotEmpty()) {
                setState { copy(messages = cachedMessages.reversed(), isLoading = false) }
            }
        } catch (e: Exception) {
            // 캐시 로드 실패는 무시
        }
    }

    private fun fetchRoomInfoAndConnect(roomId: Int) {
        viewModelScope.launch {
            // Config에서 chat_url 가져오기 (old 프로젝트: configModel.chat_url)
            val chatUrl = preferencesManager.getChatUrlSync()
            logD("ChatRoomViewModel", "fetchRoomInfoAndConnect - chatUrl: $chatUrl, roomId: $roomId")

            chatRepository.getChatRoomInfo(roomId).collect { result ->
                when (result) {
                    is ApiResult.Loading -> Unit
                    is ApiResult.Success -> {
                        val roomInfo = result.data
                        setState {
                            copy(
                                roomInfo = roomInfo,
                                isLoading = false
                            )
                        }

                        // Socket 연결 시작 (config의 chat_url 사용)
                        setupSocketAndConnect(chatUrl, roomId)
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        setEffect { ChatRoomContract.Effect.ShowError(result.message ?: "Failed to load room info") }
                    }
                }
            }
        }
    }

    private fun setupSocketAndConnect(socketUrl: String?, roomId: Int) {
        if (socketUrl.isNullOrEmpty()) {
            setEffect { ChatRoomContract.Effect.ShowError("Socket URL not available") }
            return
        }

        viewModelScope.launch {
            // SocketManager 초기화
            socketManager = ChatSocketManager.getInstance(application, preferencesManager, roomId)
            socketManager?.createSocket(socketUrl)

            // 연결 상태 관찰
            launch {
                socketManager?.connectionState?.collectLatest { state ->
                    logD("ChatRoomViewModel", "connectionState changed: $state")

                    val isConnecting = state == ChatSocketManager.ConnectionState.CONNECTING
                    val isError = state == ChatSocketManager.ConnectionState.ERROR ||
                            state == ChatSocketManager.ConnectionState.AUTH_FAILED

                    setState {
                        copy(
                            connectionState = state,
                            isConnecting = isConnecting,
                            isLoading = if (isError) false else isLoading // 에러 시 로딩 해제
                        )
                    }

                    when (state) {
                        ChatSocketManager.ConnectionState.AUTH_COMPLETE -> {
                            // 이전 메시지 요청 (첫 입장 메시지는 메시지 응답 후 처리)
                            requestPreviousMessages()
                        }
                        ChatSocketManager.ConnectionState.ERROR,
                        ChatSocketManager.ConnectionState.AUTH_FAILED -> {
                            setEffect { ChatRoomContract.Effect.ShowError("Connection failed. Please try again.") }
                        }
                        else -> Unit
                    }
                }
            }

            // 소켓 이벤트 관찰
            launch {
                socketManager?.socketEvents?.collectLatest { event ->
                    handleSocketEvent(event)
                }
            }

            // 연결 시작
            connect()
        }
    }

    private fun connect() {
        val state = uiState.value
        socketManager?.connect(state.roomId, state.userId)
    }

    private fun disconnect() {
        socketManager?.disconnect()
    }

    private fun reconnect() {
        disconnect()
        connect()
    }

    private fun handleSocketEvent(event: SocketEvent) {
        when (event) {
            is SocketEvent.AuthComplete -> {
                // 인증 완료
            }
            is SocketEvent.AuthFailed -> {
                setEffect { ChatRoomContract.Effect.ShowError("Authentication failed: ${event.message}") }
            }
            is SocketEvent.ConnectError -> {
                setEffect { ChatRoomContract.Effect.ShowError("Connection error: ${event.message}") }
            }
            is SocketEvent.ReceiveMessages -> {
                handleReceivedMessages(event.data)
            }
            is SocketEvent.SystemCommand -> {
                handleSystemCommand(event.data)
            }
            is SocketEvent.SystemMessage -> {
                handleSystemMessage(event.data)
            }
        }
    }

    private fun handleReceivedMessages(data: JSONObject) {
        viewModelScope.launch {
            try {
                logD("ChatRoomViewModel", "handleReceivedMessages: $data")

                val messagesArray = data.optJSONArray("messages")
                if (messagesArray == null || messagesArray.length() == 0) {
                    logD("ChatRoomViewModel", "No messages in response")
                    setState { copy(isLoadingMore = false) }
                    return@launch
                }

                val newMessages = mutableListOf<ChatMessageModel>()

                for (i in 0 until messagesArray.length()) {
                    val msgJson = messagesArray.getJSONObject(i)
                    val message = parseMessage(msgJson)
                    newMessages.add(message)
                }

                logD("ChatRoomViewModel", "Parsed ${newMessages.size} messages")

                // 로컬 DB에 저장
                saveMessagesToLocalDb(newMessages)

                // UI 업데이트 - 중복 제거
                val currentMessages = uiState.value.messages.toMutableList()
                val existingKeys = currentMessages.map { getMessageKey(it) }.toSet()

                // old 프로젝트: origin_req로 requestMessages 응답인지 확인
                val originReq = data.optJSONObject("origin_req")
                val isOgRequest = originReq != null && originReq.optString("cmd") == "requestMessages"

                logD("ChatRoomViewModel", "isOgRequest: $isOgRequest, originReq: $originReq")

                // 중복되지 않은 메시지만 필터링
                val uniqueNewMessages = newMessages.filter { getMessageKey(it) !in existingKeys }
                logD("ChatRoomViewModel", "Unique messages: ${uniqueNewMessages.size} out of ${newMessages.size}")

                if (uniqueNewMessages.isEmpty()) {
                    setState { copy(isLoadingMore = false) }
                    return@launch
                }

                if (isOgRequest) {
                    // 이전 메시지 로드 (앞에 추가, serverTs 기준 정렬)
                    val sortedMessages = uniqueNewMessages.sortedBy { it.serverTs }
                    currentMessages.addAll(0, sortedMessages)
                    setState {
                        copy(
                            messages = currentMessages,
                            hasMoreMessages = newMessages.size >= MESSAGE_PAGE_SIZE,
                            isLoadingMore = false
                        )
                    }
                    logD("ChatRoomViewModel", "Added ${sortedMessages.size} history messages, total: ${currentMessages.size}")

                    // 첫 입장 메시지 추가 (이전 메시지 로드 후 확인)
                    addFirstJoinMessageIfNeeded()
                } else {
                    // 새 메시지 (뒤에 추가)
                    currentMessages.addAll(uniqueNewMessages)
                    setState { copy(messages = currentMessages) }

                    logD("ChatRoomViewModel", "Added ${uniqueNewMessages.size} new messages, total: ${currentMessages.size}")

                    // 스크롤이 맨 아래가 아니면 unread count 증가
                    if (!uiState.value.showScrollToBottom) {
                        setEffect { ChatRoomContract.Effect.ScrollToBottom }
                    } else {
                        setState { copy(unreadCount = uiState.value.unreadCount + uniqueNewMessages.size) }
                    }
                }
            } catch (e: Exception) {
                logD("ChatRoomViewModel", "Error parsing messages: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 메시지 고유 키 생성 (중복 체크용)
     */
    private fun getMessageKey(message: ChatMessageModel): String {
        return "${message.serverTs}_${message.userId}_${message.clientTs}"
    }

    private fun parseMessage(json: JSONObject): ChatMessageModel {
        val contentType = json.optString("content_type", ChatMessageModel.TYPE_TEXT)
        val content = json.optString("content", "")

        // 이미지/이모티콘 메시지 디버깅
        if (contentType == ChatMessageModel.TYPE_IMAGE) {
            logD("ChatRoomViewModel", "parseMessage IMAGE: contentType=$contentType, content=$content")
        }

        return ChatMessageModel(
            clientTs = json.optLong("client_ts", 0L),
            serverTs = json.optLong("server_ts", System.currentTimeMillis()),
            userId = json.optInt("user_id", 0),
            roomId = json.optInt("room_id", uiState.value.roomId),
            content = content,
            contentType = contentType,
            contentDesc = json.optString("content_desc", null),
            status = json.optBoolean("status", true),
            isReadable = json.optBoolean("is_readable", true),
            reported = json.optBoolean("reported", false),
            deleted = json.optBoolean("deleted", false),
            isFirstJoinMsg = json.optBoolean("is_first_join_msg", false),
            seq = json.optInt("seq", 0),
            accountId = accountId
        )
    }

    private suspend fun saveMessagesToLocalDb(messages: List<ChatMessageModel>) {
        try {
            val dao = chatMessageDao ?: return
            withContext(Dispatchers.IO) {
                dao.insertMessages(messages)
            }
        } catch (e: Exception) {
            // 저장 실패 무시
        }
    }

    private fun handleSystemCommand(data: JSONObject) {
        val cmd = data.optString("cmd", "")
        when (cmd) {
            "deleteMessage" -> {
                val serverTs = data.optLong("server_ts", 0L)
                val userId = data.optInt("user_id", 0)
                markMessageAsDeleted(serverTs, userId)
            }
            "deleteRoom" -> {
                setEffect { ChatRoomContract.Effect.NavigateBack }
            }
        }
    }

    private fun handleSystemMessage(data: JSONObject) {
        val message = parseMessage(data)
        val currentMessages = uiState.value.messages.toMutableList()
        currentMessages.add(message)
        setState { copy(messages = currentMessages) }
    }

    private fun markMessageAsDeleted(serverTs: Long, userId: Int) {
        val currentMessages = uiState.value.messages.map { msg ->
            if (msg.serverTs == serverTs && msg.userId == userId) {
                msg.copy(deleted = true)
            } else {
                msg
            }
        }
        setState { copy(messages = currentMessages) }
    }

    private fun requestPreviousMessages() {
        // old 프로젝트와 동일: 로컬 DB의 가장 최근 serverTs 이후의 메시지 요청
        val lastServerTs = uiState.value.messages.maxOfOrNull { it.serverTs }
        socketManager?.requestMessages(endTs = lastServerTs?.let { it + 1 })
    }

    private fun loadMoreMessages() {
        if (uiState.value.isLoadingMore || !uiState.value.hasMoreMessages) return

        val oldestMessage = uiState.value.messages.firstOrNull() ?: return
        setState { copy(isLoadingMore = true) }
        socketManager?.requestOlderMessages(beforeTs = oldestMessage.serverTs)
    }

    private fun updateInputText(text: String) {
        setState { copy(inputText = text) }
    }

    private fun sendMessage() {
        val text = uiState.value.inputText.trim()
        if (text.isEmpty()) return

        setState { copy(inputText = "", isSending = true) }

        socketManager?.sendMessage(text)

        // 전송 후 상태 업데이트
        setState { copy(isSending = false) }
    }

    /**
     * 이모티콘 전송
     * old 프로젝트: sendEmoticonSocket() - JSON 형식으로 url, thumbnail, is_emoticon 전송
     * cdnUrl과 emoticonId를 조합하여 올바른 URL 생성
     */
    private fun sendEmoticon(emoticonId: Int) {
        viewModelScope.launch {
            val cdnUrl = preferencesManager.getCdnUrlSync()
            if (cdnUrl.isNullOrEmpty()) {
                logD("ChatRoomVM", "sendEmoticon failed: cdnUrl is empty")
                return@launch
            }

            val emoticonUrl = IdolImageUtil.getEmoticonImageUrl(cdnUrl, emoticonId).toSecureUrl()
            logD("ChatRoomVM", "sendEmoticon: id=$emoticonId, url=$emoticonUrl")

            socketManager?.sendEmoticon(emoticonUrl)
            setState { copy(showEmoticonSheet = false) }
        }
    }

    /**
     * 이미지 전송
     * old 프로젝트: sendImage(byteArray) -> imagesRepository.uploadImage() -> sendImageSocket()
     */
    private fun sendImage(imageBytes: ByteArray) {
        viewModelScope.launch {
            setState { copy(isUploadingImage = true) }

            chatRepository.uploadChatImage(imageBytes).collect { result ->
                when (result) {
                    is ApiResult.Loading -> Unit
                    is ApiResult.Success -> {
                        val response = result.data
                        // 업로드 성공 시 소켓으로 이미지 전송
                        socketManager?.sendImage(
                            imageUrl = response.imageUrl,
                            thumbnailUrl = response.thumbnailUrl,
                            umjjalUrl = response.umjjalUrl,
                            thumbHeight = response.thumbHeight,
                            thumbWidth = response.thumbWidth
                        )
                        setState { copy(isUploadingImage = false) }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isUploadingImage = false) }
                        setEffect { ChatRoomContract.Effect.ShowError(result.message ?: "이미지 업로드 실패") }
                    }
                }
            }
        }
    }

    private fun deleteMessage(message: ChatMessageModel) {
        setEffect { ChatRoomContract.Effect.ShowDeleteConfirmDialog(message) }
    }

    fun confirmDeleteMessage(message: ChatMessageModel) {
        socketManager?.deleteMessage(message.serverTs)
    }

    private fun reportMessage(message: ChatMessageModel) {
        setEffect { ChatRoomContract.Effect.ShowReportConfirmDialog(message) }
    }

    fun confirmReportMessage(message: ChatMessageModel) {
        socketManager?.reportMessage(message.serverTs, message.userId)
    }

    private fun copyMessage(message: ChatMessageModel) {
        setEffect { ChatRoomContract.Effect.CopyToClipboard(message.content) }
    }

    private fun updateScrollPosition(isAtBottom: Boolean, shouldShowButton: Boolean) {
        setState {
            copy(
                // old 프로젝트: 화면 크기의 2배 이상 스크롤했을 때만 버튼 표시
                showScrollToBottom = shouldShowButton && !isAtBottom,
                unreadCount = if (isAtBottom) 0 else unreadCount
            )
        }
    }

    private fun toggleInfoDrawer() {
        val showDrawer = !uiState.value.showInfoDrawer
        if (showDrawer) {
            loadMembers()
        }
        setState { copy(showInfoDrawer = showDrawer) }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            val roomId = uiState.value.roomId

            // 1. 먼저 로컬 DB에서 캐시된 멤버 로드 (빠른 UI 업데이트)
            loadCachedMembers(roomId)

            // 2. API 호출하여 최신 멤버 가져오기
            chatRepository.getChatMembers(roomId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val members = result.data
                        setState { copy(members = members) }

                        // 3. 로컬 DB에 저장
                        saveMembersToLocalDb(members)
                    }
                    else -> Unit
                }
            }
        }
    }

    private suspend fun loadCachedMembers(roomId: Int) {
        try {
            val dao = chatMemberDao ?: return
            val cachedMembers = withContext(Dispatchers.IO) {
                dao.getRoomMembers(roomId)
            }
            if (!cachedMembers.isNullOrEmpty()) {
                setState { copy(members = cachedMembers) }
                logD("ChatRoomViewModel", "Loaded ${cachedMembers.size} cached members")
            }
        } catch (e: Exception) {
            // 캐시 로드 실패는 무시
            logD("ChatRoomViewModel", "Failed to load cached members: ${e.message}")
        }
    }

    private suspend fun saveMembersToLocalDb(members: List<net.ib.mn.domain.model.ChatMemberModel>) {
        try {
            val dao = chatMemberDao ?: return
            withContext(Dispatchers.IO) {
                dao.insertMembers(members)
            }
            logD("ChatRoomViewModel", "Saved ${members.size} members to local DB")
        } catch (e: Exception) {
            // 저장 실패 무시
            logD("ChatRoomViewModel", "Failed to save members: ${e.message}")
        }
    }

    /**
     * 첫 입장 메시지 추가 (old 프로젝트: ChattingRoomListFragment.kt)
     * - 메시지 목록 및 로컬 DB에 첫 입장 메시지가 없으면 추가
     */
    private fun addFirstJoinMessageIfNeeded() {
        viewModelScope.launch {
            val state = uiState.value
            val currentMessages = state.messages

            // 1. 현재 메시지 목록에 있는지 확인
            val hasFirstJoinMsgInList = currentMessages.any { it.isFirstJoinMsg }
            if (hasFirstJoinMsgInList) {
                logD("ChatRoomViewModel", "First join message already exists in list")
                return@launch
            }

            // 2. 로컬 DB에 있는지 확인
            val dao = chatMessageDao
            if (dao != null) {
                val existingFirstJoinMsg = withContext(Dispatchers.IO) {
                    dao.getFirstJoinMessage(state.roomId)
                }
                if (existingFirstJoinMsg != null) {
                    // 로컬 DB에 있으면 목록에 추가만
                    val updatedMessages = listOf(existingFirstJoinMsg) + currentMessages
                    setState { copy(messages = updatedMessages) }
                    logD("ChatRoomViewModel", "First join message loaded from DB")
                    return@launch
                }
            }

            // 3. 없으면 새로 생성
            val presentClientTs = System.currentTimeMillis()
            val firstJoinMessage = ChatMessageModel(
                roomId = state.roomId,
                userId = state.userId,
                clientTs = presentClientTs,
                serverTs = presentClientTs,
                content = application.getString(R.string.chat_first_join),
                contentType = ChatMessageModel.TYPE_TEXT,
                isReadable = true,
                isFirstJoinMsg = true,
                accountId = accountId
            )

            // 로컬 DB에 저장
            saveMessagesToLocalDb(listOf(firstJoinMessage))

            // UI 업데이트 - 맨 앞에 추가
            val updatedMessages = listOf(firstJoinMessage) + currentMessages
            setState { copy(messages = updatedMessages) }

            logD("ChatRoomViewModel", "Added new first join message")
        }
    }

    private fun leaveRoom() {
        setEffect { ChatRoomContract.Effect.ShowLeaveConfirmDialog }
    }

    fun confirmLeaveRoom() {
        viewModelScope.launch {
            chatRepository.leaveChatRoom(uiState.value.roomId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        if (result.data.success) {
                            disconnect()
                            setEffect { ChatRoomContract.Effect.NavigateBackWithRefresh }
                        } else {
                            setEffect { ChatRoomContract.Effect.ShowError(result.data.message ?: "Failed to leave") }
                        }
                    }
                    is ApiResult.Error -> {
                        setEffect { ChatRoomContract.Effect.ShowError(result.message ?: "Failed to leave") }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun deleteRoom() {
        setEffect { ChatRoomContract.Effect.ShowDeleteRoomConfirmDialog }
    }

    fun confirmDeleteRoom() {
        viewModelScope.launch {
            chatRepository.deleteChatRoom(uiState.value.roomId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        disconnect()
                        setEffect { ChatRoomContract.Effect.NavigateBackWithRefresh }
                    }
                    is ApiResult.Error -> {
                        setEffect { ChatRoomContract.Effect.ShowError(result.message ?: "Failed to delete room") }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun reportRoom() {
        // 이미 신고한 방인지 확인
        if (uiState.value.isReportedRoom) {
            setEffect { ChatRoomContract.Effect.ShowReportRoomAlreadyReported(
                application.getString(R.string.chat_report_error_2401)
            ) }
        } else {
            val reportHeart = configRepository.getReportHeart()
            setEffect { ChatRoomContract.Effect.ShowReportRoomConfirmDialog(reportHeart) }
        }
    }

    /**
     * 신고 확인 후 호출
     */
    fun confirmReportRoom(reason: String) {
        // 이미 신고한 방인지 다시 확인 (API 호출 방지)
        if (uiState.value.isReportedRoom) {
            setEffect { ChatRoomContract.Effect.ShowReportRoomAlreadyReported(
                application.getString(R.string.chat_report_error_2401)
            ) }
            return
        }

        viewModelScope.launch {
            chatRepository.reportChatRoom(uiState.value.roomId, reason).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        setState { copy(isReportedRoom = true) }
                        setEffect { ChatRoomContract.Effect.ShowError(application.getString(R.string.report_done)) }
                    }
                    is ApiResult.Error -> {
                        setEffect { ChatRoomContract.Effect.ShowError(result.message ?: "Failed to report") }
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
     * 채팅방 신고 여부 확인
     */
    private fun checkRoomReported() {
        viewModelScope.launch {
            chatRepository.isReportedChatRoom(
                uiState.value.roomId,
                uiState.value.userId.toLong()
            ).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        setState { copy(isReportedRoom = result.data) }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun showGallery() {
        setEffect { ChatRoomContract.Effect.ShowGalleryPicker }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
        ChatSocketManager.destroyInstance(uiState.value.roomId)
    }
}
