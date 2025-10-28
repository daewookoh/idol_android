package net.ib.mn.chatting

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.PushStartActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.chatting.chatDb.ChatDB
import net.ib.mn.chatting.model.ChatMsgLinkModel
import net.ib.mn.chatting.model.MessageModel
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.EventBus
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

open class SocketManager private constructor(){

    //Socket
    var socket: Socket? = null

    //SequenceNumber
    var squenceNum = 1

    var mContext: Context? = null

    var roomId: Int? = null
    var userId: Int? = null

    var count = 0

    //채팅 노티 ID.
    private val ID_CHATTING_MSG = 70

    var dispatcher: Dispatcher? = null

    //소켓생성.
    fun createSocket() {
        try {
            // https://socketio.github.io/socket.io-client-java/faq.html#How_to_properly_close_a_client
            dispatcher = Dispatcher()
            val okHttpClient = OkHttpClient.Builder()
                .dispatcher(dispatcher!!)
                .readTimeout(1, TimeUnit.MINUTES)
                .build()

            //현재 서버에서는 ws,wss를 http,https로보내고있음 안드에선 ws인식 불가.
            var chatUrl = ConfigModel.getInstance(mContext).chat_url
            val parse = Uri.parse(chatUrl)
            val options = IO.Options()
            options.transports = arrayOf("websocket")
            options.secure = ServerUrl.HOST.startsWith("https")
            options.path = "/"+parse.lastPathSegment
            options.reconnection = true
            options.reconnectionAttempts = Integer.MAX_VALUE
            options.reconnectionDelay = 1_000
            options.reconnectionDelayMax = 5_000
            options.randomizationFactor = 0.5
            options.timeout = 20_000
            //Authorization헤더.
            //extraHeaders 보면 <String,List<String>> 확인.
            val headers = HashMap<String, List<String>>()
            val account = IdolAccount.getAccount(mContext)
            val creds = account?.email + ":" + account?.domain + ":" + account?.token
            val authHeader = "Basic ${Base64.encodeToString(creds.toByteArray(), Base64.NO_WRAP)}"
            headers["Authorization"] = listOf(authHeader)
            headers["User-Agent"] = listOf(System.getProperty("http.agent") + " (" + mContext!!.applicationInfo.packageName + "/" + mContext!!.getString(R.string.app_version) + ")")
            headers["X-HTTP-NATION"] = listOf(Util.getSystemLanguage(mContext))
            options.extraHeaders = headers

            options.callFactory = okHttpClient
            options.webSocketFactory = okHttpClient

            Util.log("idoltalk::socket option set")
            //socket 널체크해준다 여기서 다시연결될때 socket 인스턴스가 여러개 생성됨.
            if (socket == null){
                Util.log("idoltalk::socket is null")
                socket = IO.socket(URI.create(parse.scheme + "://" + parse.host), options)
            }

            Util.log("idoltalk::socket is created ->")
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    //소켓연결.
    fun connectSocket() {
        Util.log("idoltalk::connectSocket")
        if (socket?.connected() != true) socket?.on(Socket.EVENT_CONNECT, onConnect)
        if (!socket?.hasListeners(Socket.EVENT_DISCONNECT)!!)
            socket?.on(Socket.EVENT_DISCONNECT, onDisconnect)
        if (!socket?.hasListeners(Socket.EVENT_CONNECT_ERROR)!!)
            socket?.on(Socket.EVENT_CONNECT_ERROR, onConnectError)

        socket!!.connect()
    }

    //소켓 해제.
    fun disconnectSocket() {
        socket?.disconnect()
        dispatcher?.executorService?.shutdown()
        Util.log("idoltalk::socket disconnect")
        socket?.off(Socket.EVENT_CONNECT, onConnect)
        socket?.off(Socket.EVENT_CONNECT_ERROR, onConnectError)
    }

    fun setOffSocketEvent() {
        Util.log("idoltalk::socketEventOff")

        //Socket DisConnect될때 Listener제거.
        socket?.off(Const.CHAT_AUTH_COMPLETE, onAuthComplete)
        socket?.off(Const.CHAT_AUTH_FAILED, onAuthFailed)
        socket?.off(Const.CHAT_SYSTEM_COMMAND, onSystemCommand)
        socket?.off(Const.CHAT_RECEIVE_MESSAGES, onReceiveMessage)
        socket?.off(Const.CHAT_SYSTEM_MESSAGE, onSystemMessage)
    }

    //연결 해제.
    private val onDisconnect = object : Emitter.Listener {
        override fun call(vararg args: Any) {
            socket?.off(Socket.EVENT_DISCONNECT, this)
            disconnectSocket()
            setOffSocketEvent()
            Util.log("idoltalk::socket disconnected")
            Util.log("idoltalk::socket is " + socket?.connected())
            socket = null
        }
    }

    //소켓 연결 완료.
    private val onConnect = Emitter.Listener {
        Util.log("idoltalk::socket onConnect")
        //sendAuth이제 안쓰므로 onConnect되면 바로 리스너 등록.
        socket?.let { safeSocket ->
            if (!safeSocket.hasListeners(Const.CHAT_AUTH_COMPLETE)) {
                safeSocket.on(Const.CHAT_AUTH_COMPLETE, onAuthComplete)
            }

            if (!safeSocket.hasListeners(Const.CHAT_AUTH_FAILED)) {
                safeSocket.on(Const.CHAT_AUTH_FAILED, onAuthFailed)
            }
        }
    }

    //연결 에러.
    private val onConnectError = Emitter.Listener {args ->
        Util.log("idoltalk::socket Error is " + socket?.connected())
        val obj = JSONObject().put("connected", socket?.connected())
        EventBus.sendEvent(obj, Socket.EVENT_CONNECT_ERROR)
    }

    //인증실패.
    val onAuthFailed = Emitter.Listener { args ->
        val authFailedSeq = args[0]
        Util.log("idoltalk::isAuthFailed -> $authFailedSeq")
    }

    //인증 성공 확인.
    val onAuthComplete = Emitter.Listener { args ->
        try {
            val authSuccessSeq = args[0] as JSONObject
            Util.log("idoltalk::isAuthSuccess -> $authSuccessSeq")
            val obj = JSONObject().put("connected", socket?.connected())
            EventBus.sendEvent(obj, Const.CHAT_AUTH_COMPLETE)

            //채팅방 안에서 비행기모드 켯다가 백그라운드가고 비행기모드 끄면서 돌아올때 상태가 바껴서 내가보낸게 푸시가 올때가있음(상태가 바껴있는거같아서 여기서 다시한번 보내주기).
//            if(Const.CHATTING_IS_BACKGROUND){
//                ++squenceNum
//                socket?.emit(Const.CHAT_CHAANGE_STATE,
//                    JSONObject()
//                        .put("cmd", Const.CHAT_CHAANGE_STATE)
//                        .put("seq", squenceNum)
//                        .put("state", "BACKGROUND"))
//                Util.log("idoltalk::onAuthComplete go background")
//            } else{
//                ++squenceNum
//                socket?.emit(Const.CHAT_CHAANGE_STATE,
//                    JSONObject()
//                        .put("cmd", Const.CHAT_CHAANGE_STATE)
//                        .put("seq", squenceNum)
//                        .put("state", "FOREGROUND"))
//                Util.log("idoltalk::onAuthComplete go foreground")
//            }

            //수신되는 메시지들.
            if (!socket?.hasListeners(Const.CHAT_RECEIVE_MESSAGES)!!)
                socket?.on(Const.CHAT_RECEIVE_MESSAGES, onReceiveMessage)

            //시스템 커맨드.
            if (!socket?.hasListeners(Const.CHAT_SYSTEM_COMMAND)!!)
                socket?.on(Const.CHAT_SYSTEM_COMMAND, onSystemCommand)

            //시스템 메시지.
            if (!socket?.hasListeners(Const.CHAT_SYSTEM_MESSAGE)!!)
                socket?.on(Const.CHAT_SYSTEM_MESSAGE, onSystemMessage)

        } catch (e: Exception) {
            e.printStackTrace()
            val authSuccessSeq = args[0]
            Util.log("idoltalk::isAuthSuccess -> $authSuccessSeq")
            Util.log("idoltalk::isAuthSuccess -> error")
        }
    }

    //시스템 메시지 수신.(토스트 메시지, 치명적인 메시지)
    private val onSystemMessage = Emitter.Listener { args ->
        Util.log("idoltalk::systemMessage")
        try {
            val data = args[0] as JSONObject
            //BusEvent사용하여 채팅방안에다가 갑을 던져줌.
            EventBus.sendEvent(data, Const.CHAT_SYSTEM_MESSAGE)

        } catch (e: JSONException) {
            Util.log("idoltalk::onSystemMessage ERROR ${e.message}")
        }
    }

    // requestMessages, receiveMessages 응답 공통 처리
    private var onReceiveMessage = Emitter.Listener { args ->
        //여기는 그냥 디비에만 넣는것만 생각하면됨.
        Util.log("idoltalk::receivedMessage")

        try {
            val data = args[0] as JSONObject

            Util.log("idoltalk::$data")


            val responseRoomId = data.getInt("room_id")
            val responseCount = data.getInt("count")
            val ogRequest:JSONObject?= data.optJSONObject("origin_req")
            val responseMessageArray = data.getJSONArray("messages")
            val gson = IdolGson.getInstance(true)

            val obMessage = ArrayList<MessageModel>()
            val chatDB = ChatDB.getInstance(
                mContext!!,
                IdolAccount.getAccount(mContext)?.userId ?: 0
            )
            val isOgRequest = ogRequest != null && ogRequest.getString("cmd") == Const.CHAT_REQUEST_MESSAGES

            Logger.v("리스폰스 메세지 어레이 길이 ->"+responseMessageArray.length())
            try {
                for (i in 0 until responseMessageArray.length()) {
                    //서버로 부터 받은 메시지.
                    val obj = responseMessageArray.getJSONObject(i)

                    val receiveMessage =
                        gson.fromJson<MessageModel>(obj.toString(), MessageModel::class.java)
                    var isUpdateTsNotExist = obj.isNull("update_ts")//update ts 가 null 인지 여부

                    receiveMessage.roomId = responseRoomId

                    Util.log("idoltalk::current first status -> class ${mContext?.javaClass} roomId ${roomId} index ${i}")

                    if (receiveMessage.userId == userId) {
                        //내가 보낸 메시지 일때.

                        if (isOgRequest && receiveMessage.deleted) {
                            receiveMessage.contentType = MessageModel.CHAT_TYPE_TEXT
                        }

                        if (receiveMessage?.contentType == MessageModel.CHAT_TYPE_LINK) {
                            receiveMessage.isLinkUrl = true
                        }

                        if (!isOgRequest) {
                            EventBus.sendEvent(
                                JSONObject(gson.toJson(receiveMessage)),
                                Const.CHAT_RECEIVE_MESSAGES
                            )
                        }
                        continue
                    }

                    //현재 방안에없거나. 룸아이디가 다르면 노티를 띄워준다. null은 맨처음 앱이 시작할떄 roomId가 null로 되게 해놨음 (BaseActivity onResume확인).
                    //공통적으로 채팅푸시필터가  켜져있을때 노티를 띄어준다. ->  isChatPushFilterOff()가 false일때
                    if (((mContext?.javaClass != ChattingRoomActivity::class.java && receiveMessage.userId != IdolAccount.getAccount(
                            mContext
                        )?.userId) ||
                            (receiveMessage.roomId != roomId && receiveMessage.userId != IdolAccount.getAccount(
                                mContext
                            )?.userId) ||
                            (receiveMessage.roomId == roomId && receiveMessage.userId != IdolAccount.getAccount(
                                mContext
                            )?.userId && Const.CHATTING_IS_PAUSE) ||
                            roomId == null && receiveMessage.userId != IdolAccount.getAccount(
                            mContext
                        )?.userId) &&
                        !isChatPushFilterOff() &&
                        !obj.getString("content").isNullOrEmpty() &&
                        receiveMessage.reports == null &&
                        receiveMessage.userId != IdolAccount.getAccount(mContext)?.userId &&
                        ogRequest == null
                    ) {
                        Util.log("idoltalk::노티가 띄워졌습니다.")

                        showNotification(
                            receiveMessage,
                            responseMessageArray.getJSONObject(i),
                            chatDB,
                            obj
                        )

                        receiveMessage.isReadable = false

                    } else {
                        receiveMessage.isReadable = true
                    }

                    Util.log("idoltalk::current status -> class ${mContext?.javaClass} roomId ${roomId}")


                    receiveMessage.status = true
                    receiveMessage.statusFailed = false
                    receiveMessage.accountId = IdolAccount.getAccount(mContext)?.userId ?: 0

                    //내가보낸 메시지가 아닐때.
                    if (!receiveMessage.deleted || (isOgRequest && receiveMessage.deleted)) {

                        if (receiveMessage?.contentType == MessageModel.CHAT_TYPE_LINK) {
                            receiveMessage.isLinkUrl = true
                        }



                        if (isOgRequest && receiveMessage.deleted && (isUpdateTsNotExist || (receiveMessage?.contentType != MessageModel.CHAT_TYPE_TEXT))) {
                            isUpdateTsNotExist = true
                            receiveMessage.contentType = MessageModel.CHAT_TYPE_TEXT
                        }

                        //삭제된 메세지와  일반 메세지가 같이 올때 (request 할경우 생김) 이경우  ogrequest는 따로 업데이트 안해주므로,  여기서  삭제된 메세지를 걸러준다.
                        val deletedObMessage =
                            obMessage.find { it.serverTs == receiveMessage.serverTs && it.userId == receiveMessage.userId && it.deleted && !receiveMessage.deleted }

                        if (deletedObMessage == null) {
                            val reportMessage = chatDB?.ChatDao()?.getReportedMsg(
                                receiveMessage.userId,
                                receiveMessage.serverTs,
                                receiveMessage.roomId,
                                true
                            )

                            if (reportMessage != null && !reportMessage.reported) {
                                receiveMessage.content = reportMessage.content

                                chatDB.ChatDao().insertChatMessage(receiveMessage)

                                if (!isOgRequest) {
                                    EventBus.sendEvent(
                                        JSONObject(
                                            gson.toJson(
                                                receiveMessage
                                            )
                                        ), Const.CHAT_RECEIVE_MESSAGES
                                    )
                                }

                                if (isOgRequest) {
                                    if (receiveMessage.roomId == roomId) {
                                        val message =
                                            obMessage.find { it.serverTs == receiveMessage.serverTs && it.userId == receiveMessage.userId }
                                        if (message == null) {
                                            obMessage.add(receiveMessage)

                                            if (obMessage.size > 0 && i == responseMessageArray.length() - 1) {
                                                obMessage.last().isLastCount = true
                                            }

                                            if (obMessage.last().isLastCount) {//ogRequest가 있는 경우
                                                EventBus.sendEvent(
                                                    obMessage,
                                                    Const.CHAT_REQUEST_OG_MESSAGE
                                                )
                                            }
                                        }
                                    }
                                }


                            } else if (reportMessage == null) {

                                chatDB?.ChatDao()?.insertChatMessage(receiveMessage)

                                Util.log("idoltalk::insert db(onReceive) -> $receiveMessage ${responseMessageArray.length()}")
                                if (!isOgRequest) {

                                    try {
                                        EventBus.sendEvent(
                                            JSONObject(
                                                gson.toJson(
                                                    receiveMessage
                                                )
                                            ), Const.CHAT_RECEIVE_MESSAGES
                                        )
                                    } catch (e: Exception) {
                                        Logger.v("exception ->$e")
                                    }

                                }

                                if (isOgRequest) {

                                    if (receiveMessage.roomId == roomId) {
                                        val message =
                                            obMessage.find { it.serverTs == receiveMessage.serverTs && it.userId == receiveMessage.userId }
                                        if (message == null) {
                                            obMessage.add(receiveMessage)
                                            if (obMessage.size > 0 && i == responseMessageArray.length() - 1) {
                                                obMessage.last().isLastCount = true
                                            }

                                            if (obMessage.last().isLastCount) {//ogRequest가 있는 경우
                                                EventBus.sendEvent(
                                                    obMessage,
                                                    Const.CHAT_REQUEST_OG_MESSAGE
                                                )
                                            }
                                        }
                                    }
                                }

                            }
                        }
                        continue
                    }

                    chatDB?.ChatDao()?.updateChatRoomMessage(
                        receiveMessage.roomId,
                        receiveMessage.serverTs,
                        receiveMessage.userId,
                        true,
                        MessageModel.CHAT_TYPE_TEXT
                    )

                    Util.log("idoltalk::update db(onReceive) -> $receiveMessage ${responseMessageArray.length()}")
                    if (!isOgRequest) {
                        EventBus.sendEvent(
                            JSONObject(gson.toJson(receiveMessage)),
                            Const.CHAT_RECEIVE_MESSAGES
                        )
                    }

                    if (isOgRequest) {
                        //Logger.v("receieveveveeve ->"+receiveMessage)
                        if (receiveMessage.roomId == roomId) {
                            val message =
                                obMessage.find { it.serverTs == receiveMessage.serverTs && it.userId == receiveMessage.userId }
                            if (message == null)
                                obMessage.add(receiveMessage)
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.v("idol error ->" + e.message)
                e.printStackTrace()
            }

        } catch (e: JSONException) {
            Logger.v("idoltalk::onReceiveMessage ERROR ${e.message}")
        }
    }

    //로컬에서 받은 socket 내용을 노티로 만들때,
    //타입을 비교해서 Return 되는 content를  결정한다.
    private fun checkNotiContentsType(content:String?,contentType:String?): String? {

        // TODO: 2021/04/23 string 나오면  resource로  변경할것
        return when (contentType) {
            "text/vnd.exodus.image" -> {//이미지 일경우
                try {
                    val isEmoticon = JSONObject(content).optBoolean("is_emoticon",false)
                    if(isEmoticon){
                        mContext?.getString(R.string.chat_push_emoticon)
                    }else{
                        mContext?.getString(R.string.chatpush_image)
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                    mContext?.getString(R.string.chatpush_image)
                }
            }
            "text/vnd.exodus.video" -> {//비디오 일경우
                "비디오 입니다."
            }
            "text/vnd.exodus.link" -> {//링크일경우
                val gson = IdolGson.getInstance(true)
               "${gson.fromJson(content, ChatMsgLinkModel::class.java).originalMsg}"
            }
            else -> {
                content.toString()
            }
        }
    }

    //채팅 푸시가 꺼져있음을 판별하여,  true,false를  return 한다.
    private fun isChatPushFilterOff():Boolean{
        val idolAccount=IdolAccount.getAccount(mContext)
//        idolAccount.fetchUserInfo(mContext, null)

        //푸시 필터값 64 이상이면 최소  채팅알림은 푸시가 꺼졌음을  의미한다.
        return (idolAccount?.userModel?.pushFilter ?: 0) >= 64
    }

    //시스템 커맨드 수신.(커맨드마다 분기됨).
    private val onSystemCommand = Emitter.Listener {args ->
        Util.log("idoltalk::systemCommand")
        try {
            val data = args[0] as JSONObject
            Util.log("idoltalk::$data")

            Util.log("idoltalk::content_type->${data.getString("content_type")}")
            Util.log("idoltalk::content->${data.getString("content")}")

            val contentType = data.getString("content_type")
            val content = data.getJSONObject("content")
            val type = content.getString("type")
            //TODO:: 현재 나가기 했을때 user_id가 안오고있음.
//            val roomId = JSONObject(content).getString("room_id")
//            val userId = JSONObject(content).getString("user_id")

            Util.log("idoltalk::type->${type}")

            checkSystemMessage(type, content, data)

        } catch (e: JSONException) {
            Util.log("idoltalk::systemCommand ERROR ${e.message}")
        }

    }

    //시스템 메시지 분기.
    private fun checkSystemMessage(type: String, content: JSONObject, data: JSONObject) {

        var roomId: Int? = null

        when(type){
            //인증 실패, 혹은 강제 재인증.
            "RETRY_AUTH" -> {
                sendAuth()
            }
            "LEAVE_ROOM" -> {
                roomId = content.getInt("room_id")
                if(mContext?.javaClass != ChattingRoomActivity::class.java){
                    UtilK.deleteChatNotification(roomId, mContext!!)
                }
                EventBus.sendEvent(data, Const.CHAT_SYSTEM_COMMAND)
            }
            //방정보 변경됨.
            "UPDATE_ROOMINFO" -> {
                EventBus.sendEvent(data, Const.CHAT_SYSTEM_COMMAND)
            }
            //사용자 입장/퇴장이 일어남.
            "ADD_JOINS" -> {
                EventBus.sendEvent(data, Const.CHAT_SYSTEM_COMMAND)
            }
            "DELETE_JOINS" -> {
                EventBus.sendEvent(data, Const.CHAT_SYSTEM_COMMAND)
                //서버에서 강퇴 처리할때 updateDeletedMember 두번 불려서 deadlock 되는 현상때문에 일단 주석 처리
//                roomId = content.getInt("room_id")
//                userId = content.getInt("user_id")
//                //Delete할 멤버 userId, roomId로찾는다.
//                ChatMembersList.getInstance(mContext!!).updateDeletedMember(userId, roomId) {
//                    Util.log("idoltalk::members feield deleted됐습니다.")
//                    Util.log("idoltalk::지워진 멤버의 userId ${userId}")
//                    RxBus.instance.sendEvent(data,Const.CHAT_SYSTEM_COMMAND)
//                }
            }
            "UPDATE_USER" -> {
                EventBus.sendEvent(data, Const.CHAT_SYSTEM_COMMAND)
            }
        }

    }

    private fun sendAuth(){
        val account = IdolAccount.getAccount(mContext)
        val ts = System.currentTimeMillis()
        if (account != null) {
            val credential = "${account.email}:${account.domain}:${account.token}"
            val encodedCredential =
                "${Base64.encodeToString(credential.toByteArray(), Base64.NO_WRAP)}"

            Util.log("idoltalk::credential $encodedCredential")

            ++squenceNum

            socket?.emit(Const.CHAT_AUTH,
                JSONObject().put("cmd", Const.CHAT_AUTH)
                    .put("seq", squenceNum)
                    .put("client_ts", ts)
                    .put("authtoken", encodedCredential))

            if (!socket?.hasListeners(Const.CHAT_AUTH_COMPLETE)!!)
                socket?.on(Const.CHAT_AUTH_COMPLETE, onAuthComplete)
            if (!socket?.hasListeners(Const.CHAT_AUTH_FAILED)!!) {
                socket?.on(Const.CHAT_AUTH_FAILED, onAuthFailed)
            }

            //Util.log("idoltalk::socket is ${socket?.isActive}")
            Util.log("idoltalk::socket connected")
        }
    }

    private fun showNotification(
        receiveMessage: MessageModel,
        jsonObject: JSONObject,
        chatDB: ChatDB?,
        obj: JSONObject
    ) {
        val notificationId = ID_CHATTING_MSG + receiveMessage.roomId
        val notificationIntent = PushStartActivity.createChattingIntent(
            mContext,
            jsonObject,
            receiveMessage.roomId,
            -1,
            ""
        )
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {//12버전에서는 flag mutable 추가
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }

        val bundle = if (Build.VERSION.SDK_INT >= 35) {
            val options = android.app.ActivityOptions.makeBasic()
            options.pendingIntentBackgroundActivityStartMode = android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_SYSTEM_DEFINED
            options.toBundle()
        } else {
            null
        }

        val contentIntent = PendingIntent.getActivity(
            mContext,
            Random.nextInt(),
            notificationIntent,
            flags,
            bundle
        )

        val channel = Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW
        val manager =
            mContext!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder: NotificationCompat.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationCompat.Builder(mContext!!, channel)
            } else {
                NotificationCompat.Builder(mContext!!)
            }

        //유저 nickName표시용.
        val fetchedRoomMember = chatDB?.ChatMembersDao()
            ?.getChatRoomMemberForNotification(
                receiveMessage.userId,
                receiveMessage.roomId
            )

        val fetchedChatRoom = chatDB?.ChatRoomListDao()
            ?.getChatRoomForNotification(receiveMessage.roomId)

        //메`세지 내용
        val content = checkNotiContentsType(
            receiveMessage.content,
            receiveMessage.contentType
        )

        //보낸 사람 닉네임  socket message 에 없을때  로컬 db 에서 추출
        val senderNickname =
            if (obj.optString("sender_nickname", "").isNullOrEmpty()) {
                fetchedRoomMember?.nickname
            } else {
                obj.getString("sender_nickname")
            }

        //방제목  ->socket message 에 없을때는 기존처럼  로컬 db에서 추출
        val roomTitle = if (obj.optString("room_title", "").isNullOrEmpty()) {
            fetchedChatRoom?.title
        } else {
            obj.getString("room_title")
        }

        //일반  채팅 notification builder
        val notiGroup = Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW
        builder.setContentIntent(contentIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderNickname)
            .setSubText(roomTitle)
            .setContentText(content)
            .setVibrate(null)
            .setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + mContext!!.packageName + "/" + R.raw.no_sound))
            .setColor(ContextCompat.getColor(mContext!!, R.color.main))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setGroup(notiGroup)

        //채팅 notification들을  그룹핑 하는  group 용 notification builder
        val summary = NotificationCompat.Builder(mContext!!, channel)
            .setContentTitle(senderNickname)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setSubText(mContext!!.getString(R.string.chat_unread_message))
            .setColor(ContextCompat.getColor(mContext!!, R.color.main))
            .setGroup(notiGroup)
            .setVibrate(null)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setGroupSummary(true)

        // 내용 길 경우 펼칠 수 있는 노티
        val bigTextStyle = NotificationCompat.BigTextStyle()
        bigTextStyle.setBigContentTitle(senderNickname)
        bigTextStyle.bigText(content)
        builder.setStyle(bigTextStyle)

        manager.notify(notificationId, builder.build())
        manager.notify(Const.NOTIFICATION_GROUP_ID_CHAT_MSG, summary.build())
    }

    companion object{

        @Volatile
        private var instance: SocketManager? = null

        @JvmStatic
        fun getInstance(context: Context, roomId: Int?, userId: Int?): SocketManager {
            if (instance == null) {
                synchronized(this) {
                    instance = SocketManager()
                }
            }
            instance!!.mContext = context
            instance!!.roomId = roomId
            instance!!.userId = userId
            return instance!!
        }
   }

}
