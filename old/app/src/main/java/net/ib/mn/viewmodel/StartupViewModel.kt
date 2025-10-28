package net.ib.mn.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.FeedActivity.Companion.SIZE_OF_ARTICLE_LIMIT
import net.ib.mn.activity.FeedActivity.Companion.SIZE_OF_PHOTO_LIMIT
import net.ib.mn.activity.FeedActivity.Companion.offIconResId
import net.ib.mn.activity.FeedActivity.Companion.onIconResId
import net.ib.mn.addon.IdolGson
import net.ib.mn.common.util.logD
import net.ib.mn.common.util.logE
import net.ib.mn.common.util.logI
import net.ib.mn.common.util.logV
import net.ib.mn.core.domain.usecase.GetAdTypeListUseCase
import net.ib.mn.core.domain.usecase.GetAwardDataUseCase
import net.ib.mn.core.domain.usecase.GetAwardIdolUseCase
import net.ib.mn.core.domain.usecase.GetBlockIsOnlyUseCase
import net.ib.mn.core.domain.usecase.GetConfigSelfUseCase
import net.ib.mn.core.domain.usecase.GetConfigStartupUseCase
import net.ib.mn.core.domain.usecase.GetCouponMessage
import net.ib.mn.core.domain.usecase.GetOfferWallRewardUseCase
import net.ib.mn.core.domain.usecase.GetTypeListUseCase
import net.ib.mn.core.domain.usecase.GetUpdateInfoUseCase
import net.ib.mn.core.domain.usecase.GetUserSelfUseCase
import net.ib.mn.core.domain.usecase.UpdateTimeZoneUseCase
import net.ib.mn.core.domain.usecase.idols.GetIdolsWithFieldsUseCase
import net.ib.mn.core.model.AwardModel
import net.ib.mn.core.model.BadWordModel
import net.ib.mn.idols.IdolApiManager
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.model.BaseModel
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.model.InAppBannerType
import net.ib.mn.domain.usecase.DeleteAllAwardsIdolUseCase
import net.ib.mn.domain.usecase.GetViewableIdolsUseCase
import net.ib.mn.domain.usecase.SaveAwardsIdolUseCase
import net.ib.mn.domain.usecase.UpdateAnniversariesUseCase
import net.ib.mn.domain.usecase.datastore.SetInAppBannerPrefsUseCase
import net.ib.mn.model.AnniversaryModel
import net.ib.mn.model.InAppBannerModel
import net.ib.mn.model.toDomain
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.MessageManager
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.livedata.Event
import net.ib.mn.utils.modelToString
import net.ib.mn.utils.sort
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val idolApiManager: IdolApiManager,
    private val getConfigStartupUseCase: GetConfigStartupUseCase,
    private val getUpdateInfoUseCase: GetUpdateInfoUseCase,
    private val getAdTypeListUseCase: GetAdTypeListUseCase,
    private val updateTimeZoneUseCase: UpdateTimeZoneUseCase,
    private val getConfigSelfUseCase: GetConfigSelfUseCase,
    private val getAwardDataUseCase: GetAwardDataUseCase,
    private val getAwardIdolUseCase: GetAwardIdolUseCase,
    private val getCouponMessage: GetCouponMessage,
    private val getUserSelfUseCase: GetUserSelfUseCase,
    private val getOfferWallRewardUseCase: GetOfferWallRewardUseCase,
    private val getTypeListUseCase: GetTypeListUseCase,
    private val getBlockIsOnlyUseCase: GetBlockIsOnlyUseCase,
    private val getIdolsWithFieldsUseCase: GetIdolsWithFieldsUseCase,
    private val supportRepository: SupportRepositoryImpl,
    private val getViewableIdolsUseCase: GetViewableIdolsUseCase,
    private val updateAnniversariesUseCase: UpdateAnniversariesUseCase,
    private val saveAwardsIdolUseCase: SaveAwardsIdolUseCase,
    private val deleteAllAwardsIdolUseCase: DeleteAllAwardsIdolUseCase,
    private val setInAppBannerPrefsUseCase: SetInAppBannerPrefsUseCase,
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _configLoadComplete = MutableLiveData<Event<Boolean>>()
    val configLoadComplete: LiveData<Event<Boolean>> = _configLoadComplete

    private val _recreateActivity = MutableLiveData<Event<Boolean>>()
    val recreateActivity: LiveData<Event<Boolean>> = _recreateActivity

    private val _updateProgress = MutableLiveData<Event<Float>>()
    val updateProgress: LiveData<Event<Float>> = _updateProgress

    private var progress = 0.0f
    private var progressStep = 0.0f

    private var configLoadResult = true

    private var didShowToast = AtomicBoolean(false)

    fun getInAppBanner(context: Context) {
        viewModelScope.launch {
            supportRepository.getInAppBanner(
                { response ->
                    if (response.optBoolean("success")) {
                        val result = response.optJSONArray("objects")?.toString()
                        result?.let { jsonString ->
                            viewModelScope.launch(Dispatchers.IO) {
                                val json = Json { ignoreUnknownKeys = true }

                                try {
                                    val bannerList: List<InAppBannerModel> = json.decodeFromString(jsonString)

                                    val bannerGroup = bannerList.groupBy { it.section }.toMutableMap()

                                    if (!bannerGroup.containsKey(InAppBannerType.SEARCH.label)) {
                                        bannerGroup[InAppBannerType.SEARCH.label] = emptyList()
                                    }

                                    if (!bannerGroup.containsKey(InAppBannerType.MENU.label)) {
                                        bannerGroup[InAppBannerType.MENU.label] = emptyList()
                                    }

                                    val domainBannerGroup: Map<String, List<InAppBanner>> = bannerGroup.mapValues { entry ->
                                        entry.value.map { it.toDomain() }
                                    }

                                    setInAppBannerPrefsUseCase(domainBannerGroup).collect{}

                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        UtilK.handleCommonError(context, response)
                    }
                }, { throwable ->
                    Toast.makeText(context, throwable.message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    fun getStartApi(
        context: Context,
        account: IdolAccount?,
        isFromIdol: Boolean,
        isUserBlockFirst: Boolean,
        to: String
    ) = viewModelScope.launch {
        // configs/self에 있는 설정값들을 다른 곳에서 쓰기때문에 가장 먼저 불러온다
        async { getConfigSelf(context) }.await()

        val isStartupSuccess = async { getConfigStartup(context) }.await()
        if (!isStartupSuccess) {
            return@launch
        }

        // 비동기 작업들을 동시에 실행getStartApi
        val tasks = listOf(
            async { getUpdateInfo(context) },
            async { getAdTypeList(context) },
            async { timeZoneUpdate(context) },
            async { getConfigSelf(context) },
            async { getMessageCoupon(context) },
            async { getUserSelf(context, account) },
            async { getUserStatus(account) }
        ).toMutableList()

        if (isFromIdol) {
            tasks.add(async { getOfferWallReward(to) })
        }

        if (BuildConfig.CELEB) {
            tasks.add(async { getTypeList(context) })
        }

        if (isUserBlockFirst) {
            tasks.add(async { getBlocks(context, "Y") })
        }

        val divisor = tasks.size
        val dividend = 100.0f
        progressStep = dividend / divisor

        // 모든 작업이 완료될 때까지 대기
        tasks.awaitAll()

        _configLoadComplete.value = Event(configLoadResult)
    }

    private fun handleError(result: BaseModel<*>) {
        viewModelScope.launch(Dispatchers.Main) {
            if(!result.success) {
                configLoadResult = false
                // 토스트가 여러개 뜨는것 방지
                if (didShowToast.compareAndSet(false, true)) {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    // 네트워크 연결 실패라면 예외 오류 토스트 출력
                    if(result.error is IOException) {
                        Toast.makeText(context, context.getString(R.string.desc_failed_to_connect_internet), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private suspend fun getUpdateInfo(context: Context) {
        getUpdateInfoUseCase().collectLatest { result ->

            progress += progressStep
            Logger.v("ApiLogger", result.modelToString())


            idolApiManager.startTimer()

            if (!result.success) {
                handleError(result)

                if (!BuildConfig.CELEB) {
                    Util.setPreference(
                        context,
                        Const.PREF_OFFICIAL_CHANNEL_UPDATE,
                        ""
                    )
                    Util.setPreference(
                        context,
                        Const.PREF_SHOULD_CALL_OFFICIAL_CHANNEL,
                        true
                    )
                }
                Util.setPreference(
                    context,
                    Const.PREF_ALL_IDOL_UPDATE,
                    ""
                )
                Util.setPreference(
                    context,
                    Const.PREF_DAILY_IDOL_UPDATE,
                    ""
                )
                return@collectLatest
            }

            val allIdolUpdateFlag = Util.getPreference(
                context,
                Const.PREF_ALL_IDOL_UPDATE
            )
            val dailyIdolUpdateFlag = Util.getPreference(
                context,
                Const.PREF_DAILY_IDOL_UPDATE
            )
            val officialChannelUpdateFlag = Util.getPreference(
                context,
                Const.PREF_OFFICIAL_CHANNEL_UPDATE
            )

            idolApiManager.load()

            val allIdolUpdate = result.data?.allIdolUpdate
            val dailyIdolUpdate = result.data?.dailyIdolUpdate

            Log.d("!!!!", "$allIdolUpdate $dailyIdolUpdate")


            val officialChannelUpdate =
                result.data?.snsChannelUpdate

            // 배우자는 공식 채널 x
            if (!BuildConfig.CELEB) {
                // 공식 채널 가져올지에 대한 flag
                if (officialChannelUpdateFlag == null || !officialChannelUpdateFlag.equals(
                        officialChannelUpdate,
                        ignoreCase = true
                    )
                ) {
                    Util.setPreference(
                        context,
                        Const.PREF_SHOULD_CALL_OFFICIAL_CHANNEL,
                        true
                    )
                    Util.setPreference(
                        context,
                        Const.PREF_OFFICIAL_CHANNEL_UPDATE,
                        officialChannelUpdate
                    )
                } else {
                    Util.setPreference(
                        context,
                        Const.PREF_SHOULD_CALL_OFFICIAL_CHANNEL,
                        false
                    )
                }
            }

            idolApiManager.daily_idol_update = dailyIdolUpdate ?: ""
            idolApiManager.updateDaily {
                _updateProgress.value = Event(progress)
            }

            logV("allIdolFlag $allIdolUpdateFlag $allIdolUpdate")

            if (allIdolUpdateFlag.isNullOrEmpty() ||
                !allIdolUpdateFlag.equals(
                    allIdolUpdate,
                    ignoreCase = true
                )
            ) {
                Log.d("!!!!", "111111111111 $allIdolUpdate $dailyIdolUpdate")
                // 전체 아이돌 다시 받아오기
                idolApiManager.all_idol_update = allIdolUpdate ?: ""
                idolApiManager.daily_idol_update = dailyIdolUpdate ?: ""

                logD("call update all start")
                idolApiManager.updateAll {
                    _updateProgress.value = Event(progress)
                }
            } else if (dailyIdolUpdateFlag != dailyIdolUpdate) {
                Log.d("!!!!", "22222222222 $allIdolUpdate $dailyIdolUpdate")
                idolApiManager.daily_idol_update = dailyIdolUpdate ?: ""
                idolApiManager.updateDaily {
                    _updateProgress.value = Event(progress)
                }
            } else {
                Log.d("!!!!", "33333333333 $allIdolUpdate $dailyIdolUpdate")
                loadIdolsFromDB(context, dailyIdolUpdateFlag, dailyIdolUpdate)
            }
        }
    }

    private suspend fun getConfigStartup(context: Context): Boolean {
        var isSuccess = false

        getConfigStartupUseCase().collectLatest { result ->
            Logger.v("ApiLogger", result.modelToString())

            // 네트워크 예외 처리
            if(!result.success) {
                // TODO 현재 여기서 handleError 의미가 없다고 판단하여 일단 주석처리함
//                handleError(result)
                return@collectLatest
            }

            isSuccess = true

            val gson = IdolGson.getInstance(false)
            val wordListType =
                object : TypeToken<List<BadWordModel>>() {}.type

            //비속어 저장
            Util.setPreference(context, Const.BAD_WORDS, gson.toJson(result.data?.badword))
            IdolAccount.badWords = gson.fromJson<List<BadWordModel>>(
                Util.getPreference(
                    context,
                    Const.BAD_WORDS
                ), wordListType
            ) as ArrayList<BadWordModel>

            setTag("board_tag", gson.toJson(result.data?.boardTag), context)

            if (!BuildConfig.CELEB) {
                // lgcode
                if (ConfigModel.getInstance(context).showLg != null &&
                    ConfigModel.getInstance(context).showLg.equals(
                        "Y",
                        ignoreCase = true
                    )
                ) {
                    Util.setPreference(
                        context,
                        Const.LG_CODE,
                        gson.toJson(result.data?.lgcode)
                    )
                }

                Util.setPreference(
                    context,
                    Const.PREF_OFFICIAL_CHANNELS,
                    gson.toJson(result.data?.sns)
                )
            }

            // 공지사항.
            Util.setPreference(
                context,
                Const.PREF_NOTICE_LIST,
                result.data?.noticeList.toString()
            )

            // 이벤트.
            Util.setPreference(
                context,
                Const.PREF_EVENT_LIST,
                result.data?.eventList.toString()
            )

            //앱버전 체크.
            Util.setPreference(
                context,
                Const.PREF_FAMILY_APP_LIST,
                gson.toJson(result.data?.familyAppList)
            )

            // video 기본 설정 값
            Util.setPreference(
                context,
                Const.PERF_UPLOAD_VIDEO_SPEC,
                gson.toJson(result.data?.uploadVideoSpec)
            )

            // 종료 팝업
            Util.setPreference(
                context,
                Const.PREF_END_POPUP,
                gson.toJson(result.data?.endPopup)
            )

            // 하트픽 New 표시.
            Util.setPreference(
                context,
                Const.PREF_NEW_PICKS,
                gson.toJson(result.data?.newPicks)
            )

            Util.setPreference(
                context,
                Const.PREF_HELP_INFO,
                gson.toJson(result.data?.helpInfos)
            )

            progress += progressStep
            _updateProgress.value = Event(progress)
        }

        return isSuccess
    }

    private suspend fun getAdTypeList(context: Context) {
        getAdTypeListUseCase().collectLatest { result ->
            if(!result.success) {
                handleError(result)
                return@collectLatest
            }

            Logger.v("ApiLogger", result.modelToString())

            val gson = IdolGson.getInstance(false)

            Util.setPreference(
                context,
                Const.AD_TYPE_LIST,
                gson.toJson(result.data).toString()

            )

            // 광고 디자인 공모전 안내
            Util.setPreference(
                context,
                Const.AD_GUIDANCE,
                result.description
            )

            progress += progressStep
            _updateProgress.value = Event(progress)
        }
    }

    private suspend fun timeZoneUpdate(context: Context) {
        val calendar = Calendar.getInstance()
        val currentTimeZone =
            calendar.timeZone.getDisplayName(false, TimeZone.LONG, Locale.ENGLISH)
        val storedTimeZone =
            Util.getPreference(context, Const.TIME_ZONE)

        val timeFormat = SimpleDateFormat("ZZZZZ", Locale.getDefault())
        val resultTime = timeFormat.format(System.currentTimeMillis())
        val timeZoneData = mapOf("timezone" to resultTime)

        updateTimeZoneUseCase(timeZoneData).collectLatest { result ->
            if(!result.success) {
                handleError(result)
                return@collectLatest
            }

            Logger.v("ApiLogger", result.modelToString())

            if (!storedTimeZone.equals(currentTimeZone) || storedTimeZone == null) {
                Util.setPreference(
                    context,
                    Const.TIME_ZONE,
                    currentTimeZone
                )
            }

            progress += progressStep
            _updateProgress.value = Event(progress)
        }
    }

    private suspend fun getConfigSelf(context: Context) {
        withContext(Dispatchers.IO) {
            val result = getConfigSelfUseCase().first()
            if(!result.success) {
                handleError(result)
                return@withContext
            }
            Logger.v("ApiLogger", result.modelToString())
            ConfigModel.getInstance(context).parse(result.data)

            if (ConfigModel.getInstance(context).showAwardTab) {
                getAwardData(context)
            }
        }

        // 메인 스레드에서 UI 상태 업데이트
        withContext(Dispatchers.Main) {
            progress += progressStep
            _updateProgress.value = Event(progress)
        }
    }

    private suspend fun getAwardData(context: Context) {
        getAwardDataUseCase().collectLatest { result ->
            Logger.v("ApiLogger", result.modelToString())

            val jsonString = Json.encodeToString(result)

            Util.setPreference(
                context,
                Const.AWARD_MODEL,
                jsonString
            )

            Logger.v("awardModel :: ${result.modelToString()}")

            // 아래 코멘트 참조
            getAllAwardsIdolList(context, result)
        }
    }

    private suspend fun getAllAwardsIdolList(context: Context, data: AwardModel?) {
        val chartModel = data?.charts
        val charCodes = chartModel?.map { it.code } ?: listOf()
//        val chartCodeQuery = charCodes.joinToString(",")
        // chartCodes를 ,로 붙여서 한꺼번에 가져오는 api는 없애기로 했으므로 각각 불러준다
        viewModelScope.launch(Dispatchers.IO) {
            deleteAllAwardsIdolUseCase().collectLatest {
                supervisorScope {
                    charCodes.forEach { chartCode ->
                        if (chartCode == null) return@forEach

                        launch {
                            getAwardIdolUseCase(chartCode).collect { result ->
                                if (!result.success) {
                                    return@collect
                                }

                                val gson = IdolGson.getInstance()
                                val listType = object : TypeToken<ArrayList<IdolModel>>() {}.type
                                val idolList = gson.fromJson<ArrayList<IdolModel>>(
                                    gson.toJson(result.data),
                                    listType
                                )

                                val idols = sort(context, idolList)

                                saveAwardsIdolUseCase(idols.map { it.toDomain() })
                                    .catch { logE("SaveAwardsIdolUseCase", "예외: ${it.message}") }
                                    .collect {
                                        logD("SaveAwardsIdolUseCase 저장 성공")
                                    }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getMessageCoupon(context: Context) {
        MessageManager.shared().getCoupons(context, getCouponMessage) {
            progress += progressStep
            _updateProgress.value = Event(progress)
        }
    }

    private suspend fun getUserSelf(context: Context, account: IdolAccount?) {
        account?.mDailyPackHeart = 0
        account?.saveAccount(context)

        val ts: Int = account?.userModel?.ts ?: 0

        getUserSelfUseCase(ts).collectLatest { result ->
            Logger.v("ApiLogger", result.modelToString())
            try {
                if( result.code == 401 ) {
                    // 401 unauthorized라면 로그인으로 보냄
                    configLoadResult = false
                    account?.clearAccount(context)
                    _recreateActivity.value = Event(true)
                    return@collectLatest
                }
                if (result.code == 304) {
                    Logger.v("ApiLogger", "users/self 304 ${result.message}")
                } else {
                    result.data?.let {
                        account?.setUserInfo(
                            context,
                            it
                        )
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            progress += progressStep
            _updateProgress.value = Event(progress)
        }
    }

    private suspend fun getOfferWallReward(to: String) {
        getOfferWallRewardUseCase(to).collectLatest { result ->
            if(!result.success) {
                handleError(result)
                return@collectLatest
            }
            Logger.v("ApiLogger", result.modelToString())

            // do nothing
            progress += progressStep
            _updateProgress.value = Event(progress)
        }
    }

    suspend fun getTypeList(context: Context) {
        getTypeListUseCase().collectLatest { result ->
            Logger.v("ApiLogger", result.modelToString())
            try {
                if(!result.success) {
                    handleError(result)
                    return@collectLatest
                }

                if (result.success && result.data != null) {
                    val arrayTypeList: ArrayList<TypeListModel> = result.data!!.toCollection(ArrayList())

                    for (i in arrayTypeList.indices) {
                        arrayTypeList[i].type?.let {
                            if (it == "A" || it == "S") {
                                arrayTypeList[i].isDivided = "Y"
                            }
                        }
                    }

                    for (i in arrayTypeList.indices) {
                        if (arrayTypeList[i].isDivided == "Y") {
                            val model = UtilK.deepCopy(arrayTypeList[i])
                            model.isDivided =
                                "N" // N으로 만드는 이유는 Y로 했을 경우 무한루프가 돌 수 있음.
                            model.isFemale = true // Y인 경우 여자가 있는 경우이므로 추가.
                            model.showDivider = true // 구분선 보여주기
                            arrayTypeList.add(i + 1, model)
                        }
                    }

                    // 20250408 해외 배우 카테고리 끼워넣기
                    // 먼저 type이 G인 카테고리를 찾는다.
                    val index = arrayTypeList.indexOfFirst { it.type == "G" }
                    if(index != -1) {
                        val model = arrayTypeList[index]
                        model.showDivider = true // 구분선 보여주기

                        // 그 다음에 type이 A이고 isFemale이 true인 카테고리를 찾는다.
                        val insertIndex = arrayTypeList.indexOfFirst { it.type == "A" && it.isFemale }
                        if(insertIndex != -1) {
                            // 원래 있던 자리에서 삭제한다.
                            arrayTypeList.removeAt(index)
                            // AG 카테고리 다음에 A 카테고리를 넣는다.
                            arrayTypeList.add(insertIndex + 1, model)
                            // 여자배우 카테고리에 구분선 제거
                            arrayTypeList[insertIndex].showDivider = false
                        }
                    }

                    Util.setPreference(
                        context,
                        Const.PREF_TYPE_LIST,
                        IdolGson.getInstance().toJson(arrayTypeList)
                    )
                    UtilK.initSetMainCheck(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            progress += progressStep
            _updateProgress.value = Event(progress)
        }
    }

    private suspend fun getBlocks(context: Context, idOnly: String) {
        getBlockIsOnlyUseCase(idOnly).collectLatest { result ->
            if(!result.success) {
                handleError(result)
                return@collectLatest
            }

            Logger.v("ApiLogger", result.modelToString())
            val setUserBlockList = ArrayList<Int>()
            val array = result.data?.optJSONArray("block_ids")
            array?.let {
                for (i in 0 until it.length()) {
                    try {
                        setUserBlockList.add(it.getInt(i))
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
            Util.setPreferenceArray(
                context,
                Const.USER_BLOCK_LIST,
                setUserBlockList
            )
            Util.setPreference(
                context,
                Const.USER_BLOCK_FIRST,
                true
            )

            progress += progressStep
            _updateProgress.value = Event(progress)
        }
    }


    private fun setTag(tag: String, obj: String, context: Context) {
        val (tagsKey, selectedKey) = Pair(Const.BOARD_TAGS, Const.SELECTED_TAG_IDS)

        val boardTags = Util.getPreference(context, tagsKey)

        if (boardTags.isEmpty()) {
            Util.setPreference(context, tagsKey, obj)

            try {
                // selectedTagIds를 "0"으로 초기화 (HOT 태그)
                val selectedTagIds = "0"
                Util.setPreference(context, selectedKey, selectedTagIds)

            } catch (e: Exception) {
                e.printStackTrace()
                Util.setPreference(context, tagsKey, obj)
            }
        } else {
            Util.setPreference(context, tagsKey, obj)
        }
    }

    private suspend fun loadIdolsFromDB(
        context: Context,
        dailyIdolUpdateFlag: String?,
        dailyIdolUpdate: String?
    ) = viewModelScope.launch(Dispatchers.IO) {
        // DB에서 한 번 읽어오기
//        val idolListInstance = IdolList.getInstance(context)
//        val idols = idolListInstance.fetchIdols()

        val idols = getViewableIdolsUseCase()
            .mapDataResource { it }
            .awaitOrThrow() ?: return@launch

        logI("loadDBB ${idols.size}")

        if (idols.isEmpty()) {
            Util.setPreference(
                context,
                Const.PREF_ALL_IDOL_UPDATE,
                ""
            )
            Util.setPreference(
                context,
                Const.PREF_DAILY_IDOL_UPDATE,
                ""
            )
            withContext(Dispatchers.Main) {
                _recreateActivity.postValue(Event(true))
            }
            return@launch
        }

        // TODO unman 필요 없는 로직 같아서 보류
//        saveIdolsUseCase(idols).collectLatest {
//            if (dailyIdolUpdateFlag.isNullOrEmpty() || dailyIdolUpdateFlag != dailyIdolUpdate) {
//                updateAnniversaries(context, dailyIdolUpdate)
//            } else {
//                _updateProgress.value = Event(progress)
//            }
//        }

        if (dailyIdolUpdateFlag.isNullOrEmpty() || dailyIdolUpdateFlag != dailyIdolUpdate) {
            updateAnniversaries(context, dailyIdolUpdate)
        } else {
            _updateProgress.postValue(Event(progress))
        }
    }

    private suspend fun getUserStatus(idolAccount: IdolAccount?) {
        usersRepository.getStatus(
            userId = idolAccount?.userId ?: return,
            listener = { response ->
                val bitmask = response.optLong("tutorial")
                TutorialManager.init(bitmask)
            },
            errorListener = {
                // no - op
            }
        )
    }

    private suspend fun updateAnniversaries(context: Context, dailyIdolUpdate: String?) {
        Util.log("❌️️ DAILY IDOL UPDATE ❌️️")

        getIdolsWithFieldsUseCase(
            null,
            null,
            "anniversary,burning_day,heart,top3",
        ).collectLatest { result ->

            if (result.data?.optBoolean("success") != true) {
                _updateProgress.value = Event(progress)
                Util.setPreference(
                    context,
                    Const.PREF_DAILY_IDOL_UPDATE,
                    ""
                )
                return@collectLatest
            }

            Util.setPreference(
                context,
                Const.PREF_DAILY_IDOL_UPDATE,
                dailyIdolUpdate
            )

            updateAnniversariesDB(result.data?.optJSONArray("objects") ?: return@collectLatest)
        }
    }

    // FIXME unman jsonArray 사용하는거 제거 (위 API 사용 코드부터 다시 바꿔야함)
    private fun updateAnniversariesDB(anniversaries: JSONArray) = viewModelScope.launch(Dispatchers.IO) {
        val gson: Gson = IdolGson.getInstance()

        val cdnUrl = ConfigModel.getInstance(context).cdnUrl
        val reqImageSize = Util.getOnDemandImageSize(context)

        val models: List<AnniversaryModel> = (0 until anniversaries.length()).map { index ->
            gson.fromJson(anniversaries.getJSONObject(index).toString(), AnniversaryModel::class.java)
        }

        updateAnniversariesUseCase(cdnUrl ?: return@launch, reqImageSize, models.map { it.toDomain() })
    }
}