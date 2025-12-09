package net.ib.mn.data.remote.socket

import android.content.Context
import android.net.Uri
import android.util.Base64
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.util.Constants
import net.ib.mn.util.ServerUrl
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * 채팅 Socket 관리자
 * old 프로젝트: app/src/main/java/net/ib/mn/chatting/SocketManager.kt
 *
 * Socket.IO를 사용한 실시간 채팅 통신
 */
class ChatSocketManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private var socket: Socket? = null
    private var dispatcher: Dispatcher? = null
    private var sequenceNum = 1

    var roomId: Int? = null
        private set
    var userId: Int? = null
        private set

    // Connection State
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Socket Events
    private val _socketEvents = MutableSharedFlow<SocketEvent>(replay = 0, extraBufferCapacity = 64)
    val socketEvents: SharedFlow<SocketEvent> = _socketEvents.asSharedFlow()

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        AUTH_COMPLETE,
        AUTH_FAILED,
        ERROR
    }

    /**
     * Socket 생성
     * old 프로젝트: 서버에서는 ws,wss를 http,https로 보내고 있음. 안드로이드에서는 ws 인식 불가.
     */
    fun createSocket(chatUrl: String?) {
        if (chatUrl.isNullOrEmpty()) {
            logE("ChatSocketManager", "chatUrl is null or empty")
            _connectionState.value = ConnectionState.ERROR
            _socketEvents.tryEmit(SocketEvent.ConnectError("Chat URL is not available"))
            return
        }

        try {
            // 기존 소켓이 있으면 정리
            if (socket != null) {
                logD("ChatSocketManager", "Cleaning up existing socket before creating new one")
                socket?.off()
                socket?.disconnect()
                socket = null
            }

            dispatcher?.executorService?.shutdown()
            dispatcher = Dispatcher()
            val okHttpClient = OkHttpClient.Builder()
                .dispatcher(dispatcher!!)
                .readTimeout(1, TimeUnit.MINUTES)
                .build()

            val parse = Uri.parse(chatUrl)
            val options = IO.Options()
            options.transports = arrayOf("websocket")
            options.secure = ServerUrl.HOST.startsWith("https")
            options.path = "/" + parse.lastPathSegment
            options.reconnection = true
            options.reconnectionAttempts = Integer.MAX_VALUE
            options.reconnectionDelay = 1_000
            options.reconnectionDelayMax = 5_000
            options.randomizationFactor = 0.5
            options.timeout = 20_000

            // Authorization header (old 프로젝트와 동일한 방식)
            val headers = HashMap<String, List<String>>()
            val email = preferencesManager.getUserEmail()
            val domain = preferencesManager.getUserDomain()
            val token = preferencesManager.getUserToken()
            val creds = "$email:$domain:$token"
            val authHeader = "Basic ${Base64.encodeToString(creds.toByteArray(), Base64.NO_WRAP)}"
            headers["Authorization"] = listOf(authHeader)
            headers["User-Agent"] = listOf("${System.getProperty("http.agent")} (${context.packageName})")
            headers["X-HTTP-NATION"] = listOf(java.util.Locale.getDefault().language)
            options.extraHeaders = headers

            options.callFactory = okHttpClient
            options.webSocketFactory = okHttpClient

            logD("ChatSocketManager", "Socket options set, chatUrl=$chatUrl, path=${options.path}")

            socket = IO.socket(URI.create("${parse.scheme}://${parse.host}"), options)
            logD("ChatSocketManager", "Socket created: ${parse.scheme}://${parse.host}")
        } catch (e: Exception) {
            logE("ChatSocketManager", "Failed to create socket: ${e.message}")
            e.printStackTrace()
            _connectionState.value = ConnectionState.ERROR
            _socketEvents.tryEmit(SocketEvent.ConnectError("Failed to create socket: ${e.message}"))
        }
    }

    /**
     * Socket 연결
     */
    fun connect(roomId: Int, userId: Int) {
        this.roomId = roomId
        this.userId = userId

        val s = socket
        if (s == null) {
            logE("ChatSocketManager", "Socket is null, cannot connect")
            _connectionState.value = ConnectionState.ERROR
            _socketEvents.tryEmit(SocketEvent.ConnectError("Socket not initialized"))
            return
        }

        // 이미 연결된 상태면 인증 완료 이벤트만 발송
        if (s.connected()) {
            logD("ChatSocketManager", "Socket already connected, checking auth state")
            _connectionState.value = ConnectionState.CONNECTED
            // 이미 연결되어 있으면 onConnect 콜백이 호출되지 않으므로 직접 처리
            if (!s.hasListeners(Constants.CHAT_AUTH_COMPLETE)) {
                s.on(Constants.CHAT_AUTH_COMPLETE, onAuthComplete)
            }
            if (!s.hasListeners(Constants.CHAT_AUTH_FAILED)) {
                s.on(Constants.CHAT_AUTH_FAILED, onAuthFailed)
            }
            return
        }

        _connectionState.value = ConnectionState.CONNECTING

        // Connection events
        if (!s.hasListeners(Socket.EVENT_CONNECT)) {
            s.on(Socket.EVENT_CONNECT, onConnect)
        }
        if (!s.hasListeners(Socket.EVENT_DISCONNECT)) {
            s.on(Socket.EVENT_DISCONNECT, onDisconnect)
        }
        if (!s.hasListeners(Socket.EVENT_CONNECT_ERROR)) {
            s.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
        }

        s.connect()
        logD("ChatSocketManager", "Connecting to socket...")
    }

    /**
     * Socket 연결 해제
     */
    fun disconnect() {
        socket?.off(Socket.EVENT_CONNECT, onConnect)
        socket?.off(Socket.EVENT_DISCONNECT, onDisconnect)
        socket?.off(Socket.EVENT_CONNECT_ERROR, onConnectError)
        removeEventListeners()

        socket?.disconnect()
        dispatcher?.executorService?.shutdown()

        // socket과 dispatcher를 null로 설정하여 재접속 시 새로 생성되도록 함
        socket = null
        dispatcher = null

        logD("ChatSocketManager", "Socket disconnected and cleared")
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Socket 인스턴스 해제
     */
    fun destroy() {
        disconnect()
        socket = null
        roomId = null
        userId = null
    }

    private fun removeEventListeners() {
        socket?.off(Constants.CHAT_AUTH_COMPLETE, onAuthComplete)
        socket?.off(Constants.CHAT_AUTH_FAILED, onAuthFailed)
        socket?.off(Constants.CHAT_SYSTEM_COMMAND, onSystemCommand)
        socket?.off(Constants.CHAT_RECEIVE_MESSAGES, onReceiveMessage)
        socket?.off(Constants.CHAT_SYSTEM_MESSAGE, onSystemMessage)
    }

    // ============================================================
    // Socket Event Listeners
    // ============================================================

    private val onConnect = Emitter.Listener {
        logD("ChatSocketManager", "Socket connected")
        _connectionState.value = ConnectionState.CONNECTED

        socket?.let { s ->
            if (!s.hasListeners(Constants.CHAT_AUTH_COMPLETE)) {
                s.on(Constants.CHAT_AUTH_COMPLETE, onAuthComplete)
            }
            if (!s.hasListeners(Constants.CHAT_AUTH_FAILED)) {
                s.on(Constants.CHAT_AUTH_FAILED, onAuthFailed)
            }
        }
    }

    private val onDisconnect = Emitter.Listener {
        logD("ChatSocketManager", "Socket disconnected")
        _connectionState.value = ConnectionState.DISCONNECTED
        removeEventListeners()
    }

    private val onConnectError = Emitter.Listener { args ->
        logE("ChatSocketManager", "Socket connect error: ${args.firstOrNull()}")
        _connectionState.value = ConnectionState.ERROR
        _socketEvents.tryEmit(SocketEvent.ConnectError(args.firstOrNull()?.toString()))
    }

    private val onAuthComplete = Emitter.Listener { args ->
        logD("ChatSocketManager", "Auth complete: ${args.firstOrNull()}")

        socket?.let { s ->
            // Register message listeners BEFORE notifying auth complete
            // This prevents race condition where requestMessages is called before listeners are registered
            if (!s.hasListeners(Constants.CHAT_RECEIVE_MESSAGES)) {
                s.on(Constants.CHAT_RECEIVE_MESSAGES, onReceiveMessage)
                logD("ChatSocketManager", "Registered receiveMessages listener")
            } else {
                logD("ChatSocketManager", "receiveMessages listener already registered")
            }
            if (!s.hasListeners(Constants.CHAT_SYSTEM_COMMAND)) {
                s.on(Constants.CHAT_SYSTEM_COMMAND, onSystemCommand)
                logD("ChatSocketManager", "Registered systemCommand listener")
            }
            if (!s.hasListeners(Constants.CHAT_SYSTEM_MESSAGE)) {
                s.on(Constants.CHAT_SYSTEM_MESSAGE, onSystemMessage)
                logD("ChatSocketManager", "Registered systemMessage listener")
            }
        } ?: run {
            logE("ChatSocketManager", "Socket is null in onAuthComplete")
        }

        // Set state AFTER listeners are registered
        _connectionState.value = ConnectionState.AUTH_COMPLETE
        _socketEvents.tryEmit(SocketEvent.AuthComplete)
    }

    private val onAuthFailed = Emitter.Listener { args ->
        logE("ChatSocketManager", "Auth failed: ${args.firstOrNull()}")
        _connectionState.value = ConnectionState.AUTH_FAILED
        _socketEvents.tryEmit(SocketEvent.AuthFailed(args.firstOrNull()?.toString()))
    }

    private val onReceiveMessage = Emitter.Listener { args ->
        logD("ChatSocketManager", "onReceiveMessage triggered, args count: ${args.size}")
        try {
            val first = args.firstOrNull()
            if (first == null) {
                logE("ChatSocketManager", "onReceiveMessage: first arg is null")
                return@Listener
            }
            logD("ChatSocketManager", "onReceiveMessage: arg type = ${first.javaClass.name}")

            val data = first as? JSONObject
            if (data == null) {
                logE("ChatSocketManager", "onReceiveMessage: cannot cast to JSONObject, raw = $first")
                return@Listener
            }
            logD("ChatSocketManager", "Received message: $data")
            _socketEvents.tryEmit(SocketEvent.ReceiveMessages(data))
        } catch (e: Exception) {
            logE("ChatSocketManager", "Error parsing received message: ${e.message}")
            e.printStackTrace()
        }
    }

    private val onSystemCommand = Emitter.Listener { args ->
        try {
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            logD("ChatSocketManager", "System command: $data")
            _socketEvents.tryEmit(SocketEvent.SystemCommand(data))
        } catch (e: Exception) {
            logE("ChatSocketManager", "Error parsing system command: ${e.message}")
        }
    }

    private val onSystemMessage = Emitter.Listener { args ->
        try {
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            logD("ChatSocketManager", "System message: $data")
            _socketEvents.tryEmit(SocketEvent.SystemMessage(data))
        } catch (e: Exception) {
            logE("ChatSocketManager", "Error parsing system message: ${e.message}")
        }
    }

    // ============================================================
    // Send Methods
    // ============================================================

    /**
     * 메시지 전송
     */
    fun sendMessage(content: String, contentType: String = "text/plain") {
        val clientTs = System.currentTimeMillis()
        ++sequenceNum

        val messageJson = JSONObject().apply {
            put("cmd", Constants.CHAT_SEND_MESSAGE)
            put("seq", sequenceNum)
            put("room_id", roomId)
            put("content", content)
            put("content_type", contentType)
            put("client_ts", clientTs)
        }

        socket?.emit(Constants.CHAT_SEND_MESSAGE, messageJson)
        logD("ChatSocketManager", "Sent message: $messageJson")
    }

    /**
     * 이모티콘 전송
     * old 프로젝트: sendEmoticonSocket() - JSON 형식으로 url, thumbnail, is_emoticon 전송
     *
     * @param imageUrl 이모티콘 이미지 URL
     * @param thumbnailUrl 썸네일 URL (이모티콘은 동일 URL 사용)
     */
    fun sendEmoticon(imageUrl: String, thumbnailUrl: String = imageUrl) {
        val clientTs = System.currentTimeMillis()
        ++sequenceNum

        // old 프로젝트와 동일한 JSON 포맷
        val contentObject = JSONObject().apply {
            put("url", imageUrl)
            put("thumbnail", thumbnailUrl)
            put("is_emoticon", "true")
        }

        val messageJson = JSONObject().apply {
            put("cmd", Constants.CHAT_SEND_MESSAGE)
            put("seq", sequenceNum)
            put("room_id", roomId)
            put("content", contentObject.toString().replace("\\/", "/")) // Slash 이스케이프 처리
            put("content_type", Constants.CHAT_TYPE_IMAGE)
            put("client_ts", clientTs)
        }

        socket?.emit(Constants.CHAT_SEND_MESSAGE, messageJson)
        logD("ChatSocketManager", "Sent emoticon: $messageJson")
    }

    /**
     * 이미지 전송
     * old 프로젝트: sendImageSocket() - JSON 형식으로 url, thumbnail, umjjal 등 전송
     *
     * @param imageUrl 원본 이미지 URL
     * @param thumbnailUrl 썸네일 URL
     * @param umjjalUrl 움짤 URL (없으면 빈 문자열)
     * @param thumbHeight 썸네일 높이
     * @param thumbWidth 썸네일 너비
     */
    fun sendImage(
        imageUrl: String,
        thumbnailUrl: String,
        umjjalUrl: String = "",
        thumbHeight: Int = 0,
        thumbWidth: Int = 0
    ) {
        val clientTs = System.currentTimeMillis()
        ++sequenceNum

        // old 프로젝트와 동일한 JSON 포맷
        val contentObject = JSONObject().apply {
            put("url", imageUrl)
            put("thumbnail", thumbnailUrl)
            put("umjjal", umjjalUrl)
            if (thumbHeight > 0 && thumbWidth > 0) {
                put("thumb_height", thumbHeight.toString())
                put("thumb_width", thumbWidth.toString())
            }
        }

        val messageJson = JSONObject().apply {
            put("cmd", Constants.CHAT_SEND_MESSAGE)
            put("seq", sequenceNum)
            put("room_id", roomId)
            put("content", contentObject.toString().replace("\\/", "/")) // Slash 이스케이프 처리
            put("content_type", Constants.CHAT_TYPE_IMAGE)
            put("client_ts", clientTs)
        }

        socket?.emit(Constants.CHAT_SEND_MESSAGE, messageJson)
        logD("ChatSocketManager", "Sent image: $messageJson")
    }

    /**
     * 이전 메시지 요청
     * old 프로젝트와 동일한 형식 사용 (start_ts, end_ts)
     *
     * @param endTs 이 시간 이후의 메시지를 요청 (null이면 처음 요청)
     */
    fun requestMessages(endTs: Long? = null) {
        ++sequenceNum

        val startTs = System.currentTimeMillis()

        val requestJson = JSONObject().apply {
            put("cmd", Constants.CHAT_REQUEST_MESSAGES)
            put("seq", sequenceNum)
            put("room_id", roomId)
            put("start_ts", startTs)
            // endTs가 null이면 가장 오래된 메시지부터, 아니면 endTs 이후 메시지
            endTs?.let { put("end_ts", it) }
        }

        val hasListener = socket?.hasListeners(Constants.CHAT_RECEIVE_MESSAGES) ?: false
        logD("ChatSocketManager", "requestMessages - hasReceiveListener: $hasListener, isConnected: ${socket?.connected()}")

        socket?.emit(Constants.CHAT_REQUEST_MESSAGES, requestJson)
        logD("ChatSocketManager", "Request messages: $requestJson")
    }

    /**
     * 이전 메시지 로드 (더 오래된 메시지)
     * @param beforeTs 이 시간 이전의 메시지를 요청
     */
    fun requestOlderMessages(beforeTs: Long) {
        ++sequenceNum

        val requestJson = JSONObject().apply {
            put("cmd", Constants.CHAT_REQUEST_MESSAGES)
            put("seq", sequenceNum)
            put("room_id", roomId)
            put("start_ts", beforeTs)
            put("end_ts", 0L) // 0으로 설정하면 beforeTs 이전 메시지 요청
        }

        val hasListener = socket?.hasListeners(Constants.CHAT_RECEIVE_MESSAGES) ?: false
        logD("ChatSocketManager", "requestOlderMessages - hasReceiveListener: $hasListener, isConnected: ${socket?.connected()}")

        socket?.emit(Constants.CHAT_REQUEST_MESSAGES, requestJson)
        logD("ChatSocketManager", "Request older messages: $requestJson")
    }

    /**
     * 메시지 삭제 요청
     */
    fun deleteMessage(serverTs: Long) {
        ++sequenceNum

        val deleteJson = JSONObject().apply {
            put("cmd", Constants.CHAT_REQUEST_DELETE)
            put("seq", sequenceNum)
            put("room_id", roomId)
            put("server_ts", serverTs)
        }

        socket?.emit(Constants.CHAT_REQUEST_DELETE, deleteJson)
        logD("ChatSocketManager", "Delete message: $deleteJson")
    }

    /**
     * 메시지 신고
     */
    fun reportMessage(serverTs: Long, targetUserId: Int) {
        ++sequenceNum

        val reportJson = JSONObject().apply {
            put("cmd", Constants.CHAT_REPORT_MESSAGE)
            put("seq", sequenceNum)
            put("room_id", roomId)
            put("server_ts", serverTs)
            put("user_id", targetUserId)
        }

        socket?.emit(Constants.CHAT_REPORT_MESSAGE, reportJson)
        logD("ChatSocketManager", "Report message: $reportJson")
    }

    /**
     * 상태 변경 (foreground/background)
     */
    fun changeState(isForeground: Boolean) {
        ++sequenceNum

        val stateJson = JSONObject().apply {
            put("cmd", Constants.CHAT_CHANGE_STATE)
            put("seq", sequenceNum)
            put("state", if (isForeground) "FOREGROUND" else "BACKGROUND")
        }

        socket?.emit(Constants.CHAT_CHANGE_STATE, stateJson)
        logD("ChatSocketManager", "Change state: $stateJson")
    }

    /**
     * Socket 연결 상태 확인
     */
    fun isConnected(): Boolean = socket?.connected() == true

    companion object {
        @Volatile
        private var instances: MutableMap<Int, ChatSocketManager> = mutableMapOf()

        /**
         * 채팅방별 SocketManager 인스턴스 생성
         * 기존 인스턴스가 있으면 상태를 확인하고 필요시 정리
         */
        fun getInstance(
            context: Context,
            preferencesManager: PreferencesManager,
            roomId: Int
        ): ChatSocketManager {
            return synchronized(this) {
                val existing = instances[roomId]
                if (existing != null) {
                    logD("ChatSocketManager", "getInstance - found existing instance for room $roomId, socket=${existing.socket != null}, connected=${existing.socket?.connected()}")

                    // 기존 인스턴스가 있지만 socket이 null이거나 연결이 안 되어 있으면 정리
                    if (existing.socket == null || existing.socket?.connected() != true) {
                        existing.disconnect()
                        // 상태 초기화
                        existing._connectionState.value = ConnectionState.DISCONNECTED
                    }
                    existing
                } else {
                    logD("ChatSocketManager", "getInstance - creating new instance for room $roomId")
                    ChatSocketManager(context, preferencesManager).also {
                        instances[roomId] = it
                    }
                }
            }
        }

        /**
         * 특정 채팅방 SocketManager 해제
         */
        fun destroyInstance(roomId: Int) {
            instances[roomId]?.destroy()
            instances.remove(roomId)
        }

        /**
         * 모든 SocketManager 해제
         */
        fun destroyAll() {
            instances.forEach { (_, manager) -> manager.destroy() }
            instances.clear()
        }
    }
}

/**
 * Socket 이벤트
 */
sealed class SocketEvent {
    object AuthComplete : SocketEvent()
    data class AuthFailed(val message: String?) : SocketEvent()
    data class ConnectError(val message: String?) : SocketEvent()
    data class ReceiveMessages(val data: JSONObject) : SocketEvent()
    data class SystemCommand(val data: JSONObject) : SocketEvent()
    data class SystemMessage(val data: JSONObject) : SocketEvent()
}
