package net.ib.mn.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.model.AdCachingData
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.datastore.GetIsEnableVideoAdPrefsUseCase
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.DiamondLogModel
import net.ib.mn.model.HeartLogModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Util
import net.ib.mn.utils.livedata.Event
import org.json.JSONArray
import org.json.JSONObject

class MyHeartInfoViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val usersRepository: UsersRepository,
    private val accountManager: IdolAccountManager,
    private val getIsEnableVideoAdPrefsUseCase: GetIsEnableVideoAdPrefsUseCase
) : ViewModel() {

    private val _resultModelForMyHeartInfoFragment = MutableLiveData<Event<Map<String, Any>>>()
    val resultModelForMyHeartInfoFragment: MutableLiveData<Event<Map<String, Any>>> get() = _resultModelForMyHeartInfoFragment

    private val _resultModelForMyHeartHistory = MutableLiveData<Event<Map<String, Any>>>()
    val resultModelForMyHeartHistory: MutableLiveData<Event<Map<String, Any>>> get() = _resultModelForMyHeartHistory

    private val _resultDataExistCheck = MutableLiveData<Event<Boolean>>()
    val resultDataExistCheck: MutableLiveData<Event<Boolean>> get() = _resultDataExistCheck

    private val _errorPopup = MutableLiveData<Event<String>>()
    val errorPopup: LiveData<Event<String>> = _errorPopup

    private val _tutorial = MutableLiveData<Event<Int>>()
    val tutorial: LiveData<Event<Int>> = _tutorial

    private val _updateVideoRedDot = MutableLiveData<Event<Boolean>>()
    val updateVideoRedDot: LiveData<Event<Boolean>> = _updateVideoRedDot

    private val _updateVideoText = MutableLiveData<Event<String>>()
    val updateVideoText: LiveData<Event<String>> = _updateVideoText

    private val _updateVideoBtnEnabled = MutableLiveData<Event<Boolean>>()
    val updateVideoBtnEnabled: LiveData<Event<Boolean>> = _updateVideoBtnEnabled

    private val _moveScreenToStore = MutableLiveData<Event<Boolean>>()
    val moveScreenToStore: LiveData<Event<Boolean>> = _moveScreenToStore

    private val _moveScreenToVideo = MutableLiveData<Event<Boolean>>()
    val moveScreenToVideo: LiveData<Event<Boolean>> = _moveScreenToVideo

    private val _moveScreenToFreeCharge = MutableLiveData<Event<Boolean>>()
    val moveScreenToFreeCharge: LiveData<Event<Boolean>> = _moveScreenToFreeCharge

    // first = 에버 하트 second = 데일리 하트
    private val _todayEarnHeart = MutableLiveData<Event<Pair<Long, Long>>>()
    val todayEarnHeart: LiveData<Event<Pair<Long, Long>>> = _todayEarnHeart

    private val _currentAccount = MutableLiveData<IdolAccount>()
    val currentAccount: LiveData<IdolAccount> = _currentAccount

    private var missionHeart = 0L//미션 하트
    private var todayEarnEverHeart = 0L//오늘 얻은  에버 허트
    private var todayDailyHeart = 0L // 오늘 얻은 데일리하트

    private var account: IdolAccount? = null
    private var adCacheData: AdCachingData? = null
    private var maxAdCount: Int = 150

    //하트 , 다이아 사용 내역.
    private val diamondSpend = arrayListOf<DiamondLogModel>()
    private val diamondEarn = arrayListOf<DiamondLogModel>()
    private val heartSpend = arrayListOf<HeartLogModel>()
    private val heartEarn = arrayListOf<HeartLogModel>()

    init {
//        setMaxAdCount()
//        getAdCachingData()
    }

    private fun setMaxAdCount(context: Context) {
        val account = IdolAccount.getAccount(context)
        account?.let {
            maxAdCount = if (it.heart == Const.LEVEL_ADMIN || BuildConfig.DEBUG) 4 else 150
        }
    }

    fun getHeartData(context: Context) {

        getSaveState(context)

        // fetchUserInfo에서 하던거
        val currentAccount = IdolAccount.getAccount(context)
        account = currentAccount

        //쿠폰 업데이트 안되는 것때문에  부름.
        accountManager.fetchUserInfo(context, {
            //account가 세팅되고 나서 api부르기.
            currentAccount?.saveAccount(context)
            currentAccount?.let {
                _currentAccount.postValue(it)
            }
            getHeartDataList(context)
        })
    }

    private fun getHeartDataList(context: Context) {
        viewModelScope.launch {
            usersRepository.getHeartDiamondLog(
                { response ->
                    if (!response.optBoolean("success")) {
                        resultDataExistCheck.postValue(Event(false))
                        val responseMsg = ErrorControl.parseError(context, response) ?: return@getHeartDiamondLog
                        _errorPopup.postValue(Event(responseMsg))
                        return@getHeartDiamondLog
                    }

                    // ConfigModel의 nasHeart, videoHeart 업데이트
                    if (response.has("nas_heart"))
                        ConfigModel.getInstance(context).nasHeart =
                            response.optInt("nas_heart")
                    if (response.has("video_heart"))
                        ConfigModel.getInstance(context).video_heart =
                            response.optInt("video_heart")

                    setMyHeartInfo(response, context)
                    setMyHeartHistory(response)

                    postMyHeartInfo()
                    postMyHeartHistory(response)

                    setSaveState()

                    resultDataExistCheck.postValue(Event(true))
                },
                { throwable ->
                    resultDataExistCheck.postValue(Event(false))
                    _errorPopup.postValue(Event(throwable.message ?: ""))
                }
            )
        }
    }

    private fun setMyHeartInfo(response: JSONObject, context: Context) {
        todayEarnEverHeart = response.optLong("today_earn")

        // heart mission
        if (response.optInt("mission_heart") > 0) {
            missionHeart = response.optLong("mission_heart")

            // 미션 달성 여부
            Util.setPreference(
                context,
                Const.PREF_MISSION_COMPLETED,
                todayEarnEverHeart >= missionHeart
            )
        } else {
            Util.setPreference(
                context,
                Const.PREF_MISSION_COMPLETED,
                false
            )
        }

        _todayEarnHeart.postValue(Event(Pair(todayEarnEverHeart, response.optLong("today_earn_daily"))))

        account?.setUserHearts(response)
        account?.saveAccount(context)//account 값 refresh
    }

    //하트 , 다이아 사용내역 세팅 (리스트 형태로 저장).
    private fun setMyHeartHistory(response: JSONObject) {

        val gson = IdolGson.getInstance()
        val diamondSpendArray = response.getJSONArray("diamond_spend")
        val diamondEarnArray = response.getJSONArray("diamond_earn")
        val heartSpendArray = response.getJSONArray("spend")
        val heartEarnArray = response.getJSONArray("earn")

        diamondSpend.clear()
        diamondEarn.clear()
        heartSpend.clear()
        heartEarn.clear()

        val diamondListType = object : TypeToken<ArrayList<DiamondLogModel>>() {}.type
        val heartListType = object : TypeToken<ArrayList<HeartLogModel>>() {}.type

        val diamondSpendList = gson.fromJson<ArrayList<DiamondLogModel>>(
            diamondSpendArray.toString(),
            diamondListType
        )
        diamondSpend.addAll(diamondSpendList)

        val diamondEarnList = gson.fromJson<ArrayList<DiamondLogModel>>(
            diamondEarnArray.toString(),
            diamondListType
        )
        diamondEarn.addAll(diamondEarnList)

        val heartSpendList = gson.fromJson<ArrayList<HeartLogModel>>(
            heartSpendArray.toString(),
            heartListType
        )
        heartSpend.addAll(heartSpendList)

        val heartEarnList = gson.fromJson<ArrayList<HeartLogModel>>(
            heartEarnArray.toString(),
            heartListType
        )
        heartEarn.addAll(heartEarnList)
    }

    //하트 , 다이아 사용내역 Json형태로 가지고옴.
    fun getMyHeartHistory(): JSONObject {
        val gson = Gson()
        val diamondListType = object : TypeToken<ArrayList<DiamondLogModel>>() {}.type
        val heartListtype = object : TypeToken<ArrayList<HeartLogModel>>() {}.type

        val diamondSpendString = gson.toJson(diamondSpend, diamondListType)
        val diamondEarnString = gson.toJson(diamondEarn, diamondListType)

        val heartSpendString = gson.toJson(heartSpend, heartListtype)
        val heartEarnString = gson.toJson(heartEarn, heartListtype)

        val response = JSONObject()
        response.put("diamond_spend", JSONArray(diamondSpendString))
        response.put("diamond_earn", JSONArray(diamondEarnString))
        response.put("spend", JSONArray(heartSpendString))
        response.put("earn", JSONArray(heartEarnString))

        return response
    }

    //나의 정보 전달.
    private fun postMyHeartInfo() {
        val account = account ?: return
        _resultModelForMyHeartInfoFragment.postValue(
            Event(
                mapOf(
                    "account" to account,
                    "missionHeart" to missionHeart,
                    "todayEarnEverHeart" to todayEarnEverHeart
                )
            )
        )
    }

    //하트, 다이아몬드 지출 내역 전달.
    fun postMyHeartHistory(response: JSONObject) {
        _resultModelForMyHeartHistory.postValue(
            Event(
                mapOf(
                    "diamond_spend" to response.getJSONArray("diamond_spend"),
                    "diamond_earn" to response.getJSONArray("diamond_earn"),
                    "spend" to response.getJSONArray("spend"),
                    "earn" to response.getJSONArray("earn")
                )
            )
        )
    }

    fun getAccount() = account

    fun getMissionHeart() = missionHeart

    //데이터 값 저장.
    private fun setSaveState() {

        //나의 정보 저장.
        savedStateHandle.set("mission_heart", missionHeart)
        savedStateHandle.set("today_earn", todayEarnEverHeart)

        //하트 다이아 지출 내역 저장.
        savedStateHandle.set("diamond_spend", diamondSpend)
        savedStateHandle.set("diamond_earn", diamondEarn)
        savedStateHandle.set("spend", heartSpend)
        savedStateHandle.set("earn", heartEarn)
    }

    private fun getSaveState(context: Context) {

        with(savedStateHandle) {

            get<Long>("mission_heart")?.run {
                missionHeart = this
            }

            get<Long>("today_earn")?.run {
                todayEarnEverHeart = this
            }

            account = IdolAccount.getAccount(context)

            get<ArrayList<DiamondLogModel>>("diamond_spend")?.run {
                diamondSpend.clear()
                diamondSpend.addAll(this)
            }

            get<ArrayList<DiamondLogModel>>("diamond_earn")?.run {
                diamondEarn.clear()
                diamondEarn.addAll(this)
            }

            get<ArrayList<HeartLogModel>>("spend")?.run {
                heartSpend.clear()
                heartSpend.addAll(this)
            }

            get<ArrayList<HeartLogModel>>("earn")?.run {
                heartEarn.clear()
                heartEarn.addAll(this)

                postMyHeartInfo()
                postMyHeartHistory(getMyHeartHistory())
            }
        }
    }

    fun setTutorial(value: Int) {
        _tutorial.value = Event(value)
    }

    fun updateVideoRedDot() {
        _updateVideoRedDot.value = Event(true)
    }

    fun updateVideoText(text : String) {
        _updateVideoText.value = Event(text)
    }

    fun updateVideoBtnEnabled(isEnabled: Boolean) {
        _updateVideoBtnEnabled.value = Event(isEnabled)
    }

    fun moveScreenToStore() {
        _moveScreenToStore.value = Event(true)
    }

    fun moveScreenToVideo() = viewModelScope.launch(Dispatchers.IO){
        val isEnabled = getIsEnableVideoAdPrefsUseCase()
            .mapDataResource { it }
            .awaitOrThrow()
        _moveScreenToVideo.postValue(Event(isEnabled ?: true))
    }

    fun moveScreenToFreeCharge() {
        _moveScreenToFreeCharge.value = Event(true)
    }
}

class MyHeartInfoViewModelFactory(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val usersRepository: UsersRepository,
    private val accountManager: IdolAccountManager,
    private val getIsEnableVideoAdPrefsUseCase: GetIsEnableVideoAdPrefsUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyHeartInfoViewModel::class.java)) {
            return MyHeartInfoViewModel(savedStateHandle, usersRepository, accountManager, getIsEnableVideoAdPrefsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}