package net.ib.mn.data.remote.udp

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ib.mn.BuildConfig
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.data.remote.dto.toEntity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UDP Broadcast Manager - 실시간 아이돌 데이터 업데이트
 *
 * old 프로젝트의 IdolBroadcastManager를 현재 프로젝트에 맞게 재구현
 * - AsyncServer 대신 표준 DatagramSocket 사용
 * - EventBus 대신 Flow 사용
 * - Room DB 직접 업데이트
 * - 업데이트 시 Flow를 통해 ViewModel에 알림
 */
@Singleton
class IdolBroadcastManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val idolDao: IdolDao,
    private val idolRepository: net.ib.mn.domain.repository.IdolRepository,
    private val rankingCacheRepository: net.ib.mn.data.repository.RankingCacheRepository
) {
    companion object {
        /**
         * UDP 상세 로그 출력 여부
         * true: 모든 UDP 수신/파싱 상세 로그 출력
         * false: 기본 로그만 출력
         */
        var VERBOSE_LOGGING = true
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val TAG = "IdolBroadcast"
    private val version = 3

    // UDP 소켓 및 연결 정보
    private var socket: DatagramSocket? = null
    private var host = if (BuildConfig.CELEB) "myloveactor.com" else "myloveidol.com"
    private var port = 9413
    private var lbHost = host  // routing host
    private var lbPort = port

    // 타이밍 및 상태 관리
    private var lastTs: Int = 0
    private var lastOverallSeq: UShort = 0u
    private var connectCount = 0
    private var userId = 0

    // 업데이트 제어
    private val mutex = Mutex()
    private val idols: ConcurrentHashMap<Int, IdolUpdateData> = ConcurrentHashMap()
    private val updatingNotExistingIds = ConcurrentHashMap.newKeySet<Int>()
    private var updatingAll = false

    // Job 관리
    private var connectionJob: Job? = null
    private var heartbeatJob: Job? = null
    private var receiveJob: Job? = null
    private var retryJob: Job? = null

    // 업데이트 이벤트 Flow - 변경된 아이돌 ID 리스트 전달
    // replay = 1로 설정하여 구독 전 발행된 마지막 이벤트를 재생
    private val _updateEvent = MutableSharedFlow<Set<Int>>(
        replay = 1,
        extraBufferCapacity = 0
    )
    val updateEvent: SharedFlow<Set<Int>> = _updateEvent.asSharedFlow()

    /**
     * UDP 연결 설정
     */
    fun setupConnection(url: String, userId: Int) {
        this.userId = userId

        scope.launch {
            try {
                val uri = Uri.parse(url)
                host = uri.host ?: host
                port = uri.port.takeIf { it > 0 } ?: port

                lbHost = host
                lbPort = port

                Log.i(TAG, "=== setupConnection host=$host port=$port userId=$userId")

                connect()
                startHeartbeat()
            } catch (e: Exception) {
                Log.e(TAG, "=== setupConnection error", e)
            }
        }
    }

    /**
     * routing server부터 다시 접속
     */
    fun reinitConnection() {
        scope.launch {
            disconnect()
            host = lbHost
            port = lbPort
            connect()
        }
    }

    /**
     * UDP 소켓 연결
     */
    private fun connect() {
        connectCount++

        connectionJob?.cancel()
        connectionJob = scope.launch {
            try {
                // 기존 소켓 정리
                socket?.close()

                // 새 소켓 생성 및 서버에 연결
                socket = DatagramSocket()

                // DatagramSocket.connect() - 특정 서버 주소에 소켓 연결
                // 이렇게 하면 해당 서버로부터만 패킷을 받을 수 있음 (old 프로젝트의 connectDatagram과 동일)
                val address = InetAddress.getByName(host)
                socket?.connect(address, port)

                Log.i(TAG, "=== UDP socket created and connected to $host:$port")
                Log.i(TAG, "=== Socket isConnected: ${socket?.isConnected}, isClosed: ${socket?.isClosed}")

                // 수신 시작 - 독립적인 scope에서 실행
                // connectionJob이 완료되어도 receiveJob은 계속 실행되도록
                startReceiving()

                // 1초 후 초기 패킷 전송
                delay(1000L)
                if (connectCount != 2) {
                    send(true)
                } else {
                    send(false)
                }

                Log.i(TAG, "=== connect completed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "=== connect error", e)
                retryConnection()
            }
        }
    }

    /**
     * UDP 수신 시작
     */
    private fun startReceiving() {
        receiveJob?.cancel()
        receiveJob = scope.launch(Dispatchers.IO) {
            try {
                val currentSocket = socket
                if (currentSocket == null) {
                    Log.e(TAG, "=== startReceiving: socket is null!")
                    return@launch
                }

                if (currentSocket.isClosed) {
                    Log.e(TAG, "=== startReceiving: socket is closed!")
                    return@launch
                }

                Log.i(TAG, "=== startReceiving: socket ready, waiting for UDP packets...")
                Log.i(TAG, "=== socket local port: ${currentSocket.localPort}")

                val buffer = ByteArray(8192)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive && !currentSocket.isClosed) {
                    try {
                        // DatagramSocket.receive()는 blocking call - 패킷이 올 때까지 대기
                        currentSocket.receive(packet)

                        val data = packet.data.copyOf(packet.length)
                        Log.i(TAG, "=== received ${data.size} bytes from ${packet.address}:${packet.port}")

                        if (VERBOSE_LOGGING) {
                            Log.d(TAG, "📦 UDP 패킷 수신")
                            Log.d(TAG, "   크기: ${data.size} bytes")
                            Log.d(TAG, "   송신자: ${packet.address}:${packet.port}")
                            Log.d(TAG, "   hex: ${bytesToHex(data.copyOf(minOf(data.size, 64)))}")
                        }

                        if (!updatingAll) {
                            launch {
                                parse(ByteBuffer.wrap(data), data.size)
                            }
                        } else {
                            Log.i(TAG, "=== updating all is in progress. skip.")
                        }
                    } catch (e: java.net.SocketException) {
                        if (isActive && !currentSocket.isClosed) {
                            Log.e(TAG, "=== socket exception during receive", e)
                            throw e
                        } else {
                            Log.i(TAG, "=== socket closed, stopping receive loop")
                            break
                        }
                    }
                }

                Log.i(TAG, "=== startReceiving: loop ended")

            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "=== receive error", e)
                    retryConnection()
                }
            }
        }
    }

    /**
     * 연결 재시도
     */
    private fun retryConnection() {
        retryJob?.cancel()
        retryJob = scope.launch {
            repeat(10) {
                delay(10000) // 10초 간격
                if (socket?.isClosed != false) {
                    Log.i(TAG, "=== retry connection attempt ${it + 1}")
                    connect()
                } else {
                    return@launch
                }
            }
        }
    }

    /**
     * Heartbeat 전송 시작 (30초마다)
     */
    fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(30000) // 30초
                Log.i(TAG, "=== 30 sec timer fired. Send request.")
                send()
            }
        }
    }

    /**
     * Heartbeat 중지
     */
    fun stopHeartbeat() {
        heartbeatJob?.cancel()
    }

    /**
     * 연결 해제
     */
    suspend fun disconnect() {
        stopHeartbeat()
        receiveJob?.cancel()
        connectionJob?.cancel()
        retryJob?.cancel()

        socket?.close()
        socket = null

        Log.i(TAG, "=== disconnected")
    }

    /**
     * UDP 패킷 전송
     */
    private fun send(requestAll: Boolean = false) {
        scope.launch {
            try {
                // version
                var ba = byteArrayOf(version.toByte())

                // 전체 아이돌 요청인지?
                ba += if (requestAll) {
                    byteArrayOf(0.toByte())
                } else {
                    byteArrayOf(1.toByte())
                }

                // timestamp
                val ts: Int = (System.currentTimeMillis() / 1000).toInt()
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

                // connect()된 소켓은 주소 없이도 전송 가능
                val packet = DatagramPacket(ba, ba.size)
                socket?.send(packet)

                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "=== sent ${bytesToHex(ba)} to connected address")
                }
            } catch (e: Exception) {
                Log.e(TAG, "=== send error", e)
            }
        }
    }

    /**
     * 수신한 UDP 패킷 파싱
     */
    private suspend fun parse(bb: ByteBuffer, size: Int) {
        mutex.withLock {
            idols.clear()
            try {
                var pos = 0
                val ver = bb[pos].toUByte().toInt()

                // version 체크
                if (version < ver) {
                    Log.w(TAG, "=== version mismatch. client=$version server=$ver")
                    stopHeartbeat()
                    return
                }

                pos += 1

                // type 처리
                val isAll = bb[pos].toUByte().toInt() == 0
                val type = bb[pos].toInt()

                when (type) {
                    2 -> { // re-route (ip:port)
                        Log.i(TAG, "=== packet type 2: re-route")
                        host = "${bb[pos + 1].toUByte()}.${bb[pos + 2].toUByte()}.${bb[pos + 3].toUByte()}.${bb[pos + 4].toUByte()}"
                        port = bb[pos + 5].toUByte().toInt().shl(8) or bb[pos + 6].toUByte().toInt()
                        Log.i(TAG, "=== re-route host=$host port=$port")
                        // 순차 실행을 위해 코루틴 내에서 실행
                        scope.launch {
                            disconnect()
                            connect()
                        }
                        return
                    }

                    3 -> { // re-route (port only)
                        Log.i(TAG, "=== packet type 3: re-route port")
                        port = bb[pos + 1].toUByte().toInt().shl(8) or bb[pos + 2].toUByte().toInt()
                        Log.i(TAG, "=== re-route port=$port")
                        // 순차 실행을 위해 코루틴 내에서 실행
                        scope.launch {
                            disconnect()
                            connect()
                        }
                        return
                    }
                }

                pos += 1

                // timestamp
                val ts = getLong(bb, pos).toInt()
                lastTs = ts
                pos += 4

                // overall-seq
                val overallSeq = (bb[pos].toUByte().toInt().shl(8) or bb[pos + 1].toUByte().toInt()).toUShort()
                val nextSeq = (lastOverallSeq + 1u).toUShort()

                if (nextSeq != overallSeq && !isAll) {
                    Log.e(TAG, "=== 패킷 누락 expected=$nextSeq received=$overallSeq")
                    send(true)
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

                Log.i(TAG, "=== overallSeq=$overallSeq total=$total seq=$seq isAll=$isAll")

                if (VERBOSE_LOGGING) {
                    Log.d(TAG, "🔍 패킷 파싱 정보")
                    Log.d(TAG, "   version: $ver")
                    Log.d(TAG, "   isAll: $isAll")
                    Log.d(TAG, "   timestamp: $ts")
                    Log.d(TAG, "   overallSeq: $overallSeq (prev: $lastOverallSeq)")
                    Log.d(TAG, "   total: $total, seq: $seq")
                }

                // 전체 패킷의 마지막을 받으면 바로 전송
                if (isAll && seq == total - 1) {
                    send()
                }

                // 아이돌 데이터 파싱
                var lastId = 0
                var idolCount = 0
                while (pos < size) {
                    // id (delta encoding)
                    val idPair = getDec(bb, pos)
                    val id = idPair.first
                    pos += idPair.second

                    // heart (vote count)
                    val votesPair = getDec(bb, pos)
                    val votes = votesPair.first
                    pos += votesPair.second

                    // info version
                    val infoVer = bb[pos].toUByte().toInt()
                    pos++

                    lastId += id.toInt()

                    // top3 version
                    val top3Ver = bb[pos].toUByte().toInt()
                    pos++

                    // addition count
                    val additionCount = bb[pos].toUByte().toInt()
                    pos++

                    var top3: String? = null
                    var top3Type: String? = null
                    var top3ImageVer: String? = null

                    // addition 정보 파싱
                    repeat(additionCount) {
                        val additionType = bb[pos].toUByte().toInt()
                        pos++

                        val additionLength = bb[pos].toUByte().toInt()
                        pos++

                        when (additionType) {
                            1 -> { // compact-top3
                                var p = pos
                                val top1 = getLong(bb, p)
                                p += 4
                                val type1 = bb[p].toInt().toChar()
                                p += 1
                                val top2 = getLong(bb, p)
                                p += 4
                                val type2 = bb[p].toInt().toChar()
                                p += 1
                                val top3Value = getLong(bb, p)
                                p += 4
                                val type3 = bb[p].toInt().toChar()

                                top3 = "$top1,$top2,$top3Value"
                                top3Type = "$type1,$type2,$type3"
                                Log.i(TAG, "=== id=$lastId additionType=1 top3=$top3")
                            }

                            2 -> { // top3-image-ver
                                if (pos + additionLength > size) {
                                    Log.e(TAG, "=== size mismatch!!! pos=$pos additionLength=$additionLength size=$size")
                                    return
                                }

                                val imageVer1 = bb[pos].toUInt()
                                val imageVer2 = bb[pos + 1].toUInt()
                                val imageVer3 = bb[pos + 2].toUInt()

                                top3ImageVer = "$imageVer1,$imageVer2,$imageVer3"
                                Log.i(TAG, "=== id=$lastId additionType=2 top3ImageVer=$top3ImageVer")
                            }
                        }

                        pos += additionLength
                    }

                    // 데이터 저장
                    idols[lastId] = IdolUpdateData(
                        heart = votes,
                        infoSeq = infoVer,
                        top3Seq = top3Ver,
                        top3 = top3,
                        top3Type = top3Type,
                        top3ImageVer = top3ImageVer
                    )

                    idolCount++

                    if (VERBOSE_LOGGING) {
                        Log.d(TAG, "   👤 idol #$idolCount: id=$lastId heart=$votes infoVer=$infoVer top3Ver=$top3Ver")
                        if (top3 != null) Log.d(TAG, "      top3=$top3 type=$top3Type ver=$top3ImageVer")
                    }
                }

                if (VERBOSE_LOGGING) {
                    Log.d(TAG, "✅ 파싱 완료: $idolCount 명의 아이돌 데이터")
                }

                // DB 업데이트
                updateDatabase(ts)

            } catch (e: Exception) {
                Log.e(TAG, "=== parse error", e)
            }
        }
    }

    /**
     * DB 업데이트
     */
    private suspend fun updateDatabase(ts: Int) {
        try {
            val keyList = idols.keys.toList()
            if (keyList.isEmpty()) return

            if (VERBOSE_LOGGING) {
                Log.d(TAG, "💾 DB 업데이트 시작")
                Log.d(TAG, "   수신한 아이돌 수: ${keyList.size}")
            }

            // DB에서 기존 데이터 조회
            val dbIdols = idolDao.getIdolsByIds(keyList)
            val idolMap = dbIdols.associateBy { it.id }

            if (VERBOSE_LOGGING) {
                Log.d(TAG, "   DB에서 조회한 아이돌 수: ${dbIdols.size}")
            }

            val notExistingIds = ArrayList<Int>()
            val updatedIdols = ArrayList<IdolEntity>()
            val updatedInfoVerIds = ArrayList<Int>()

            for ((id, incomingData) in idols.entries) {
                val idol = idolMap[id]

                if (idol == null) {
                    // DB에 없는 아이돌
                    notExistingIds.add(id)
                    continue
                }

                // info_ver 변경 (API 호출 필요하지만 heart는 업데이트)
                if (idol.infoSeq != incomingData.infoSeq) {
                    Log.i(TAG, "=== info_ver updated id=$id ${idol.infoSeq} → ${incomingData.infoSeq}")
                    updatedInfoVerIds.add(id)
                    // continue 제거: heart 값은 여전히 업데이트해야 함
                }

                // top3_ver 변경 (API 호출 필요하지만 heart는 업데이트)
                if (idol.top3Seq != incomingData.top3Seq) {
                    Log.i(TAG, "=== top3_ver updated id=$id ${idol.top3Seq} → ${incomingData.top3Seq}")
                    updatedInfoVerIds.add(id)
                    // continue 제거: heart 값은 여전히 업데이트해야 함
                }

                // top3_image_ver 변경 (API 호출 필요하지만 heart는 업데이트)
                if (idol.top3ImageVer != incomingData.top3ImageVer && incomingData.top3ImageVer != null) {
                    Log.i(TAG, "=== top3_image_ver updated id=$id")
                    updatedInfoVerIds.add(id)
                    // continue 제거: heart 값은 여전히 업데이트해야 함
                }

                // heart, top3 변경
                val heartChanged = idol.heart != incomingData.heart
                val top3Changed = incomingData.top3 != null && idol.top3 != incomingData.top3

                if (!heartChanged && !top3Changed) {
                    continue
                }

                // 업데이트할 엔티티 생성
                val updated = idol.copy(
                    heart = incomingData.heart,
                    infoSeq = incomingData.infoSeq,
                    top3Seq = incomingData.top3Seq,
                    top3 = incomingData.top3 ?: idol.top3,
                    top3Type = incomingData.top3Type ?: idol.top3Type,
                    top3ImageVer = incomingData.top3ImageVer ?: idol.top3ImageVer,
                    updateTs = ts
                )

                Log.i(TAG, "=== update id=$id heart:${idol.heart}→${incomingData.heart}")

                if (VERBOSE_LOGGING) {
                    Log.d(TAG, "   🔄 업데이트: id=$id")
                    if (heartChanged) Log.d(TAG, "      heart: ${idol.heart} → ${incomingData.heart}")
                    if (top3Changed) Log.d(TAG, "      top3: ${idol.top3} → ${incomingData.top3}")
                }

                updatedIdols.add(updated)
            }

            // DB 업데이트 실행
            val changedIdolIds = mutableSetOf<Int>()

            if (updatedIdols.isNotEmpty()) {
                idolDao.upsertIdols(updatedIdols)
                changedIdolIds.addAll(updatedIdols.map { it.id })
                Log.i(TAG, "=== updated ${updatedIdols.size} idols in DB")

                if (VERBOSE_LOGGING) {
                    Log.d(TAG, "✅ DB 업데이트 완료: ${updatedIdols.size}명")
                    updatedIdols.take(5).forEach { idol ->
                        Log.d(TAG, "   - id=${idol.id} heart=${idol.heart}")
                    }
                    if (updatedIdols.size > 5) {
                        Log.d(TAG, "   ... 외 ${updatedIdols.size - 5}명")
                    }
                }
            }

            // info/top3 버전 변경도 추적 (API 호출 필요)
            if (updatedInfoVerIds.isNotEmpty()) {
                changedIdolIds.addAll(updatedInfoVerIds)
            }

            // 업데이트 이벤트 발행 - 변경된 아이돌 ID 리스트 전달
            if (changedIdolIds.isNotEmpty()) {
                _updateEvent.emit(changedIdolIds)
                Log.i(TAG, "=== emitted update event with ${changedIdolIds.size} changed idols")

                if (VERBOSE_LOGGING) {
                    Log.d(TAG, "📢 업데이트 이벤트 발행")
                    Log.d(TAG, "   변경된 아이돌 수: ${changedIdolIds.size}")
                    Log.d(TAG, "   변경된 ID: ${changedIdolIds.take(10)}")
                    if (changedIdolIds.size > 10) {
                        Log.d(TAG, "   ... 외 ${changedIdolIds.size - 10}개")
                    }
                    Log.d(TAG, "   → ViewModel에서 해당 아이돌만 재계산")
                }

                // 랭킹 캐시 부분 업데이트 (전체 재생성이 아닌 해당 아이돌만)
                Log.i(TAG, "🔄 Updating ranking cache for ${changedIdolIds.size} idols")
                rankingCacheRepository.updateIdolsFromUdp(changedIdolIds)
                Log.i(TAG, "✅ Ranking cache partially updated")
            } else {
                if (VERBOSE_LOGGING) {
                    Log.d(TAG, "ℹ️ 변경사항 없음 - 이벤트 발행 안 함")
                }
            }

            // info_ver 변경된 아이돌 API 호출 (old 프로젝트 로직)
            if (updatedInfoVerIds.isNotEmpty()) {
                Log.i(TAG, "=== ${updatedInfoVerIds.size} idols need info update via API")

                // 30개 이상이면 전체 갱신 (old 프로젝트 로직)
                if (updatedInfoVerIds.size > 30) {
                    Log.w(TAG, "⚠️ ${updatedInfoVerIds.size} idols changed (>30) - Starting full refresh")
                    refreshAllIdols()
                } else {
                    // API 호출하여 전체 필드 업데이트
                    updateIdolsByIds(updatedInfoVerIds.toList())
                }
            }

            // DB에 없는 아이돌 처리 (old 프로젝트 로직)
            if (notExistingIds.isNotEmpty()) {
                Log.w(TAG, "=== ${notExistingIds.size} idols not found in DB")

                // 10개 이상이면 전체 갱신 (old 프로젝트 로직)
                if (notExistingIds.size > 10) {
                    Log.w(TAG, "⚠️ ${notExistingIds.size} missing idols (>10) - Starting full refresh")
                    refreshAllIdols()
                } else {
                    // API 호출하여 누락된 아이돌 정보 가져오기
                    updateIdolsByIds(notExistingIds.toList())
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "=== updateDatabase error", e)
        }
    }

    /**
     * Delta-encoded 숫자 복원
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    private fun getDec(bb: ByteBuffer, pos: Int): Pair<Long, Int> {
        val dec: Long
        val len: Int

        when (bb[pos].toUByte().toInt()) {
            in 0..0xFB -> {
                dec = bb[pos].toUByte().toLong()
                len = 1
            }

            0xFC -> {
                dec = bb[pos + 1].toUByte().toLong().shl(8) or bb[pos + 2].toUByte().toLong()
                len = 3
            }

            0xFD -> {
                dec = bb[pos + 1].toUByte().toLong().shl(16) or
                        bb[pos + 2].toUByte().toLong().shl(8) or
                        bb[pos + 3].toUByte().toLong()
                len = 4
            }

            0xFE -> {
                dec = bb[pos + 1].toUByte().toLong().shl(24) or
                        bb[pos + 2].toUByte().toLong().shl(16) or
                        bb[pos + 3].toUByte().toLong().shl(8) or
                        bb[pos + 4].toUByte().toLong()
                len = 5
            }

            else -> {
                dec = bb[pos + 1].toUByte().toLong().shl(56) or
                        bb[pos + 2].toUByte().toLong().shl(48) or
                        bb[pos + 3].toUByte().toLong().shl(40) or
                        bb[pos + 4].toUByte().toLong().shl(32) or
                        bb[pos + 5].toUByte().toLong().shl(24) or
                        bb[pos + 6].toUByte().toLong().shl(16) or
                        bb[pos + 7].toUByte().toLong().shl(8) or
                        bb[pos + 8].toUByte().toLong()
                len = 9
            }
        }

        return Pair(dec, len)
    }

    /**
     * 4바이트 Long 읽기
     */
    private fun getLong(bb: ByteBuffer, pos: Int): Long {
        return bb[pos].toUByte().toLong().shl(24) or
                bb[pos + 1].toUByte().toLong().shl(16) or
                bb[pos + 2].toUByte().toLong().shl(8) or
                bb[pos + 3].toUByte().toLong()
    }

    /**
     * ByteArray를 Hex 문자열로 변환
     */
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

    /**
     * ID 리스트로 아이돌 정보 업데이트 (API 호출)
     *
     * old 프로젝트의 IdolApiManager.updateIdols() 로직
     * info_ver 변경 감지 시 전체 필드 업데이트용
     *
     * @param ids 업데이트할 아이돌 ID 리스트
     */
    private fun updateIdolsByIds(ids: List<Int>) {
        scope.launch {
            try {
                Log.i(TAG, "🔄 Updating ${ids.size} idols via API: $ids")

                // API 호출 (fields=null이면 모든 필드 반환)
                idolRepository.getIdolsByIds(ids, fields = null).collect { result ->
                    when (result) {
                        is net.ib.mn.domain.model.ApiResult.Success -> {
                            val idolDataList = result.data.data

                            if (idolDataList != null && idolDataList.isNotEmpty()) {
                                Log.i(TAG, "✅ API returned ${idolDataList.size} idols")

                                // DB에 업데이트 (isViewable 포함 모든 필드)
                                val entities = idolDataList.map { it.toEntity() }
                                idolDao.upsertIdols(entities)

                                Log.i(TAG, "✅ Updated ${entities.size} idols in DB (isViewable included)")

                                // isViewable 값 로깅
                                if (VERBOSE_LOGGING) {
                                    entities.forEach { entity ->
                                        Log.d(TAG, "  - ID:${entity.id} ${entity.name} isViewable=${entity.isViewable}")
                                    }
                                }

                                // UI 갱신 이벤트 발행
                                val updatedIdSet = entities.map { it.id }.toSet()
                                _updateEvent.emit(updatedIdSet)

                                // ⚠️ API 업데이트 시 cacheIdolsRanking() 호출하지 않음
                                // 이유: 전체 캐시 재생성 시 사용자의 투표 업데이트가 덮어씌워질 수 있음
                                // StateFlow 구독 방식으로 UI는 자동으로 업데이트됨
                            } else {
                                Log.w(TAG, "⚠️ API returned empty data")
                            }
                        }
                        is net.ib.mn.domain.model.ApiResult.Error -> {
                            Log.e(TAG, "❌ API error: ${result.message}")
                        }
                        is net.ib.mn.domain.model.ApiResult.Loading -> {
                            // Loading state
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ updateIdolsByIds error", e)
            }
        }
    }

    /**
     * 전체 아이돌 정보 갱신
     * old 프로젝트의 전체 갱신 로직
     *
     * 30개 이상 info_ver 변경 또는 10개 이상 누락 시 호출
     */
    private fun refreshAllIdols() {
        scope.launch {
            try {
                Log.i(TAG, "🔄 Starting full idol refresh via API")

                // API 호출 (type=null, category=null이면 전체 조회)
                idolRepository.getIdols(type = null, category = null).collect { result ->
                    when (result) {
                        is net.ib.mn.domain.model.ApiResult.Success -> {
                            val idolDataList = result.data.data

                            if (idolDataList != null && idolDataList.isNotEmpty()) {
                                Log.i(TAG, "✅ Full refresh: API returned ${idolDataList.size} idols")

                                // DB에 전체 업데이트
                                val entities = idolDataList.map { it.toEntity() }
                                idolDao.upsertIdols(entities)

                                Log.i(TAG, "✅ Full refresh complete: ${entities.size} idols updated in DB")

                                // isViewable 통계 로깅
                                val viewableCount = entities.count { it.isViewable == "Y" }
                                val hiddenCount = entities.count { it.isViewable == "N" }
                                Log.i(TAG, "   Viewable: $viewableCount, Hidden: $hiddenCount")

                                // UI 전체 갱신 이벤트 발행 (empty set = 전체 갱신)
                                _updateEvent.emit(emptySet())

                                // ⚠️ 전체 갱신 시에도 cacheIdolsRanking() 호출하지 않음
                                // 이유: 전체 캐시 재생성 시 사용자의 투표 업데이트가 덮어씌워질 수 있음
                                // StateFlow 구독 방식으로 UI는 자동으로 업데이트됨
                            } else {
                                Log.w(TAG, "⚠️ Full refresh: API returned empty data")
                            }
                        }
                        is net.ib.mn.domain.model.ApiResult.Error -> {
                            Log.e(TAG, "❌ Full refresh error: ${result.message}")
                        }
                        is net.ib.mn.domain.model.ApiResult.Loading -> {
                            // Loading state
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ refreshAllIdols error", e)
            }
        }
    }

    /**
     * 소멸자
     */
    fun destroy() {
        scope.launch {
            disconnect()
        }
        scope.cancel()
    }
}

/**
 * UDP로 받은 아이돌 업데이트 데이터
 */
data class IdolUpdateData(
    val heart: Long,
    val infoSeq: Int,
    val top3Seq: Int,
    val top3: String? = null,
    val top3Type: String? = null,
    val top3ImageVer: String? = null
)
