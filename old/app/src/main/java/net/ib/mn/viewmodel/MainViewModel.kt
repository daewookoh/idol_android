package net.ib.mn.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.util.Log
import android.util.SparseIntArray
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity.Companion.PARAM_NEXT_INTENT
import net.ib.mn.addon.IdolGson
import net.ib.mn.base.BaseViewModel
import net.ib.mn.common.util.logE
import net.ib.mn.core.data.model.ChartCodeInfo
import net.ib.mn.core.data.model.MainChartModel
import net.ib.mn.core.data.model.RecordRoomModel
import net.ib.mn.core.data.repository.EmoticonRepository
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.language.LanguagePreferenceRepository
import net.ib.mn.core.domain.usecase.GetChartsUseCase
import net.ib.mn.core.domain.usecase.GetEventUseCase
import net.ib.mn.core.domain.usecase.GetIdolsChartCodesUseCase
import net.ib.mn.core.model.ChartModel
import net.ib.mn.emoticon.ZipManager
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.EmoticonDetailModel
import net.ib.mn.model.EmoticonsetModel
import net.ib.mn.model.EventHeartModel
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.datastore.GetIdolChartCodesPrefsUseCase
import net.ib.mn.domain.usecase.datastore.SetIdolChartCodePrefsUseCase
import net.ib.mn.feature.friend.InvitePayload
import net.ib.mn.model.IdolModel
import net.ib.mn.model.RankingModel
import net.ib.mn.model.StoreItemModel
import net.ib.mn.model.SupportListModel
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.ExtendedDataHolder
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.VMDetector
import net.ib.mn.utils.livedata.Event
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class MainViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getChartsUseCase: GetChartsUseCase,
    private val getIdolsChartCodesUseCase: GetIdolsChartCodesUseCase,
    private val getIdolChartCodesPrefsUseCase: GetIdolChartCodesPrefsUseCase,
    private val setIdolChartCodePrefsUseCase: SetIdolChartCodePrefsUseCase,
    private val getEventUseCase: GetEventUseCase,
    private val usersRepository: UsersRepository,
    private val emoticonRepository: EmoticonRepository,
    private val languagePreferenceRepository: LanguagePreferenceRepository,
) : BaseViewModel() {
    @Inject
    lateinit var supportRepository: SupportRepositoryImpl
    @Inject
    lateinit var miscRepository: MiscRepository

    private val _liveChart =
        MutableLiveData<Triple<MainChartModel, Map<String, List<ChartModel>>, Intent?>>()
    val liveChart: LiveData<Triple<MainChartModel, Map<String, List<ChartModel>>, Intent?>> get() = _liveChart

    private val _eventBanner = MutableLiveData<Event<EventHeartModel?>>()
    val eventBanner: LiveData<Event<EventHeartModel?>> = _eventBanner

    private val _showWelcomeMission = MutableLiveData<Event<Boolean?>>()
    val showWelcomeMission: LiveData<Event<Boolean?>> = _showWelcomeMission

    private val _moveToSupportScreen = MutableLiveData<Event<SupportListModel>>()
    val moveToSupportScreen: LiveData<Event<SupportListModel>> = _moveToSupportScreen

    private val _moveToSubscriptionScreen = MutableLiveData<Event<Pair<StoreItemModel, String>>>()
    val moveToSubscriptionScreen: LiveData<Event<Pair<StoreItemModel, String>>> =
        _moveToSubscriptionScreen

    private var intentOfLink: Intent? = null
    private var _intentOfLinkObserve = MutableLiveData<Event<Intent>>()
    var intentOfLinkObserve: LiveData<Event<Intent>> = _intentOfLinkObserve


    private val topNavigationTab: MutableList<View> = ArrayList()
    private var currentNavigationTabIdx: Int = 0

    private val _selectedBottomNavIndex =
        savedStateHandle.getStateFlow(BOTTOM_NAV_INDEX_STATE, 0)
    val selectedBottomNavIndex: StateFlow<Int> = _selectedBottomNavIndex

    private val _isMaleGender = MutableLiveData<Event<Boolean>>()
    val isMaleGender: LiveData<Event<Boolean>> = _isMaleGender

    private val _changeHallOfGender = MutableLiveData<Event<Boolean>>()
    val changeHallOfGender: LiveData<Event<Boolean>> = _changeHallOfGender

    private val _bottomTabIndex = MutableLiveData<Int>()
    val bottomTabIndex: LiveData<Int> = _bottomTabIndex

    private val _refreshMyHearData = MutableLiveData<Boolean>()
    val refreshMyHearData: LiveData<Boolean> = _refreshMyHearData

    private val _favoriteData = MutableLiveData<Map<String, List<RankingModel>>>()
    val favoriteData: LiveData<Map<String, List<RankingModel>>> = _favoriteData

    private val _mostIdolData = MutableLiveData<RankingModel>()
    val mostIdolData: LiveData<RankingModel> = _mostIdolData

    private val _inviteData = MutableLiveData<Event<InvitePayload>>()
    val inviteData: LiveData<Event<InvitePayload>> = _inviteData

    val tabWidths = MutableLiveData<SparseIntArray>()

    var mainChartModel: MainChartModel? = savedStateHandle.get("mainChartModel")
        private set

    private lateinit var historyChartModel: RecordRoomModel
    private lateinit var etcChartModel: Map<String, List<ChartModel>>

    fun setMainChartModel(mainChartModel: MainChartModel) {
        this.mainChartModel = mainChartModel
    }

    fun getHistoryChartModel() = historyChartModel

    fun getLiveChart(context: Context, fragment: Fragment) = viewModelScope.launch {
        getChartsUseCase().collect { response ->
            // response.message가 null이 아니면 에러 메시지를 처리
            response.message?.let {
                _errorToast.postValue(Event(it))
                return@collect
            }

            if (response.main == null ||
                response.recordRoom == null
            ) {
                return@collect
            }

            val chartResponse = response ?: return@collect
            val groupedChartList: Map<String, List<ChartModel>> =
                response.objects?.groupBy { it.type ?: "" } ?: emptyMap()

            response.main?.let {
                withContext(Dispatchers.IO) {
                    getIdolsChartCodesUseCase.invoke(it).collectLatest { data ->
                        setIdolChartCodePrefsUseCase(data).collect{}
                    }
                }
            }

            mainChartModel = response.main!!
            savedStateHandle.set("mainChartModel", mainChartModel)

            historyChartModel = chartResponse.recordRoom!!
            etcChartModel = groupedChartList

            _liveChart.value = Triple(mainChartModel!!, groupedChartList, intentOfLink)
        }
    }

    fun triggerEmoticonUnzipProcess(context: Context) {
        val cacheEmoticonVersion = Util.getPreferenceInt(context, Const.EMOTICON_VERSION, -1)
        val remoteEmoticonVersion = ConfigModel.getInstance(context).emoticonVersion

        if (cacheEmoticonVersion == remoteEmoticonVersion) {
            return
        }

        // 이모티콘 버전 세팅.
        Util.setPreference(
            context,
            Const.EMOTICON_VERSION,
            remoteEmoticonVersion,
        )

        getEmoticons(context)

    }

    // 서버에서 이모티콘을 리스트를 가져옵니다.
    private fun getEmoticons(context: Context) {
        viewModelScope.launch {
            emoticonRepository.getEmoticon(
                null,
                { response ->
                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(context, response)
                        _errorToast.value = Event(responseMsg ?: "")
                        return@getEmoticon
                    }

                    val emoGson = IdolGson.getInstance()
                    val emoListType = object : TypeToken<ArrayList<EmoticonsetModel>>() {}.type

                    val obj: JSONArray?
                    try {
                        obj = response.getJSONArray("emoticon_set")
                    } catch (e: JSONException) {
                        return@getEmoticon
                    }

                    // 가장 처음 시작했을 때.
                    if (Util.getPreference(context, Const.EMOTICON_SET).isNullOrEmpty()) {
                        val emoList: List<EmoticonsetModel> =
                            emoGson.fromJson(obj.toString(), emoListType)

                        for (i in emoList.indices) { // 가장 처음엔 모두다 다운받아야되므로 모두 true 처리해준다.
                            emoList[i].isChanged = true
                        }

                        val reveredList = emoList.toMutableList().reversed()
                        Util.setPreference(
                            context,
                            Const.EMOTICON_SET,
                            emoGson.toJson(reveredList),
                        )
                        // 이모티콘 받아오기.
                        downLoadZipFile(context)
                        return@getEmoticon
                    }

                    try {
                        // 기존 가지고있는 이모티콘.
                        val priorEmoList: ArrayList<EmoticonsetModel> =
                            emoGson.fromJson(
                                Util.getPreference(
                                    context,
                                    Const.EMOTICON_SET,
                                ),
                                emoListType,
                            )

                        // 서버에서 가지고온 이모티콘.
                        val newEmoList: ArrayList<EmoticonsetModel> =
                            emoGson.fromJson(obj.toString(), emoListType)

                        newEmoList.forEach { it.isChanged = true }

                        priorEmoList.forEach { prior ->
                            newEmoList.forEach { new ->
                                if (new.id == prior.id) {
                                    new.isChanged = new.version != prior.version
                                }
                            }
                        }

                        val reveredList = newEmoList.toMutableList().reversed()
                        Util.setPreference(
                            context,
                            Const.EMOTICON_SET,
                            emoGson.toJson(reveredList),
                        )
                        // 이모티콘 받아오기.
                        downLoadZipFile(context)
                    } catch (e: JsonSyntaxException) {
                        return@getEmoticon
                    }
                }, { throwable ->
                    _errorToast.value = Event(throwable.message ?: "")
                }
            )
        }
    }

    fun downLoadZipFile(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        var input: InputStream? = null
        var output: OutputStream? = null
        var connection: HttpURLConnection? = null
        val gson = IdolGson.getInstance()
        val emoListType = object : TypeToken<ArrayList<EmoticonsetModel>>() {}.type

        val emoList: ArrayList<EmoticonsetModel>
        try {
            emoList =
                gson.fromJson(Util.getPreference(context, Const.EMOTICON_SET), emoListType)
        } catch (e: NullPointerException) {
            e.printStackTrace()
            return@launch
        }

        val emoticonUrl = ConfigModel.getInstance(context).emoticonUrl

        // 이모티콘 SET마다 서버에서 이모티콘 zip파일을 불러옵니다.
        for (i in emoList.indices) {
            try {
                val url =
                    URL(emoticonUrl + File.separator + emoList[i].id + File.separator + emoList[i].id + ".zip")
                connection = url.openConnection() as HttpURLConnection
                connection.connect()

                input = connection.inputStream

                val zipFolder = File(context.filesDir.absolutePath + "/zipFile")
                if (!zipFolder.exists()) { // zipFolder 만들어져있나 확인(zip파일은 zipFolder, zip파일 압축푼거는 unzipped폴더에 있음).
                    zipFolder.mkdir()
                }

                output =
                    FileOutputStream(zipFolder.toString() + File.separator + emoList[i].id + ".zip")

                val data = ByteArray(4096)
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    output.write(data, 0, count)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    output?.close()
                    input?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        initEmoticon(context)
    }

    private fun initEmoticon(context: Context) {
        // 서버에서 불러온 이모티콘 zip파일 저장 경로.
        val zipFolder = File(context.filesDir.absolutePath + "/zipFile")

        try {
            val gson = IdolGson.getInstance()
            val emoListType = object : TypeToken<ArrayList<EmoticonsetModel>>() {}.type

            val emoList: ArrayList<EmoticonsetModel> =
                gson.fromJson(Util.getPreference(context, Const.EMOTICON_SET), emoListType)

            // 이모티콘 전체리스트 지워주고 다시 가져와줌.
            val emoAllInfoListType = object : TypeToken<List<EmoticonDetailModel>>() {}.type
            var emoticonAllInfoList: ArrayList<EmoticonDetailModel> = ArrayList()
            val emoticonAllInfo = Util.getPreference(context, Const.EMOTICON_ALL_INFO)
            if (emoticonAllInfo.isNotEmpty()) { // 빈값으로 들어있으면 removeIf 할때 exception으로 나와서 비어있나 체크해줘야됨.
                emoticonAllInfoList = gson.fromJson(emoticonAllInfo, emoAllInfoListType)
            }

            Logger.v("CurrentThread ::${Thread.currentThread().name} ${Looper.getMainLooper() == Looper.myLooper()}")
            emoList.forEach { emoticonSet ->
                if (emoticonSet.isChanged) {
                    // Remove local cache for the updated set
                    emoticonAllInfoList.removeIf { it.emoticonSetId == emoticonSet.id }

                    // Unzip the updated emoticon set
                    try {
                        val manager = ZipManager.getInstance(emoticonAllInfoList)
                        manager?.unzip(
                            context,
                            "$zipFolder${File.separator}${emoticonSet.id}.zip",
                            "${context.filesDir.canonicalPath}${File.separator}unzipped",
                            emoticonSet.id
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 전면 배너 요청
     * @param context Context
     * @param ignoreBanner 배너 무시 여부 (최애 변경시 most_picks만 적용하기 위함)
     */
    fun requestEvent(context: Context, ignoreBanner: Boolean = false) {
        viewModelScope.launch {
            getEventUseCase(
                version = context.getString(R.string.app_version),
                gmail = Util.getGmail(context),
                isVM = VMDetector.getInstance(context).isVM(),
                isRooted = VMDetector.getInstance(context).isRooted,
                deviceId = Util.getDeviceUUID(context)
            ).collect { response ->

                if (!response.optBoolean("success")) {
                    return@collect
                }

                // banner 리스트 받기
                try {
                    val mostPicks = response.optString("most_picks")
                    Util.setPreference(context, Const.PREF_MOST_PICKS, mostPicks)

                    val gson = IdolGson.getInstance()
                    val eventHeartModel =
                        gson.fromJson(response.toString(), EventHeartModel::class.java)

                    val extras = ExtendedDataHolder.getInstance()
                    extras.clear()

                    if (eventHeartModel?.banners?.isNotEmpty() == true) {
                        extras.putExtra("bannerList", eventHeartModel.banners!!)
                    }

                    // 언어에 따른 guide url
                    Util.setPreference(
                        context,
                        Const.PREF_GUIDE,
                        eventHeartModel?.guideUrl,
                    )

                    if (eventHeartModel?.progress == Const.RESPONSE_Y) {
                        Util.setPreference(
                            context,
                            Const.PREF_BURNING_TIME,
                            eventHeartModel.burningTime,
                        )
                    }

                    if(!ignoreBanner) {
                        _eventBanner.value = Event(eventHeartModel)
                    }
                    _showWelcomeMission.value =
                        Event(response.optBoolean("show_welcome_mission"))
                } catch (e: JSONException) {
                    e.printStackTrace()
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setLinkIntent(intent: Intent, isNotExistTab: Boolean = false) {
        this.intentOfLink = intent
        if (isNotExistTab) {
            return
        }
        _intentOfLinkObserve.value = Event(intent)
    }

    @SuppressLint("UnsafeIntentLaunch")
    fun getNextIntent(intent: Intent?): Intent? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(PARAM_NEXT_INTENT, Intent::class.java)
            } else {
                intent?.getParcelableExtra(PARAM_NEXT_INTENT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    // 해당 게시물의 status값(성공여부)를 가져오기위해 호출.
    fun getSupportList(targetId: Int, context: Context) {
        viewModelScope.launch {
            supportRepository.getSupports(
                limit = 100,
                offset = 0,
                listener = { response ->
                    try {

                        if (!response.optBoolean("success")) {
                            val responseMsg = ErrorControl.parseError(context, response)
                            _errorToast.value = Event(responseMsg ?: "")
                        }

                        val array = response.getJSONArray("objects")
                        val gson = IdolGson.getInstance(true)
                        val items = ArrayList<SupportListModel>()

                        var supportModel: SupportListModel? = null

                        if (array.length() != 0) {
                            for (i in 0 until array.length()) {
                                items.add(
                                    gson.fromJson(
                                        array.getJSONObject(i).toString(),
                                        SupportListModel::class.java,
                                    ),
                                )

                                // targetId와 서버에서 불러온 아이디값만 비교해서 같으면 넣어준다.`
                                if (targetId == items[i].id) {
                                    supportModel = items[i]
                                }
                            }
                        }

                        if (supportModel == null) {
                            return@getSupports
                        }

                        _moveToSupportScreen.value = Event(supportModel)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                errorListener = { throwable ->
                    if (Util.is_log()) {
                        _errorToast.value = Event(throwable.message ?: "")
                    }
                },
            )
        }
    }

    fun getSupportInfo(supportListModel: SupportListModel, context: Context): String {
        // 서포트 관련 필요 정보를 서포트 인증샷 화면에 JSON 화 시켜서 넘겨준다.
        val supportInfo = JSONObject()

        try {
            if (supportListModel.idol.getName(context).contains("_")) {
                val nameParts = Util.nameSplit(context, supportListModel.idol)
                supportInfo.apply {
                    put("name", nameParts[0])
                    put("group", nameParts[1])
                }
            } else {
                supportInfo.put("name", supportListModel.idol.getName(context))
            }

            supportInfo.apply {
                put("support_id", supportListModel.id)
                put("title", supportListModel.title)
                put("profile_img_url", supportListModel.image_url)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return supportInfo.toString()
    }

    fun goToDailyPackDetail(context: Context) {
        viewModelScope.launch {
            miscRepository.getStore("P",
                { response ->
                    if (response.optBoolean("success")) {
                        val models = ArrayList<StoreItemModel>()
                        val skus = ArrayList<String>()
                        val gson = IdolGson.getInstance()

                        try {
                            val array = response.getJSONArray("objects")
                            for (i in 0 until array.length()) {
                                val obj = array.getJSONObject(i)
                                val model =
                                    gson.fromJson(obj.toString(), StoreItemModel::class.java)

                                if (model.isViewable.equals("Y", ignoreCase = true) &&
                                    model.subscription.equals("Y", ignoreCase = true) &&
                                    model.type.equals("A", ignoreCase = true)
                                ) {
                                    models.add(model)
                                    skus.add(model.skuCode)
                                }
                            }

                            if (models.isNotEmpty()) {
                                viewModelScope.launch {
                                    usersRepository.getIabKey(
                                        { response ->
                                            Util.closeProgress(200)
                                            if (response.optBoolean("success")) {
                                                val key = response.optString("key")
                                                val iabHelperKey = checkKey(key)

                                                _moveToSubscriptionScreen.value =
                                                    Event(Pair(models[0], iabHelperKey))
                                            }
                                        }, {

                                        }
                                    )
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                },
                { })
        }
    }

    private fun checkKey(key: String): String {
        val key1 = key.substring(key.length - 7, key.length)
        val data = key.substring(0, key.length - 7)
        val pKey = Util.xor(data.toByteArray(), key1.toByteArray())
        return String(pKey)
    }

    fun addTopNavigationTab(button: View) {
        topNavigationTab.add(button)
    }

    fun getTopNavigationTab(): MutableList<View> {
        return topNavigationTab
    }

    fun setCurrentNavigationTabIdx(idx: Int) {
        currentNavigationTabIdx = idx
    }

    fun getCurrentNavigationTabIdx(): Int {
        return currentNavigationTabIdx
    }

    fun clearData() {
        currentNavigationTabIdx = 0
        topNavigationTab.clear()
    }

    fun setSelectedIndex(index: Int) {
        savedStateHandle[BOTTOM_NAV_INDEX_STATE] = index
    }

    fun setIsMaleGender(isMale: Boolean) {
        _isMaleGender.value = Event(isMale)
    }

    fun setChangeHallOfGender(isMale: Boolean) {
        _changeHallOfGender.value = Event(isMale)
    }

    fun setTabWidth(position: Int, width: Int) {
        val currentWidths = tabWidths.value ?: SparseIntArray()
        currentWidths.put(position, width)
        tabWidths.value = currentWidths
    }

    fun getTabWidth(position: Int): Int? {
        return tabWidths.value?.get(position, -1).takeIf { it != -1 }
    }

    fun resetPresentData() {
        currentNavigationTabIdx = 0
        topNavigationTab.clear()
        intentOfLink = null
    }

    fun updateBottomTabIndex(index: Int) {
        _bottomTabIndex.value = index
    }

    fun refreshMyHeartData() {
        _refreshMyHearData.value = true
    }

    fun findFavoriteIdolList(
        mostIdol: IdolModel?,
        favIds: Set<Int>,
        allIdol: List<IdolModel>
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (mainChartModel == null) return@launch

        val maleIterator = mainChartModel!!.males.iterator()
        val femaleIterator = mainChartModel!!.females.iterator()

        // 결합할 리스트
        val combinedList = mutableListOf<ChartCodeInfo>()

        // 아이템을 남성 -> 여성 순으로 하나씩 추가
        while (maleIterator.hasNext() || femaleIterator.hasNext()) {
            if (maleIterator.hasNext()) combinedList.add(maleIterator.next())
            if (femaleIterator.hasNext()) combinedList.add(femaleIterator.next())
        }

        val result = getIdolChartCodesPrefsUseCase()
            .mapDataResource { it }
            .awaitOrThrow()

        val resultList = arrayListOf<RankingModel>()

        result?.forEach { (chartCode, idListInChartCode) ->
            val idSet = idListInChartCode.toSet()

            val filteredIdols: List<IdolModel> = allIdol.filter { idol ->
                idSet.contains(idol.getId().toString())
            }

            val sortedList: List<RankingModel> = filteredIdols
                .sortedByDescending { it.heart } // 하트 높은 순으로 정렬
                .foldIndexed(mutableListOf<RankingModel>()) { index, acc, idol ->

                    if (idol.isViewable == "N") {
                        // 배제된 아이돌은 ranking = -1
                        acc.add(RankingModel(-1, idol))
                    } else {
                        // 이전 유효 랭킹과 유효한 개수 계산
                        val lastRanking = acc.lastOrNull { it.ranking > 0 }?.ranking ?: 0
                        val lastHeart = acc.lastOrNull { it.ranking > 0 }?.idol?.heart ?: -1
                        val validCount = acc.count { it.ranking > 0 } // 유효한 아이템 개수

                        // 랭킹 계산
                        val ranking = if (idol.heart == lastHeart) {
                            lastRanking // 동일 점수는 동일 랭킹
                        } else {
                            validCount + 1 // 유효 아이템 개수 + 1
                        }

                        acc.add(RankingModel(ranking, idol))
                    }
                    acc
                }
                .filter { favIds.contains(it.idol?.getId()) }

            resultList.addAll(sortedList)
        }

        if (mostIdol != null) {
            val idol = resultList.firstOrNull { it.idol?.getId() == mostIdol.getId() }
            if (idol?.idol?.category.equals("B")) {
                idol?.ranking = -1
            }
            _mostIdolData.postValue(idol ?: RankingModel(-1, null))
        } else {
            _mostIdolData.postValue(RankingModel(-1, null))
        }

        getIdolsChartCode(resultList)
    }

    /**
     * 셀럽은 아직 차트코드를 사용하지 않아서 별도 구현
     */
    fun findFavoriteIdolListCeleb(
        context: Context,
        mostIdol: IdolModel?,
        favIdolList: Set<Int>,
        allIdol: List<IdolModel>
    ) = viewModelScope.launch(Dispatchers.IO) {
        Logger.w("===== findFavoriteIdolListCeleb mostIdol=${mostIdol?.getId()}")
//        delay(3000) // https://exodusent.atlassian.net/browse/IDOL-12936 재현 테스트
//        val favIds = favIdolList.map { it.idol.getId() }.toSet()
        // 결합할 리스트
        val combinedList = mutableListOf<ChartCodeInfo>()
        // 셀럽 카테고리별
        val resultList = arrayListOf<RankingModel>()
        val updatedFilteredResult =
            linkedMapOf<String, List<RankingModel>>() // 카테고리별로 List로 묶음

        val typeListModels = Util.setGenderTypeLIst(UtilK.getTypeListArray(context), context)
        typeListModels.forEach { type ->
            val filteredIdols = allIdol.filter { idol ->
                // type.type : AM, AF, SM, SF, E, P, ...
                val typeCategory = idol.type + idol.category
//                Util.log("type.type=[${type.type}], typeCategory=[$typeCategory]")
                typeCategory.startsWith(type.type ?: "")
            }

            val sortedList: List<RankingModel> = filteredIdols
                .sortedWith(compareBy<IdolModel>(
                    { it.isViewable == "N" }, // 제외된 항목 아래로 false -> true 순
                    { -it.heart }, // 하트 내림차순
                    { it.getName(context) }, // 이름 오름차순
                ))
                .foldIndexed(mutableListOf<RankingModel>()) { index, acc, idol ->

                    if (idol.isViewable == "N") {
                        // 배제된 아이돌은 ranking = -1
                        acc.add(RankingModel(-1, idol))
                    } else {
                        // 이전 유효 랭킹과 유효한 개수 계산
                        val lastRanking = acc.lastOrNull { it.ranking > 0 }?.ranking ?: 0
                        val lastHeart = acc.lastOrNull { it.ranking > 0 }?.idol?.heart ?: -1
                        val validCount = acc.count { it.ranking > 0 } // 유효한 아이템 개수

                        // 랭킹 계산
                        val ranking = if (idol.heart == lastHeart) {
                            lastRanking // 동일 점수는 동일 랭킹
                        } else {
                            validCount + 1 // 유효 아이템 개수 + 1
                        }

                        acc.add(RankingModel(ranking, idol))
                    }
                    acc
                }
                .filter { favIdolList.contains(it.idol?.getId()) }

            resultList.addAll(sortedList)
            updatedFilteredResult[type.name] = sortedList
        }

        if (mostIdol != null) {
            val idol = resultList.firstOrNull { it.idol?.getId() == mostIdol.getId() }
            if (idol?.idol?.category.equals("B")) {
                idol?.ranking = -1
            }
            _mostIdolData.postValue(idol ?: RankingModel(-1, null))
        } else {
            _mostIdolData.postValue(RankingModel(-1, null))
        }

        _favoriteData.postValue(updatedFilteredResult)
    }

    private fun getIdolsChartCode(favIdolList: List<RankingModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            if (mainChartModel == null) return@launch

            val result = getIdolChartCodesPrefsUseCase()
                .mapDataResource { it }
                .awaitOrThrow()

            val favIds = favIdolList.map { it.idol?.getId() }.toSet()
            val favIdolMap = favIdolList.associateBy { it.idol?.getId() }

            val filteredResult = result?.mapValues { (_, ids) ->
                ids.mapNotNull { it.toIntOrNull() }
                    .filter { it in favIds }
                    .mapNotNull { favIdolMap[it] }
                    .sortedWith(
                        compareBy(
                            { it.ranking == -1 },         // Move items with ranking == -1 to the end
                            { if (it.ranking == -1) -(it.idol?.heart ?: 0) else 0 }, // For ranking -1, sort by score
                            { it.ranking },               // Sort by ranking
                            { it.idol?.getName() }                   // If ranking is the same, sort by name
                        )
                    )
            }?.filterValues { it.isNotEmpty() }

            val maleIterator = mainChartModel!!.males.iterator()
            val femaleIterator = mainChartModel!!.females.iterator()

            // 결합할 리스트
            val combinedList = mutableListOf<ChartCodeInfo>()

            // 아이템을 남성 -> 여성 순으로 하나씩 추가
            while (maleIterator.hasNext() || femaleIterator.hasNext()) {
                if (maleIterator.hasNext()) combinedList.add(maleIterator.next())
                if (femaleIterator.hasNext()) combinedList.add(femaleIterator.next())
            }

            val codeToFullNameMap = combinedList.associateBy({ it.code }, { it.fullName })

            val updatedFilteredResult =
                linkedMapOf<String, List<RankingModel>>() // List<RankingModel> 타입으로 지정

            codeToFullNameMap.keys.forEach { key ->
                val newKey = codeToFullNameMap[key] ?: key
                val value = filteredResult?.get(key)
                if (value != null) {
                    updatedFilteredResult[newKey] = value
                }
            }

            _favoriteData.postValue(updatedFilteredResult)
        }

    fun updateTutorial(tutorialIndex: Int) = viewModelScope.launch {
        usersRepository.updateTutorial(
            tutorialIndex = tutorialIndex,
            listener = { response ->
                if (response.optBoolean("success")) {
                        Logger.d("Tutorial updated successfully: $tutorialIndex")
                        val bitmask = response.optLong("tutorial", 0L)
                        TutorialManager.init(bitmask)
                } else {
                    _errorToast.value = Event(response.toString())
                }
            },
            errorListener = { throwable ->
                _errorToast.value = Event(throwable.message ?: "Error updating tutorial")
            }
        )
    }

    fun invite() = viewModelScope.launch {
        val languageDeferred = async { languagePreferenceRepository.getSystemLanguage() }
        val tokenDeferred = async { getWebTokenSuspend() }

        val language = languageDeferred.await()
        when (val tokenResult = tokenDeferred.await()) {
            is TokenResult.Success -> {
                val token = tokenResult.token
                _inviteData.value = Event(InvitePayload(language, token))
            }

            is TokenResult.ApiError -> {
                logE("tokenResult.response")
                _errorToastWithJson.postValue(Event(tokenResult.response))
            }

            is TokenResult.NetworkError -> {
                logE(tokenResult.throwable.message ?: "Unknown error")
                _errorToast.postValue(Event(tokenResult.throwable.message ?: "Unknown error"))
            }
        }
    }

    private suspend fun getWebTokenSuspend(): TokenResult =
        suspendCancellableCoroutine { continuation ->
            val job =
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        usersRepository.getWebToken(
                            listener = { response ->
                                if (continuation.isActive) {
                                    val success = response.optBoolean("success", false)
                                    val token =
                                        response.optString("token").takeIf { it.isNotBlank() }
                                    if (success && token != null) {
                                        continuation.resume(TokenResult.Success(token))
                                    } else {
                                        continuation.resume(TokenResult.ApiError(response))
                                    }
                                }
                            },
                            errorListener = { throwable ->
                                if (continuation.isActive) {
                                    continuation.resume(TokenResult.NetworkError(throwable))
                                }
                            }
                        )
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(TokenResult.NetworkError(e))
                        }
                    }
                }

            continuation.invokeOnCancellation {
                job.cancel()
            }
        }

    companion object {
        private const val BOTTOM_NAV_INDEX_STATE = "selectedBottomNavIndex"
    }

    private sealed class TokenResult {
        data class Success(val token: String) : TokenResult()
        data class ApiError(val response: JSONObject) : TokenResult()
        data class NetworkError(val throwable: Throwable) : TokenResult()
    }
}
