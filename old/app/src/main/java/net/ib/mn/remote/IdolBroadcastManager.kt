package net.ib.mn.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.koushikdutta.async.AsyncDatagramSocket
import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.callback.CompletedCallback
import com.koushikdutta.async.callback.DataCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ib.mn.BuildConfig
import net.ib.mn.common.util.logD
import net.ib.mn.common.util.logE
import net.ib.mn.common.util.logI
import net.ib.mn.common.util.logV
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.domain.usecase.GetIdolsByIdsUseCase
import net.ib.mn.domain.usecase.UpsertIdolsWithTsUseCase

import net.ib.mn.idols.IdolApiManager
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toDomain
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.Const
import net.ib.mn.utils.EventBus
import net.ib.mn.utils.GlobalVariable
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.timer

@Singleton
class IdolBroadcastManager @Inject constructor(){
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    @Inject
    lateinit var idolApiManager: IdolApiManager

    @Inject
    lateinit var getIdolsByIdsUseCase: GetIdolsByIdsUseCase

    @Inject
    lateinit var upsertIdolsWithTsUseCase: UpsertIdolsWithTsUseCase

    private val updatingNotExistingIds = ConcurrentHashMap.newKeySet<Int>()
    private var syncToken = Object() // ConcurrentModificationException 방지용
    private var mutex = Mutex()
    val TAG = "IdolBroadcast"
    val version = 3
    private val idols : ConcurrentHashMap<Int, IdolModel> = ConcurrentHashMap() // idol id : Pair<hearts,info_ver>
    private var socket : AsyncDatagramSocket? = null
    private var lastTs : Int = 0
    // 서버에서 UDP 처음 연결이 소켓을 바꾸라고 알려주는데 처음 바꿀 때는 굳이 디비 업데이트 할 필요 없음 (처음 reconnect 때만 안 하도록 추가)
    private var connectCount = 0
    var userId = 0
    var host = if(BuildConfig.CELEB) "myloveactor.com" else "myloveidol.com"
    private var port = 9413
    // routing host
    private var lbHost = host
    private var lbPort = port
    private var timerHeartbeat : Timer? = null
    var context : Context? = null
    private var lastOverallSeq : UShort = 0u // 마지막으로 받은 overall-seq. 패킷 누락 확인용
    private var updatingAll = false // 전체 아이돌 갱신중에는 udp 패킷을 무시하게
    private var retryTimer : Timer? = null
    private val interval : Long = 10

    private var connectionJob: Job? = null

    private val endCallback = CompletedCallback {
        Log.i(TAG, "=== endCallback")
        logE("=== endCallback")
        retryConnection()
    }
    private val dataCallback = DataCallback { _, bb ->
        val msg = bb.allByteArray
        val bytes = ByteBuffer.wrap(msg)
        Log.i(TAG, "=== bb="+msg.size)

        if(updatingAll) {
            Log.i(TAG, "=== updating all is in progress. skip.");
            return@DataCallback
        }

        scope.launch {
            parse(bytes, msg.size)
        }
    }

    fun setupConnection(ctx: Context, url : String ) {
         CoroutineScope(Dispatchers.IO).launch {
            try{
                context = ctx
                val uri = Uri.parse(url)
                host = uri.host!!
                port = uri.port

                lbHost = host
                lbPort = port

                connect()
                startHeartbeat()
            }catch (e:NullPointerException){
                e.printStackTrace()
            }
        }
    }

    // routing server부터 다시 접속
    fun reinitConnection() {
         CoroutineScope(Dispatchers.IO).launch {
            socket?.disconnect()
            host = lbHost
            port = lbPort
            connect()
        }
    }

    fun connect() {
        connectCount++

        connectionJob?.cancel()
        connectionJob = scope.launch(Dispatchers.IO) {
            try {
                // 소켓 생성 및 연결 (AsyncServer 예시)
                val server = AsyncServer.getDefault()
                socket = server.connectDatagram(host, port)
                socket?.dataCallback = dataCallback
                socket?.closedCallback = endCallback

                // 1초 딜레이 후 send(true) 호출
                delay(1000L)
                if (connectCount != 2) {
                    send(true)
                } else {
                    send(false)
                }

                // 필요에 따라 추가 작업을 여기에 작성할 수 있습니다.
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // udp 연결이 끊긴 경우 재연결 시도
    private fun retryConnection() {
        retryTimer = timer(period = interval * 1000, initialDelay = interval * 1000) {
            if(socket?.isOpen == false) {
                connect()
            } else {
                retryTimer?.cancel()
            }
        }
    }

    // 수신 유지를 위한 heartbeat 전송
    fun startHeartbeat() {
        timerHeartbeat?.cancel()
        timerHeartbeat = timer(period = 30000, initialDelay = 30000) {
            Log.i(TAG, "=== 30 sec timer fired. Send request.")
            send()
        }
    }
    // 30마다 보내는 heartbeat를 멈춤
    fun stopHeartbeat() {
        timerHeartbeat?.cancel()
    }

    fun disconnect() {
        connectionJob?.cancel()
        scope.launch(Dispatchers.IO) {
            stopHeartbeat()
            socket?.close()
            socket?.disconnect()
        }
    }

    fun send( requestAll : Boolean = false ) {
//        Log.i(TAG, "=== send Thread:"+Thread.currentThread().name)
        // version
        var ba = byteArrayOf(version.toByte())
        // 전체 아이돌 요청인지?
        ba += if( requestAll ) {
            byteArrayOf(0.toByte())
        } else {
            byteArrayOf(1.toByte())
        }
        // timestamp
        val ts : Int = (System.currentTimeMillis() / 1000).toInt()
        // network byte order
        var buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(ts)
        ba += buffer.array()

        // last timestamp
        buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(lastTs)
        ba += buffer.array()

        // user id
        buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(userId)
        ba += buffer.array()

        socket?.send(host, port, ByteBuffer.wrap(ba))

        if(BuildConfig.DEBUG) {
            val hex = bytesToHex(ba)
            Log.i(TAG, "=== sent $hex")
        }
    }

    suspend fun parse(bb : ByteBuffer, size: Int ) {
        logV("=== Call Parse")

        mutex.withLock {
            idols.clear()
            try {
                idolApiManager.setLastLocalTs((System.currentTimeMillis() / 1000).toInt())

                var pos = 0
                val ver = bb[pos].toUByte().toInt()

                // version이 내 버전보다 높으면 그만두자
                if (version < ver && context != null) {
                    ConfigModel.getInstance(context).udp_stage = 0
                    timerHeartbeat?.cancel()
                    return
                }

                pos += 1
                // type 처리
                val isAll = bb[pos].toUByte().toInt() == 0
                val type = bb[pos].toInt()
                when( type ) {
                    2 -> { // re-route (ip:port)
                        Log.i(TAG, "=== packet type 2")
                        host = "${bb[pos+1].toUByte()}.${bb[pos+2].toUByte()}.${bb[pos+3].toUByte()}.${bb[pos+4].toUByte()}"
                        port = bb[pos+5].toUByte().toInt().shl(8) or bb[pos+6].toUByte().toInt()
                        Log.i(TAG, "=== re-route host=$host port=$port")
                        socket?.disconnect()
                        connect()
                        return
                    }
                    3 -> { // re-route (port)
                        Log.i(TAG, "=== packet type 3")
                        port = bb[pos+1].toUByte().toInt().shl(8) or bb[pos+2].toUByte().toInt()
                        Log.i(TAG, "=== re-route host=$host port=$port")
                        socket?.disconnect()
                        connect()
                        return
                    }
                }

                pos += 1
                // timestamp
                val ts = getLong(bb, pos).toInt()
                // 서버에서 날짜가 바뀌었는지 검사
                checkDayChange(ts)
                lastTs = ts
                pos += 4
                // overall-seq
                val overallSeq = (bb[pos].toUByte().toInt().shl(8) or bb[pos+1].toUByte().toInt()).toUShort()
                val nextSeq = (lastOverallSeq + 1u).toUShort() // overflow 방지
                if( nextSeq != overallSeq && !isAll ) {
                    logE("패킷 누락 $nextSeq $overallSeq $isAll")
                    // 패킷 누락. 다시 전체 요청. 전체 아이돌 패킷 아니고 부분 패킷일때만.
                    send(true)
                    // 30초 타이머 다시 시작
                    startHeartbeat()
                }
                lastOverallSeq = overallSeq
                pos += 2
                // 전체 패킷 수
                val total = bb[pos].toUByte().toInt()
                pos += 1
                // 시퀀스
                val seq = bb[pos].toUByte().toInt()
                pos += 1

//            Log.i(TAG, "=== ver="+ver)
//            Log.i(TAG, "=== ts="+lastTs)
//            Log.i(TAG, "=== total="+total)
//            Log.i(TAG, "=== seq="+seq)
                Log.i(TAG, "=== overallSeq=$overallSeq")
                // 전체 패킷을 받은 경우라면 전송요구 패킷을 바로 보낸다
                if( isAll && seq == total - 1 ) {
                    send()
                }

                var lastId = 0
                while( pos < size ) {
                    // id
                    val idPair = getDec(bb, pos)
                    val id = idPair.first
                    pos += idPair.second

                    // heart
                    val votesPair = getDec(bb, pos)
                    val votes = votesPair.first
                    pos += votesPair.second

                    // infover
                    val infoVer = bb[pos].toUByte().toInt()
                    pos++

                    lastId += id.toInt()

                    val idol = IdolModel(lastId, 0)

                    // 23.06.09 top3-ver, addition-count 추가
                    // https://exodusent.atlassian.net/wiki/spaces/MYL/pages/490602505/UDP+packet+version+3#클라이언트가-보내는-패킷
                    /**
                     * 1 byte: top3-ver: top3 정보 버전이다. 이 값은 0~255 중 하나의 값을 가지며, 클라이언트는 이 값을 저장해 두어야한다. 다른 방송 패킷에서 이 값이 달라지면 해당 패킷에서 보내준 (compact-top3) 로 업데이트 하거나, 기존 방식으로 top3 정보를 가져와야 한다.
                     * 1 byte: addition-count: 후에 있는 addition 정보 리스트의 개수이다. 1~255 중 하나의 값을 가진다. 클라이언트는 이 값 만큼의 addition 정보를 읽어야 한다.
                     */
                    val top3Ver = bb[pos].toUByte().toInt()
                    pos++

                    val additionCount = bb[pos].toUByte().toInt()
                    pos++

                    // addition count만큼 반복한다
                    repeat( additionCount ) {
                        // addition-type
                        // 0: addition 없음, 1: compact-top3. info-ver의 변경중에 top3만 변경될 경우에만 1이다.
                        val additionType = bb[pos].toUByte().toInt()
                        pos++

//                    Log.i(TAG, "=== id:$lastId addtionType:$additionType")
                        val additionLength = bb[pos].toUByte().toInt()
                        pos++

                        if( additionType == 1 ) {
                            var p = pos
                            // addition : compact top3
                            val top1 = getLong(bb, p)
                            p += 4
                            val type1 = bb[p].toInt().toChar()
                            p += 1
                            val top2 = getLong(bb, p)
                            p += 4
                            val type2 = bb[p].toInt().toChar()
                            p += 1
                            val top3 = getLong(bb, p)
                            p += 4
                            val type3 = bb[p].toInt().toChar()

                            idol.top3 = "$top1,$top2,$top3"
                            idol.top3Type = "$type1,$type2,$type3"
                            Log.i(TAG, "=== additionType=1 top3=${idol.top3}")
                        }

                        if( additionType == 2 ) {
                            if( pos + additionLength > size ) {
                                logE("=== size mismatch!!! [1] pos=$pos additionLength=$additionLength size=$size")
                                return
                            }

                            // addition : top3-image-ver
                            val p = pos
                            val imageVer1 = bb[p].toUInt()
                            val imageVer2 = bb[p+1].toUInt()
                            val imageVer3 = bb[p+2].toUInt()

                            idol.top3ImageVer = "${imageVer1},${imageVer2},${imageVer3}"
                            Log.i(TAG, "=== additionType=2 top3ImageVer=${idol.top3ImageVer}")
                        }

                        pos += additionLength
                    }

                    idol.heart = votes
                    idol.infoSeq = infoVer
                    idol.top3Seq = top3Ver

                    idols[lastId] = idol
                }

                // DB에 업데이트
                val notExistingIdolsIds = ArrayList<Int>() // DB에 없는 아이돌
                val updatedIdols = ArrayList<IdolModel>()// heart, top3만 바뀐 아이돌들
                val updatedInfoVerIdols = ArrayList<Int>()  // 그 외 info_ver 변경되는 경우

                val keyList = synchronized(syncToken) { HashMap(idols).keys.toList() }

                val context = context ?: return
                scope.launch(Dispatchers.IO) {
                    val dbIdols = mutex.withLock {
                        getIdolsByIdsUseCase(keyList)
                            .mapListDataResource { it.toPresentation() }
                            .awaitOrThrow()
                    }

                    val idolMap = dbIdols?.associateBy { it.getId() } ?: return@launch

                    for ((id, incomingData) in idols.entries) {
                        val originalIdol = idolMap[id]
                        val idol = originalIdol?.copy()

                        if (idol == null) {
                            // DB에 없는 아이돌
                            notExistingIdolsIds.add(id)
                            continue
                        }

                        // info_ver가 변경되었다면
                        if (idol.infoSeq != incomingData.infoSeq) {
                            Log.i(TAG, "=== info_ver updated ${idol.getName()} ${idol.infoSeq} → ${incomingData.infoSeq}")
                            updatedInfoVerIdols.add(idol.getId())
                            continue
                        }

                        // top3_ver 변경된 경우는 update해야 함
                        if (idol.top3Seq != incomingData.top3Seq) {
                            Log.i(TAG, "=== top3_ver updated ${idol.getName()} ${idol.top3Seq} → ${incomingData.top3Seq}")
                            updatedInfoVerIdols.add(idol.getId())
                            continue
                        }
                        // top3_image_ver 변경된 경우는 update해야 함
                        if (idol.top3ImageVer != incomingData.top3ImageVer) {
                            Log.i(TAG, "=== top3_image_ver updated ${idol.getName()} ${idol.top3ImageVer} → ${incomingData.top3ImageVer}")
                            updatedInfoVerIdols.add(idol.getId())
                            continue
                        }

                        // top3, heart가 변경된 경우 처리 (top3가 없으면 udp로 받은 건 nil)
                        if (idol.heart == incomingData.heart && (idol.top3 == incomingData.top3 || incomingData.top3 == null)) {
                            continue
                        }

                        val msg = "=== update ${idol.getName()} heart:${idol.heart} → ${incomingData.heart} top3:${idol.top3 ?: "null"} → ${incomingData.top3 ?: "변경없음"}"
                        // DB에서 가져온 idol의 값을 incomingData의 값으로 업데이트
                        idol.heart = incomingData.heart
                        idol.infoSeq = incomingData.infoSeq
                        idol.top3Seq = incomingData.top3Seq
                        idol.top3ImageVer = incomingData.top3ImageVer
                        if (incomingData.top3 != null) {
                            idol.top3 = incomingData.top3
                            idol.top3Type = incomingData.top3Type
                            // 변경된 top3를 image_url*에 적용
                            idolApiManager.updateTop3Legacy(idol)
                        }
                        idol.updateTs = lastTs
                        Log.i(TAG, msg)
                        logI("udp data $msg")
                        updatedIdols.add(idol)
                    }

                    if(!BuildConfig.CELEB) {
                        updateMissionComplete(lastTs = lastTs, context = context)
                    }

                    // DB에 없는 아이돌 일괄처리
                    if( notExistingIdolsIds.size > 10 ) {
                        // 중복 호출 방지
                        if( updatingAll ) {
                            return@launch
                        }
                        // 전체 아이돌 목록 다시 가져오기
                        updatingAll = true
                        logD("call update all with noExistingIdolsIds ${notExistingIdolsIds.size}")
                        idolApiManager.updateAll { result ->
                            updatingAll = false
                            EventBus.sendEvent(true, Const.BROADCAST_MANAGER_MESSAGE)
                        }
                        return@launch // 전체 업데이트 처리하므로 더이상 진행할 필요 없음
                    } else {
                        for( id in notExistingIdolsIds) {
                            if (updatingNotExistingIds.add(id)) {
                                idolApiManager.updateOne(id).invokeOnCompletion {
                                    updatingNotExistingIds.remove(id)
                                }
                            }
                        }
                    }

                    val hasUpdate = upsertIdolsWithTsUseCase(updatedIdols.map { it.toDomain() }, ts)
                        .mapDataResource { it }
                        .awaitOrThrow()
                    hasUpdate?.let {
                        if (it) EventBus.sendEvent(true, Const.BROADCAST_MANAGER_MESSAGE)
                    }

                    // updatedInfoVerIdols가 많으면. 소희님과 합의하여 30개로 정함.
                    if( updatedInfoVerIdols.size > 30 ) {
                        // 중복 호출 방지
                        if( updatingAll ) {
                            return@launch
                        }
                        updatingAll = true

                        logD("call update all updateInfoVerIdols ${updatedInfoVerIdols.size}")

                        idolApiManager.updateAll { _ ->
                            updatingAll = false
                            EventBus.sendEvent(true, Const.BROADCAST_MANAGER_MESSAGE)
                        }
                    } else {
                        idolApiManager.updateIdols(updatedInfoVerIdols) {
                            // 변경된게 있을때 갱신
                            if(updatedInfoVerIdols.isNotEmpty()) {
                                EventBus.sendEvent(true, Const.BROADCAST_MANAGER_MESSAGE)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 일정시간동안 응답이 없는지 확인용
    fun isConnected() : Boolean {
        val threshold = 10
        val ts : Int = (System.currentTimeMillis() / 1000).toInt()
        if( /*lastTs > 0 &&*/ ts - idolApiManager.getLastLocalTs() > threshold ) {
            logE("=== No response from server within $threshold seconds.")
            return false
        } else {
            logE("=== Turn server")
            return true
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    // 데이터량을 줄이기위해 직렬화한 숫자를 다시 복원
    private fun getDec(bb : ByteBuffer, pos : Int ) : Pair<Long, Int> {
        val dec : Long
        val len : Int

        if( bb[pos].toUByte().toInt() <= 0xFB ) {
            dec = bb[pos].toUByte().toLong()
            len = 1
        } else if( bb[pos].toUByte().toInt() == 0xFC ) {
            dec = bb[pos+1].toUByte().toLong().shl(8) or bb[pos+2].toUByte().toLong()
            len = 3
        } else if( bb[pos].toUByte().toInt() == 0xFD ) {
            dec = bb[pos+1].toUByte().toLong().shl(16) or bb[pos+2].toUByte().toLong().shl(8) or bb[pos+3].toUByte().toLong()
            len = 4
        } else if( bb[pos].toUByte().toInt() == 0xFE ) {
            dec =  bb[pos+1].toUByte().toLong().shl(24) or bb[pos+2].toUByte().toLong().shl(16) or
                    bb[pos+3].toUByte().toLong().shl(8) or bb[pos+4].toUByte().toLong()
            len = 5
        } else {
            dec = bb[pos+1].toUByte().toLong().shl(56)   or bb[pos+2].toUByte().toLong().shl(48) or
                    bb[pos+3].toUByte().toLong().shl(40) or bb[pos+4].toUByte().toLong().shl(32) or
                    bb[pos+5].toUByte().toLong().shl(24) or bb[pos+6].toUByte().toLong().shl(16) or
                    bb[pos+7].toUByte().toLong().shl(8)  or bb[pos+8].toUByte().toLong()
            len = 9
        }

        return Pair(dec, len)
    }

    private fun getLong(bb: ByteBuffer, pos: Int) : Long {
        return  bb[pos].toUByte().toLong().shl(24) or bb[pos+1].toUByte().toLong().shl(16) or
                bb[pos+2].toUByte().toLong().shl(8) or bb[pos+3].toUByte().toLong()
    }

    private val hexArray = "0123456789ABCDEF".toCharArray()
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = (bytes[j].toUByte() and 0xFF.toUByte()).toUInt()

            hexChars[j * 2] = hexArray[(v shr 4).toInt()]
            hexChars[j * 2 + 1] = hexArray[(v and 0x0F.toUInt()).toInt()]
        }
        return String(hexChars)
    }

    // 날짜가 바뀌는지 검사해서 바꼈다면 api로 update daily 처리
    private fun checkDayChange(ts: Int) {
        val context = context ?: return

        if( lastTs == 0 ) { return }
        val oldDate = Date(lastTs.toLong()*1000)
        val newDate = Date(ts.toLong()*1000)
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("Asia/Seoul") // 한국시간 기준으로 날짜 변경 체크
        if( !fmt.format(oldDate).equals(fmt.format(newDate)) ) {
            idolApiManager.updateDailyNeeded = true
        }

    }

    private fun updateMissionComplete(lastTs: Int, context: Context) {

        val pattern = "yyyy-MM-dd'T'HH:mm:ss"
        val formatter = SimpleDateFormat(pattern, Locale.US)

        val lastTsDateFormat = formatter.format(lastTs * 1000L)
        val globalVariableDateFormat = formatter.format(GlobalVariable.ServerTs * 1000L)

        val lastTsDate = formatter.parse(lastTsDateFormat)
        val globalVariableDate = formatter.parse(globalVariableDateFormat)

        if (lastTsDate == null || globalVariableDate == null) {
            return
        }

        //udp에서온 ts값과 api에서 업데이트된 ts중 가장 최신값을 뽑습니다.
        val resultDate = if (lastTsDate.after(globalVariableDate)) {
            lastTsDateFormat
        } else {
            globalVariableDateFormat
        }

        if (UtilK.isDayChanged(resultDate, context)) {
            // 미션 초기화
            Util.setPreference(context, Const.PREF_MISSION_COMPLETED, false)
        }
    }

}