package net.ib.mn.liveStreaming

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.model.LiveChatMessageModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


/**
 * ProjectName: idol_app_renew
 *
 * Description:  라이브 화면에서 사용되는  소켓 관련 코드
 * 소켓 connect부터 disconnect까지 여기서 관리되며,
 * inteface를 통해  뷰 업데이트가 필요한 경우  업데이트 진행한다.
 * */
class LiveSocketManager {

    //Socket
    var socket: Socket? = null
    var isSocketConnected: Boolean = false
    private var liveId:Int? = null
    private var activity:Context? = null

    private var authFailCount = 0;

    lateinit var updateLive: UpDateLiveListener
    private val listType = object : TypeToken<List<LiveChatMessageModel>>() {}.type
    //SequenceNumber
    var sequenceNum = 1

    private val gson = IdolGson.getInstance(true)
    //차단된 유저 ArrayList
    private var reportedChatList = ArrayList<LiveChatMessageModel>()

    var dispatcher: Dispatcher? = null

    fun createSocket() {
        try {
            dispatcher = Dispatcher()
            val okHttpClient = OkHttpClient.Builder()
                .dispatcher(dispatcher!!)
                .readTimeout(1, TimeUnit.MINUTES)
                .build()

            val liveUrl = ConfigModel.getInstance(activity).live_url
            val parse = Uri.parse(liveUrl)

            val options = IO.Options()
            options.transports = arrayOf("websocket")
            options.secure = ServerUrl.HOST.startsWith("https")
            options.path = "/live/${liveId}/"
            options.reconnection = true
            options.reconnectionAttempts = Int.MAX_VALUE
            options.reconnectionDelay = 1_000
            options.reconnectionDelayMax = 5_000
            options.randomizationFactor = 0.5
            options.timeout = 20_000


            //Authorization헤더.
            //extraHeaders 보면 <String,List<String>> 확인.
            val headers = HashMap<String, List<String>>()
            val account = IdolAccount.getAccount(activity)
            val creds = account?.email + ":" + account?.domain + ":" + account?.token
            val authHeader = "Basic ${Base64.encodeToString(creds.toByteArray(), Base64.NO_WRAP)}"
            headers["Authorization"] = listOf(authHeader)
            headers["User-Agent"] = listOf(System.getProperty("http.agent") + " (" + activity?.applicationInfo?.packageName + "/" + activity?.getString(
                R.string.app_version) + ")")
            headers["X-HTTP-NATION"] = listOf(Util.getSystemLanguage(activity))
            options.extraHeaders = headers
            Logger.v("IDOL_LIVE","socket option set")

            options.callFactory = okHttpClient
            options.webSocketFactory = okHttpClient

            //socket 널체크해준다 여기서 다시연결될때 socket 인스턴스가 여러개 생성됨.
            if (socket == null){
                Logger.v("IDOL_LIVE","socket is null ->socket 인스턴스 생성 진행 ")
                socket = IO.socket(URI.create(parse.scheme + "://" + parse.host), options)
            }

            Logger.v("IDOL_LIVE","socket is created")
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    //라이브 업데이트 리스너 set
    fun setUpdateLiveListener(updateLive: UpDateLiveListener){
        this.updateLive = updateLive
    }


    //라이브 관련 데이터 업데이트 사항을 전달하기 위한 리스너
    interface UpDateLiveListener{
        fun updateLiveInfo(updatedLiveInfo: JSONObject)//라이브 정보 업데이트 (5초마다)
        fun updateLiveToken(updatedToken: JSONObject)//라이브 토큰 업데이트(1분마다)
        fun broadCastSystemCommand(type:String,liveId: Int?)//라이브 시스템 command관련해서 알림.
        fun broadCastAuthFailed()//auth fail이 난 경우  불러준다. 총 5번 진행.
        fun receiveChatMessage(liveStreamChatMessageModel: LiveChatMessageModel)
    }



    //소켓연결.
    fun connectSocket() {
        Logger.v("IDOL_LIVE","connectSocket")
        if (!isSocketConnected) socket?.on(Socket.EVENT_CONNECT, onConnect)
        if (!socket?.hasListeners(Socket.EVENT_DISCONNECT)!!)
             socket?.on(Socket.EVENT_DISCONNECT, onDisconnect)
        if (!socket?.hasListeners(Socket.EVENT_CONNECT_ERROR)!!)
             socket?.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
        if (!socket?.hasListeners(LIVE_UPDATE_INFO)!!)
             socket?.on(LIVE_UPDATE_INFO,getLiveInfoMessage)
        if (!socket?.hasListeners(LIVE_UPDATE_TOKEN)!!)
             socket?.on(LIVE_UPDATE_TOKEN,getLiveToken)
        if (!socket?.hasListeners(LIVE_RECEIVE_MESSAGES)!!)
            socket?.on(LIVE_RECEIVE_MESSAGES,getChattingMessages)

        socket!!.connect()
    }


    private val getChattingMessages = Emitter.Listener { args ->
        val chattingMessages = args[0] as JSONObject
        Logger.v("IDOL_LIVE","chat messages  ->"+chattingMessages)

        //LiveChatMessageModel list 로 받아온 메세지들 넣어줌.
        //일반적으로는  주고 받으면 메세지 한개 옴. 하지만, 맨처음 들어올시는 30개씩 옴으로 리스트로 처리함.
        val liveChatMessageList: ArrayList<LiveChatMessageModel> =
            gson.fromJson(chattingMessages.getJSONArray("messages").toString(), listType)

        if(liveChatMessageList.size>1){//메세지가 1개 초과일때, serverts로 보여지게 한번더 정렬
            liveChatMessageList.sortBy { it.serverTs }
        }

        //리포트 채팅 리스트  값  있는 경우  reportedChatList에 넣어줌.
        if(Util.getPreference(activity,KEY_SHARED_LIVE_REPORT_LIST+liveId).isNotEmpty()){
            //차단된 유저 array로 저장 가져옴.
            reportedChatList = gson.fromJson(Util.getPreference(activity, KEY_SHARED_LIVE_REPORT_LIST+liveId).toString(), listType)
        }


        for (i in 0 until liveChatMessageList.size) {//receive메세지에 있는 메세지들을 순서대로 보내준다.

            //저장된 리포트 메세지들을  체크해서  보내려는  메세지가  리포트 저장된  메세지와 동일한 경우 isReported true를 적용해준다.
            // > 30개 받아올때  내가 신고한거 체크하기위해..
            // deleted  true가 아닌  조건이 들어간 이유는 이미 삭제된 메세지의 경우는  신고 메세지로 안바꿔주기 위해서..
            reportedChatList.apply {
                if(this.any {
                    it.userId == liveChatMessageList[i].userId && it.serverTs == liveChatMessageList[i].serverTs && liveChatMessageList[i].deleted != true
                }){
                    liveChatMessageList[i].isReported = true
                }
            }

            updateLive.receiveChatMessage(liveChatMessageList[i])
        }
    }


    //서버로부터  5초 주기로  라이브 정보를 수신한다.
    private val getLiveInfoMessage = Emitter.Listener { args ->
        val liveInfo = args[0] as JSONObject
        updateLive.updateLiveInfo(liveInfo.getJSONObject("content"))
        Logger.v("IDOL_LIVE","message -> ${liveInfo.getJSONObject("content")}")

    }

    //서버로부터 1분 주기로 라이브 토큰을 수신한다.
    private val getLiveToken = Emitter.Listener { args ->
        val liveToken = args[0] as JSONObject
        updateLive.updateLiveToken(liveToken)
        Logger.v("IDOL_LIVE", "live token -> $liveToken")
    }

    //disconnect 실행
    private val onDisconnect = object : Emitter.Listener {
        override fun call(vararg args: Any) {
            isSocketConnected = false
            socket?.off(Socket.EVENT_DISCONNECT, this)
            Logger.v("IDOL_LIVE","socket disconnected")
            Logger.v("IDOL_LIVE","socket is " + socket?.connected())
        }
    }

    //소켓 연결 완료.
    private val onConnect = Emitter.Listener {
        Logger.v("IDOL_LIVE","socket onConnect")

        //auth complete failed 관련  리스너 달아줌.
        if (!socket?.hasListeners(LIVE_AUTH_COMPLETE)!!){
            socket?.on(LIVE_AUTH_COMPLETE, onAuthComplete)
        }
        if (!socket?.hasListeners(LIVE_AUTH_FAILED)!!) {
            socket?.on(LIVE_AUTH_FAILED, onAuthFailed)
        }

        //connection 할때  이미 auth보내서 주석처리 함.
        //auth fail이거나  systemcommand로  auth 요청 값  왔을때  만  사용한다.
        //sendAuth()
    }


    //연결 에러.
    private val onConnectError = Emitter.Listener { args ->
        Logger.v("IDOL_LIVE","socket Error is " + socket?.connected())
        Logger.v("IDOL_LIVE","socket Error reason " + args[0])
        val obj = JSONObject().put("connected", socket?.connected())

        //다시 소켓 커넥트 실행
        connectSocket()

        isSocketConnected = false
    }

    //인증실패.
    private val onAuthFailed = Emitter.Listener { args ->
        val authFailedSeq = args[0]
        Logger.v("IDOL_LIVE","isAuthFailed -> $authFailedSeq")

        authFailCount ++

        //5번 초과 시도면  authfail 에따른 로직 실행 안함.
        if(authFailCount<=5) {
            updateLive.broadCastAuthFailed()
        }

        //auth 인증 실패시  한번더  보냄.
        //sendAuth()

        isSocketConnected = false

    }


    //인증 성공 확인.
    private val onAuthComplete = Emitter.Listener { args ->
        try {
            val authSuccessSeq = args[0] as JSONObject
            Logger.v("IDOL_LIVE","isAuthSuccess -> $authSuccessSeq")
            val obj = JSONObject().put("connected", socket?.connected())

            //auth 성공이면  다시  failcount 0으로 변경해준다.
            authFailCount = 0

            //시스템 커맨드.
            if (!socket?.hasListeners(LIVE_SYSTEM_COMMAND)!!)
                socket?.on(LIVE_SYSTEM_COMMAND, onSystemCommand)

            //시스템 메시지.
            if (!socket?.hasListeners(LIVE_SYSTEM_MESSAGE)!!)
                socket?.on(LIVE_SYSTEM_MESSAGE, onSystemMessage)

            //socket 연결됨 true로
            isSocketConnected = true

        } catch (e: Exception) {
            e.printStackTrace()
            val authSuccessSeq = args[0]
            Logger.v("IDOL_LIVE","isAuthSuccess -> $authSuccessSeq")
            Logger.v("IDOL_LIVE","isAuthSuccess -> error")
        }
    }


    //시스템 커맨드 수신.(커맨드마다 분기됨).
    private val onSystemCommand = Emitter.Listener { args ->
        try {
           Logger.v("IDOL_LIVE","onSystemCommand"+args[0])
            val data = args[0] as JSONObject
            val content = data.getJSONObject("content")
            val type = content.getString("type")
            val liveId = content.getInt("live_id")
            updateLive.broadCastSystemCommand(type,liveId)//livestreamingActivity로 값 보냄.

        }catch (e:Exception){
            Logger.v("IDOL_LIVE","onSystemCommand ERROR ${e.message}")
        }

    }



    //시스템 메세지 수신 -> type toast이면 toast로  fatal 이면  팝업 창으로 보여준다.
    private val onSystemMessage = Emitter.Listener { args ->
        Logger.v("IDOL_LIVE","systemMessage")
        try {
            val data = args[0] as JSONObject

        Logger.v("IDOL_LIVE", "systemMessage data ->  $data")

        } catch (e: JSONException) {
            Logger.v("IDOL_LIVE","onSystemMessage ERROR ${e.message}")
        }
    }


    //인증 요청을 보냄.
    fun sendAuth(){
        val account = IdolAccount.getAccount(activity)
        val ts = System.currentTimeMillis()
        if (account != null) {
            val credential = "${account.email}:${account.domain}:${account.token}"
            val encodedCredential =
                "${Base64.encodeToString(credential.toByteArray(), Base64.NO_WRAP)}"

            ++sequenceNum

            socket?.emit(LIVE_AUTH,
                JSONObject().put("cmd", LIVE_AUTH)
                    .put("seq", sequenceNum)
                    .put("client_ts", ts)
                    .put("authtoken", encodedCredential))

            //auth complete failed 관련  리스너 달아줌.
            if (!socket?.hasListeners(LIVE_AUTH_COMPLETE)!!){
                socket?.on(LIVE_AUTH_COMPLETE, onAuthComplete)
            }
            if (!socket?.hasListeners(LIVE_AUTH_FAILED)!!) {
                socket?.on(LIVE_AUTH_FAILED, onAuthFailed)
            }

            Util.log("idoltalk::socket is ${socket?.isActive}")
            Util.log("idoltalk::socket connected")
        }
    }



    //소켓 disconnect 진행
    fun disconnectSocket() {
        socket?.disconnect()
        dispatcher?.executorService?.shutdown()
        isSocketConnected = false
        Logger.v("IDOL_LIVE","socket disconnect")
        socket?.off(LIVE_AUTH_COMPLETE, onAuthComplete)
        socket?.off(LIVE_AUTH_FAILED, onAuthFailed)
        socket?.off(LIVE_UPDATE_INFO, getLiveInfoMessage)
        socket?.off(LIVE_UPDATE_TOKEN,getLiveToken)
        socket?.off(Socket.EVENT_CONNECT, onConnect)
        socket?.off(Socket.EVENT_CONNECT_ERROR, onConnectError)
    }

    //socketmanger intance 해제
    fun destroyInstance() {
        instance = null
    }



    companion object{

        const val LIVE_AUTH_COMPLETE = "authComplete"
        const val LIVE_UPDATE_INFO = "updateLiveinfo"
        const val LIVE_UPDATE_TOKEN = "updateToken"
        const val LIVE_AUTH_FAILED = "authFailed"
        const val LIVE_AUTH = "auth"
        const val LIVE_SYSTEM_COMMAND = "systemCommand"
        const val LIVE_SYSTEM_MESSAGE = "systemMessage"
        const val LIVE_RECEIVE_MESSAGES = "receiveMessages"
        const val LIVE_REQUEST_MESSAGES = "requestMessages"
        const val LIVE_SEND_MESSAGES = "sendMessage"
        const val LIVE_CHAT_TYPE_TEXT = "text/plain"
        const val LIVE_DELETE_MESSAGE = "requestDelete"
        const val LIVE_REPORT_MESSAGE = "reportMessage"

        const val KEY_SHARED_LIVE_REPORT_LIST ="reportedLiveList"

        private var instance: LiveSocketManager? = null

        @JvmStatic
        @Synchronized
        fun getInstance(liveId:Int?,activity: Context?): LiveSocketManager {
            if (instance == null) {
                synchronized(this) {
                    instance = LiveSocketManager()
                }
            }
            instance!!.activity = activity
            instance!!.liveId = liveId
            return instance!!
        }


    }

}