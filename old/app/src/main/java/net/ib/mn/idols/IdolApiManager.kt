package net.ib.mn.idols

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.addon.IdolGson
import net.ib.mn.common.util.logD
import net.ib.mn.common.util.logE
import net.ib.mn.common.util.logV
import net.ib.mn.core.data.repository.FavoritesRepository
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.model.Idol
import net.ib.mn.domain.model.IdolFiledData
import net.ib.mn.domain.usecase.DeleteAllAndSaveIdolsUseCase
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.domain.usecase.UpdateHeartAndTop3UseCase
import net.ib.mn.domain.usecase.UpsertIdolsWithTsUseCase
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolCapriciousFieldModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toDomain
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.Const
import net.ib.mn.utils.EventBus
import net.ib.mn.utils.GlobalVariable
import net.ib.mn.utils.Util
import org.json.JSONArray
import org.json.JSONException
import java.util.Arrays
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import net.ib.mn.common.util.logI
import net.ib.mn.common.util.updateQueryParameter

//  기존 Api를 사용한 아이돌 리스트 관리
//  앱 설치 후 최초 시작시 아이돌 리스트를 받아오고, 다음부터는 이 저장된 리스트를 계속 활용.
//  서버에서 변경이 발생하면 그때 업데이트 (configs/self 응답의 daily_idol_update, all_idol_update 값)
//  daily_idol_update 업데이트시 anniversary, anniversary_days,   받아감
//  all_idol_update 업데이트시 전체 아이돌 리스트 갱신

@Singleton
class IdolApiManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private var deleteAllAndSaveIdolsUseCase: DeleteAllAndSaveIdolsUseCase,
    private var getIdolByIdUseCase: GetIdolByIdUseCase,
    private var upsertIdolsWithTsUseCase: UpsertIdolsWithTsUseCase,
    private var updateHeartAndTop3UseCase: UpdateHeartAndTop3UseCase,
    private var favoritesRepository: FavoritesRepository,
    private var idolsRepository: IdolsRepository,
) {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val reqImageSize = Util.getOnDemandImageSize(context)

    @Volatile
    private var isUpdatingLegacy = false

    private val KEY_DAILY = Const.PREF_DAILY_IDOL_UPDATE // "daily_idol_update_v2" // api 응답의 daily_idol_update 항목 저장 // v2 로 나눈 이유가 기억이 안남...
    private val KEY_ALL = Const.PREF_ALL_IDOL_UPDATE // "all_idol_update_v2" // api 응답의 all_idol_update 항목 저장

    private var lastTime = HashMap<String,Int>()  // 마지막으로 api 호출한 시간 timestamp
    open var daily_idol_update : String = "" // ConfigData 참조
    open var all_idol_update : String = ""
    var updateDailyNeeded = false     // 기념일/몰빵일 업데이트가 필요한지 여부
    var updateAllNeeded = false     // 전체 업데이트가 필요한지 여부
    var server_time : String = "" // 서버에서 날짜 바뀌는지 확인용
    private var lastUpdateTs : Int = 0
    private val interval : Long = if(BuildConfig.DEBUG) { 5 } else { 10 }
    private var updateJob: Job? = null

    private var lastLocalTs : Int = 0 // 마지막으로 패킷을 받은 로컬 타임

    // 테섭 실섭 전환시 usecase와 repository를 바꿔야 함
    fun reinit(
        deleteAllAndSaveIdolsUseCase: DeleteAllAndSaveIdolsUseCase,
        getIdolByIdUseCase: GetIdolByIdUseCase,
        upsertIdolsWithTsUseCase: UpsertIdolsWithTsUseCase,
        updateHeartAndTop3UseCase: UpdateHeartAndTop3UseCase,
        favoritesRepository: FavoritesRepository,
        idolsRepository: IdolsRepository,
    ) {
        this.deleteAllAndSaveIdolsUseCase = deleteAllAndSaveIdolsUseCase
        this.getIdolByIdUseCase = getIdolByIdUseCase
        this.upsertIdolsWithTsUseCase = upsertIdolsWithTsUseCase
        this.updateHeartAndTop3UseCase = updateHeartAndTop3UseCase
        this.favoritesRepository = favoritesRepository
        this.idolsRepository = idolsRepository
    }

    fun setLastLocalTs(ts: Int) {
        lastLocalTs = ts
    }

    fun getLastLocalTs() = lastLocalTs

    fun isConnected() : Boolean {
        val threshold = 10
        val ts : Int = (System.currentTimeMillis() / 1000).toInt()
        if( /*lastTs > 0 &&*/ ts - lastLocalTs > threshold ) {
            logD("=== No response from server within $threshold seconds.")
            return false
        } else {
            logD("=== Turn server")
            return true
        }
    }

    fun startTimer() {
        // 기존 updateJob이 있으면 취소
        updateJob?.cancel()
        updateJob = scope.launch {
            // 집계시간 직후인지 판단해서, 해당 경우에 3~5초 사이에 업데이트를 수행하도록 함
            if (isAggregationTimeJustPassed()) {
                val randomDelay = 3000L
                delay(randomDelay)
                update()
            }
            // 주기적으로 업데이트 수행
            while (isActive) {
                // configs/self 받아오기 전이면 아무것도 안한다
                if (ConfigModel.getInstance(context).cdnUrl.isNullOrEmpty()) {
                    logD("=== CDN URL is not set yet, skipping idol update.")
                    delay(interval * 1000)
                    continue
                }

                if (updateAllNeeded || updateDailyNeeded) {
                    logV("in StartTime updateAllNeeded: $updateAllNeeded updateDailyNeeded: $updateDailyNeeded")
                    update()
                } else if (!isConnected()) {
                    logD("❌ UDP server is gone... use legacy")
                    logV("in StartTime updateAllNeeded: legacy")
                    update()
                }

                delay(interval * 1000)
            }
        }
    }

    private fun isAggregationTimeJustPassed(): Boolean {
        // TODO: 집계시간 직후 3~5초 사이에 갱신되게 함
        // 여기에 집계 시간이 지나고 난 후 일정 시간(예: 3~5초)이 경과했는지 판단하는 로직을 구현
        return false
    }

    fun stopTimer() {
        updateJob?.cancel()
    }

    fun save() {
        Util.setPreference(context, KEY_DAILY, daily_idol_update)
        Util.setPreference(context, KEY_ALL, all_idol_update)
    }

    fun load() {
        daily_idol_update = Util.getPreference(context, KEY_DAILY)
        all_idol_update = Util.getPreference(context, KEY_ALL)

        Log.e("!!!!", "$all_idol_update $daily_idol_update")
    }

    private fun updateTop3(model: Idol) {
        _updateTop3(
            model.top3,
            model.top3ImageVer,
            model.imageUrl,
            model.imageUrl2,
            model.imageUrl3
        ) { index, newUrl ->
            when (index) {
                0 -> model.imageUrl = newUrl
                1 -> model.imageUrl2 = newUrl
                2 -> model.imageUrl3 = newUrl
            }
        }
    }

    // updateTop3와 같은 내용인데 모델만 달라서 일단 냅둠
    fun updateTop3Legacy(model: IdolModel) {
        _updateTop3(
            model.top3,
            model.top3ImageVer,
            model.imageUrl,
            model.imageUrl2,
            model.imageUrl3
        ) { index, newUrl ->
            when (index) {
                0 -> model.imageUrl = newUrl
                1 -> model.imageUrl2 = newUrl
                2 -> model.imageUrl3 = newUrl
            }
        }
    }

    private fun _updateTop3(
        top3: String?,
        top3ImageVer: String,
        imageUrl: String?,
        imageUrl2: String?,
        imageUrl3: String?,
        updateAction: (Int, String) -> Unit
    ) {
        var top3Ids: Array<String?> = if (top3 == null) {
            arrayOfNulls(3)
        } else {
            top3.split(",")
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
        }
        top3Ids = Arrays.copyOf(top3Ids, 3)

        for (index in 0 until 3) {
            if (!top3Ids[index].isNullOrEmpty()) {
                val ver = top3ImageVer.split(",").firstOrNull() ?: ""
                val url: String? = when (index) {
                    0 -> imageUrl
                    1 -> imageUrl2
                    2 -> imageUrl3
                    else -> null
                }

                if (!url.isNullOrEmpty()) {
                    val newUrl = url.updateQueryParameter("ver", ver)
                    updateAction(index, newUrl)
                }
            }
        }
    }

    // heart, top3, image_url들 등을 갱신시켜준다
    private fun updateTop3RelatedFields(idols: ArrayList<Idol>) {
        if( idols.isEmpty() ) {
            return
        }

        scope.launch(Dispatchers.IO) {
            val ids = idols.map { it.id }.joinToString(",")
            idolsRepository.getIdolsByIds(
                ids = ids,
                fields = "heart,top3,top3_type,image_url,image_url2,image_url3,top3_image_ver",
                onServerTime = {
                    GlobalVariable.ServerTs = it
                },
                listener = { response ->
                    val gson = IdolGson.getInstance()
                    if(response.optBoolean("success")){
                        scope.launch(Dispatchers.IO) {
                            try{
                                val array = response.getJSONArray("objects")
                                val ts = GlobalVariable.ServerTs

                                val updatedIdols = ArrayList<Idol>()

                                for(i in 0 until array.length()){
                                    val model = gson.fromJson(array.getJSONObject(i).toString(), IdolModel::class.java)
                                    val id = model.getId()

                                    val idol = getIdolByIdUseCase(id)
                                        .mapDataResource { it }
                                        .awaitOrThrow()

                                    if(idol == null) {
                                        continue
                                    }

                                    // heart, top3 변경이 있는 것만 업데이트 -> image_url들도 같이 업데이트 (top3만 가지고 이미지 주소 추출하던 것을 이미지 버저닝을 적용해야 해서 변경)
                                    idol.heart = model.heart
                                    idol.top3 = model.top3
                                    idol.top3Type = model.top3Type
                                    idol.imageUrl = model.imageUrl
                                    idol.imageUrl2 = model.imageUrl2
                                    idol.imageUrl3 = model.imageUrl3
                                    idol.top3ImageVer = model.top3ImageVer

                                    updatedIdols.add(idol)
                                }

                                // DB에 기록
                                val hasUpdate = upsertIdolsWithTsUseCase(updatedIdols, ts)
                                    .mapDataResource { it }
                                    .awaitOrThrow()
                                hasUpdate?.let {
                                    if (it) EventBus.sendEvent(true, Const.BROADCAST_MANAGER_MESSAGE)
                                }
                            } catch (e:JSONException){
                                e.printStackTrace()
                            }
                        }
                    }
                }, errorListener = {
                })
        }
    }

    // 모든 아이돌 리스트를 새로 가져온다
    fun updateAll(cb: ((Boolean) -> Unit)? ) {
        Util.log("=== 모든 아이돌 받아오기 ===")
        val idols = ArrayList<IdolModel>()
        scope.launch(Dispatchers.IO) {
            idolsRepository.getIdolsForSearch(
                listener = { response ->
                    scope.launch(Dispatchers.IO) {
                        val gson = IdolGson.getInstance()
                        if(response?.optBoolean("success") ?: false){
                            try{
                                val array = response.getJSONArray("objects")

                                for(i in 0 until array.length()){
                                    val model = gson.fromJson(array.getJSONObject(i).toString(), IdolModel::class.java)
                                    idols.add(model)
                                }

                                // DB에 기록
                                // 기존것 싹 날리고 새로 넣는다
                                this@IdolApiManager.deleteAllAndSaveIdolsUseCase.invoke(idols.map { it.toDomain() }).collectLatest{
                                    updateDailyNeeded = false
                                    updateAllNeeded = false
                                    save()
                                    scope.launch(Dispatchers.Main) {
                                        cb?.invoke(true)
                                    }
                                }
                            }catch (e:JSONException){
                                e.printStackTrace()
                                scope.launch(Dispatchers.Main) {
                                    cb?.invoke(false)
                                }
                            }
                        }else{
                            scope.launch(Dispatchers.Main) {
                                cb?.invoke(false)
                            }
                        }
                    }
                },
                errorListener = {
                    scope.launch(Dispatchers.Main) {
                        cb?.invoke(false)
                    }
                }
            )
        }
    }

    // 하트수/프사 변경을 적용
    private fun updateLegacy(cb: ( (Boolean) -> Unit )?) {
        if (isUpdatingLegacy) return
        isUpdatingLegacy = true

        scope.launch(Dispatchers.IO) {
            idolsRepository.getIdolsWithTs(
                fields = "heart,top3",
                onServerTime = {
                    GlobalVariable.ServerTs = it
                }
            ).collect {
                val response = it.data
                val gson = IdolGson.getInstance()
                if(response?.optBoolean("success") == true){
                    try{
                        val array = response.getJSONArray("objects")
                        val ts = GlobalVariable.ServerTs

                        val updatedIdols = ArrayList<Idol>()
                        val top3UpdatedIdols = ArrayList<Idol>() // top3가 변경된 아이돌들 (하트수는 무시)

                        for(i in 0 until array.length()){
                            val model = gson.fromJson(array.getJSONObject(i).toString(), IdolModel::class.java)
                            val id = model.getId()

                            val idol = getIdolByIdUseCase(id)
                                .mapDataResource { it }
                                .awaitOrThrow()

                            if(idol == null) {
                                logE("in updateLegacy")
                                updateAllNeeded = true
                                continue
                            }

                            // heart, top3 변경이 있는 것만 업데이트
                            val heart = model.heart
                            val top3 = model.top3

                            // top3가 변경되면 image_url들도 같이 업데이트 (top3만 가지고 이미지 주소 추출하던 것을 이미지 버저닝을 적용해야 해서 변경)
                            if(idol.top3 != top3) {
                                top3UpdatedIdols.add(idol)
                            }

                            if(idol.heart != heart || idol.top3 != top3) {
                                idol.heart = heart
                                idol.top3 = top3

                                updateTop3(idol)

                                updatedIdols.add(idol)
                            }
                        }

                        // updatedIdols에 있는 아이돌이 top3UpdatedIdols에도 있으면 updateIdols에서 제거
                        // top3UpdatedIdols는 투표수, 탑3, image_url들을 갱신하므로 중복으로 업데이트할 필요 없음
                        // 중복을 걸러내지 않으면 heart 업데이트와 image_url들 업데이트가 1초내로 이루어질 경우 ts가 같아서 갱신이 안되는 문제가 생긴다
                        val top3Ids = top3UpdatedIdols.map { it.id }.toSet()
                        updatedIdols.removeAll { it.id in top3Ids }

                        if (!updateAllNeeded) {
                            val hasUpdate = upsertIdolsWithTsUseCase(updatedIdols, ts)
                                .mapDataResource { it }
                                .awaitOrThrow()
                            hasUpdate?.let {
                                // top3 갱신할 필요가 없으면 순위내 투표수 갱신
                                // top3UpdatedIdols가 있으면 updateTop3RelatedFields()에서 BROADCAST_MANAGER_MESSAGE 보냄
                                if (it && top3UpdatedIdols.isEmpty()) EventBus.sendEvent(true, Const.BROADCAST_MANAGER_MESSAGE)
                            }

                            // top3가 변경된 image_url들을 idol_by_ids api를 사용하여 갱신하고 db에 반영
                            updateTop3RelatedFields(idols = top3UpdatedIdols)
                        }

                        // 아이돌 목록 갱신여부 확인
                        val all = response.optString("all_idol_update")
                        if(!all.isEmpty() && all_idol_update != all) {
                            updateAllNeeded = true
                            all_idol_update = all
                            save()
                        }

                        val daily = response.optString("daily_idol_update")
                        if(!daily.isEmpty() && daily_idol_update != daily) {
                            updateDailyNeeded = true
                            daily_idol_update = daily
                            save()
                        }
                        scope.launch(Dispatchers.Main) {
                            isUpdatingLegacy = false
                            cb?.invoke(true)
                        }

                    }catch (e:JSONException){
                        e.printStackTrace()
                        scope.launch(Dispatchers.Main) {
                            isUpdatingLegacy = false
                            cb?.invoke(false)
                        }
                    }
                }else{
                    scope.launch(Dispatchers.Main) {
                        isUpdatingLegacy = false
                        cb?.invoke(false)
                    }
                }
            }
        }
    }

    // 기념일/몰빵일 정보를 업데이트한다
    fun updateDaily(cb: ( (Boolean) -> Unit )?) {
        scope.launch(Dispatchers.IO) {
            idolsRepository.getIdolsWithTs(
                fields = "anniversary,burning_day",
                onServerTime = {
                    GlobalVariable.ServerTs = it
                }).collect {
                    val response = it.data
                    if(response?.optBoolean("success") == true){
                        try{
                            val gson = IdolGson.getInstance()
                            val array = response.getJSONArray("objects")
                            val ts = GlobalVariable.ServerTs

                            val updatedIdols = ArrayList<Idol>()

                            for(i in 0 until array.length()){
                                val model = gson.fromJson(array.getJSONObject(i).toString(), IdolModel::class.java)
                                val id = model.getId()

                                // DB에 없는 아이돌이 있으면
                                val idol = getIdolByIdUseCase(id)
                                    .mapDataResource { it }
                                    .awaitOrThrow()

                                if(idol == null) {
                                    logE("no exist idol ${model.getId()}")
                                    updateAllNeeded = true
                                    break
                                }

                                // 변경이 있는 것만 업데이트
                                val anniversary = model.anniversary
                                val anniversaryDays = model.anniversaryDays
                                val burningDay = model.burningDay
                                if(idol.anniversary != anniversary || idol.anniversaryDays != anniversaryDays || idol.burningDay != burningDay) {
                                    idol.anniversary = anniversary ?: "N"
                                    idol.anniversaryDays = anniversaryDays
                                    idol.burningDay = idol.burningDay
                                    updatedIdols.add(idol)
                                }
                            }

                            // 전체 업데이트 해야해서 updateAllNeeded true면 로직 탈 필요 없음
                            if (!updateAllNeeded) {
                                // DB에 기록
                                val hasUpdate = upsertIdolsWithTsUseCase(updatedIdols, ts)
                                    .mapDataResource { it }
                                    .awaitOrThrow()

                                hasUpdate?.let {
                                    if (it) EventBus.sendEvent(true, Const.BROADCAST_MANAGER_MESSAGE)
                                }
                            }

                            // 아이돌 목록 갱신여부 확인
                            val daily = response.optString("daily_idol_update")
                            if(!daily.isEmpty() && daily_idol_update != daily) {
                                daily_idol_update = daily
                            }
                            updateDailyNeeded = false
                            save()
                            scope.launch(Dispatchers.Main) {
                                cb?.invoke(true)
                            }

                        }catch (e:JSONException){
                            e.printStackTrace()
                            scope.launch(Dispatchers.Main) {
                                cb?.invoke(false)
                            }
                        }
                    }else{
                        scope.launch(Dispatchers.Main) {
                            cb?.invoke(false)
                        }
                    }
                }
        }
    }

    // 아이돌 하나를 콕 찝어서 업데이트.
    fun updateOne(id: Int): Job {
        return scope.launch(Dispatchers.IO) {
            logI("중복 호출 체크용 로직Call list_for_search api $id")
            idolsRepository.getIdolsForSearch(
                id = id,
                onServerTime = {
                    GlobalVariable.ServerTs = it
                },
                listener = { response ->
                    scope.launch(Dispatchers.IO) {
                        val gson = IdolGson.getInstance()
                        if(response?.optBoolean("success") ?: false){
                            try{
                                val array = response.getJSONArray("objects")
                                if( array.length() == 0) {
                                    return@launch
                                }
                                val ts = GlobalVariable.ServerTs

                                val updatedIdols = ArrayList<Idol>()
                                val model = gson.fromJson(array.getJSONObject(0).toString(), IdolModel::class.java)
                                updatedIdols.add(model.toDomain())

                                val hasUpdate = upsertIdolsWithTsUseCase(updatedIdols, ts)
                                    .mapDataResource { it }
                                    .awaitOrThrow()

                                hasUpdate?.let {
                                    if (it) EventBus.sendEvent(true, Const.BROADCAST_MANAGER_MESSAGE)
                                }
                            }catch (e:JSONException){
                                e.printStackTrace()
                            }
                        }
                    }
                },
                errorListener = {}
            )
        }
    }

    // 투표 후 top3 변경 감지 또는 제외된 아이돌 투표/탑3 변경 감지
    private fun updateHeartTop3(idolIds: ArrayList<Int>) {
        if( idolIds.isEmpty() ) {
            return
        }
        val ids = idolIds.joinToString(",")
        // idol_by_ids 호출
        scope.launch(Dispatchers.IO) {
            idolsRepository.getIdolsByIds(
                ids = ids,
                fields = "heart,top3",
                onServerTime = {
                    GlobalVariable.ServerTs = it
                },
                listener = { response ->
                    val gson = IdolGson.getInstance()
                    if(response.optBoolean("success")) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val array = response.getJSONArray("objects")
                                val updatedIdols = ArrayList<IdolFiledData>()

                                for (i in 0 until array.length()) {
                                    val model = gson.fromJson(
                                        array.getJSONObject(i).toString(),
                                        IdolCapriciousFieldModel::class.java
                                    )
                                    val id = model.id

                                    // DB에 없는 아이돌이 있으면
                                    val idol = getIdolByIdUseCase(id)
                                        .mapDataResource { it }
                                        .awaitOrThrow()

                                    if (idol == null) {
                                        logE("inAfterVoteWithTs")
                                        updateAllNeeded = true
                                        break
                                    }

                                    // heart, top3 변경이 있는 것만 업데이트
                                    val heart = model.heart
                                    val top3 = model.top3
                                    if (idol.heart != heart || idol.top3 != top3) {
                                        updatedIdols.add(model.toDomain())
                                    }
                                }

                                // DB에 기록
                                if (!updateAllNeeded) {
                                    val cdnUrl = ConfigModel.getInstance(context).cdnUrl ?: ""
                                    val reqImageSize = Util.getOnDemandImageSize(context)
                                    updateHeartAndTop3UseCase(
                                        cdnUrl,
                                        reqImageSize,
                                        updatedIdols
                                    ).collectLatest {
                                        logD("call updateHeartAndTop3UseCase")
                                    }
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                },
                errorListener = {

                }
            )
        }
    }

    // info-ver 갱신된 아이돌 업데이트
    // birthday가 변경되면 anniversary, anniversary_days도 변경되어야 하므로 이것들도 같이 요청
    fun updateIdols(idolIds: ArrayList<Int>, cb: ( (JSONArray) -> Unit )?) {
        if( idolIds.isEmpty() ) {
            scope.launch(Dispatchers.Main) {
                cb?.invoke(JSONArray())
            }
            return
        }
        val ids = idolIds.joinToString(",")

        // idol_by_ids 호출
        scope.launch(Dispatchers.IO) {
            idolsRepository.getIdolsByIds(
                ids = ids,
                fields = "",
                onServerTime = {
                    GlobalVariable.ServerTs = it
                },
                listener = { response ->
                    scope.launch(Dispatchers.IO) {
                        val gson = IdolGson.getInstance()
                        if(response?.optBoolean("success") == true){
                            try{
                                val array = response.getJSONArray("objects")
                                val ts = GlobalVariable.ServerTs

                                val updatedIdols = mutableListOf<Idol>()

                                for(i in 0 until array.length()){
                                    val model = gson.fromJson(array.getJSONObject(i).toString(), IdolModel::class.java)
                                    val id = model.getId()

                                    val deferred = async(Dispatchers.IO) {
                                        val idol = getIdolByIdUseCase(id)
                                            .mapDataResource { it?.toPresentation() }
                                            .awaitOrThrow()

                                        if (idol == null) {
                                            return@async null // ID 존재 안함
                                        } else {
                                            return@async idol.toDomain()
                                        }
                                    }

                                    val result = deferred.await()

                                    if (result == null) {
                                        logE("in updateIdols")
                                        updateAllNeeded = true
                                        break // 더 이상 진행 불필요
                                    } else {
                                        if (model.fdName == null) {
                                            model.fdName = result.fdName
                                        }
                                        updatedIdols.add(model.toDomain())
                                    }
                                }

                                if (!updateAllNeeded) {
                                    val hasUpdate = upsertIdolsWithTsUseCase(updatedIdols, ts)
                                        .mapDataResource { it }
                                        .awaitOrThrow()
                                    hasUpdate?.let {
                                        withContext(Dispatchers.Main) {
                                            cb?.invoke(array)
                                        }
                                    }
                                }

                            }catch (e:JSONException){
                                e.printStackTrace()
                                scope.launch(Dispatchers.Main) {
                                    cb?.invoke(JSONArray())
                                }
                            }
                        }else{
                            scope.launch(Dispatchers.Main) {
                                cb?.invoke(JSONArray())
                            }
                        }
                    }
                },
                errorListener = {
                    cb?.invoke(JSONArray())
                }
            )
        }
    }

    // 전체 아이돌 목록 프사/하트수를 갱신하고 등록된 콜백을 호출. 필요에 따라 데일리갱신/전체 갱신을 수행.
    fun update() {
        val ts = (Date().time / 1000).toInt()
        if(updateAllNeeded) {
            logD("call update all")
            updateAll {
                lastUpdateTs = ts
            }
        } else if(updateDailyNeeded) {
            logD("call update dailt")
            updateDaily {
                lastUpdateTs = ts
            }
        } else {
            logD("call update legacy")
            updateLegacy {
                lastUpdateTs = ts
            }
        }
    }

    // 제외된 아이돌이 즐찾에 포함되어 있는 경우 대비
    fun updateExcludedFavorites() {
        val excluded = ArrayList<Int>()
        val gson = IdolGson.getInstance()

        scope.launch(Dispatchers.IO) {
            favoritesRepository.getFavoritesSelf(
                { response ->
                    val array = response.optJSONArray("objects")
                    if(array != null){
                        for(i in 0 until array.length()) {
                            val model = gson.fromJson(array.getJSONObject(i).getJSONObject("idol").toString(), IdolModel::class.java)
                            if(model.isViewable != "Y") {
                                excluded.add(model.getId())
                            }
                        }
                    }

                    updateHeartTop3(excluded)
                }, {}
            )
        }
    }
}