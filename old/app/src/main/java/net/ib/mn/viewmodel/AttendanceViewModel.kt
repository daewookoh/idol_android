/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.addon.IdolGson
import net.ib.mn.base.BaseViewModel
import net.ib.mn.model.DailyRewardGuidLineModel
import net.ib.mn.model.DailyRewardModel
import net.ib.mn.core.data.repository.StampsRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.datastore.GetIsEnableVideoAdPrefsUseCase
import net.ib.mn.model.AttendanceSaveStateModel
import net.ib.mn.model.StampRewardModel
import net.ib.mn.model.UserModel
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import net.ib.mn.utils.getKSTMidnightEpochTime
import net.ib.mn.utils.livedata.Event
import net.ib.mn.utils.modelToString
import net.ib.mn.utils.stringToModel
import org.json.JSONException
import org.json.JSONObject

/**
 * @see getAttendanceInfo 출석체크 관련된 정보들을 가져옵니다.
 * @see setStamp 출석체크 도장 찍음.
 * @see postAttendanceInfo 출석 정보 전달.
 * @see getSaveState 출석체크 관련 저장된 정보 가져옴.
 * */

class AttendanceViewModel(
    private val usersRepository: UsersRepository,
    private val stampsRepository: StampsRepository,
    private val getIsEnableVideoAdPrefsUseCase: GetIsEnableVideoAdPrefsUseCase,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val _getAttendanceInfoSuccess = MutableLiveData<Event<AttendanceSaveStateModel>>()
    val getAttendanceInfoSuccess: LiveData<Event<AttendanceSaveStateModel>> = _getAttendanceInfoSuccess

    private val _setStampeSuccess = MutableLiveData<Event<AttendanceSaveStateModel>>()
    val setStampeSuccess: LiveData<Event<AttendanceSaveStateModel>> = _setStampeSuccess

    private val _showRewardAd = MutableLiveData<Event<Boolean>>()
    val showRewardAd : LiveData<Event<Boolean>> = _showRewardAd

    private val _bottomSheetDismiss = MutableLiveData<Event<Boolean>>()
    val bottomSheetDismiss : LiveData<Event<Boolean>> = _bottomSheetDismiss

    private val _getDailyRewards = MutableLiveData<Event<List<DailyRewardModel>>>()
    val getDailyRewards: LiveData<Event<List<DailyRewardModel>>> = _getDailyRewards

    private val _postDailyRewards = MutableLiveData<Event<DailyRewardModel>>()
    val postDailyRewards: LiveData<Event<DailyRewardModel>> = _postDailyRewards

    private val _moveScreenToVideo = MutableLiveData<Event<Boolean>>()
    val moveScreenToVideo: LiveData<Event<Boolean>> = _moveScreenToVideo

    private val _moveToLink = MutableLiveData<Event<String>>()
    val moveToLink: LiveData<Event<String>> = _moveToLink

    //유저정보.
    private lateinit var userModel: UserModel

    //이계정의 연속 접속일.
    private var continueDays: Int = 0

    //현재 유저가 가지고있는 Stamp정보.
    private lateinit var stamp: JSONObject

    //보상 하트 , 다이아
    private var rewardArrayList = arrayListOf<Map<String, StampRewardModel>>()

    var attendanceSaveStateModel: AttendanceSaveStateModel? = null
    private var dailyRewardList = arrayListOf<DailyRewardModel>()
    private var guideLine: DailyRewardGuidLineModel? = null

    init {
        //시스템에서 데이터 날아가는경우를 대비해서 값을 가져온다.
        getSaveState()

    }


    //TODO:: context 같은경우 공통적으로 사용하기때문에 DI로 빼서 사용해야됨.
    fun getAttendanceInfo(context: Context) {
        viewModelScope.launch {
            stampsRepository.getStampsCurrent(
                { response ->
                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(context, response) ?: return@getStampsCurrent
                        _errorToast.postValue(Event(responseMsg))
                        return@getStampsCurrent
                    }

                    try {
                        val gson = IdolGson.getInstance()
                        val user = response.getJSONObject("user")
                        val gcode = response.optInt("gcode")
                        val msg = response.optString("msg")

                        //유저정보.
                        userModel = gson.fromJson(user.toString(), UserModel::class.java)

                        //계정 연속 접속일.
                        continueDays = response.optInt("continue_days")
                        //스탬프 리스폰스.
                        stamp = response.getJSONObject("stamp")

                        //연속 출석 보상값 가져오기.
                        val rewards = response.getJSONObject("rewards")

                        val rewardsIterator = rewards.keys()
                        rewardArrayList.clear()

                        while (rewardsIterator.hasNext()) {
                            val key = rewardsIterator.next()

                            val stampRewardModel = gson.fromJson(
                                rewards.getJSONObject(key).toString(),
                                StampRewardModel::class.java
                            )

                            rewardArrayList.add(mapOf(key to stampRewardModel))
                        }

                        //데이터 날라감 현상으로인해 저장해줍니다.
                        setSaveState(stamp, rewardArrayList, userModel, continueDays, msg, gcode)
                        postAttendanceInfo()

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                { throwable ->
                    _errorToast.postValue(Event(throwable.message ?: ""))
                }
            )
        }
    }

    fun setStamp(context: Context) {
        viewModelScope.launch {
            stampsRepository.postStamp(
                { response ->
                    val gcode = response.optInt("gcode")
                    val msg = response.optString("msg")

                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(context, response) ?: return@postStamp
                        _errorToast.postValue(Event(responseMsg))
                        postSetStampSuccess(isSuccess = false)
                        return@postStamp
                    }

                    stamp = response.getJSONObject("stamp")

                    setSaveState(stamp, rewardArrayList, userModel, continueDays, msg, gcode)
                    // 출석체크 상태 갱신을 위해 호출 (없으면 홈화면 나갔다 다시 들어오면 출첵 버튼 부활됨)
                    postAttendanceInfo()
                    //스탬프 정보 가져옴 성공 라이브데이터 전달.
                    postSetStampSuccess(isSuccess = true)
                }, { throwable ->
                    _errorToast.postValue(Event(throwable.message ?: ""))
                    postSetStampSuccess(isSuccess = false)
                }
            )
        }
    }

    private fun postAttendanceInfo() {
        //스탬프 정보 가져옴 성공 라이브데이터 전달.
        _getAttendanceInfoSuccess.postValue(
            Event(
                attendanceSaveStateModel ?: return
            )
        )
    }

    private fun postSetStampSuccess(isSuccess: Boolean) {
        _setStampeSuccess.postValue(
            Event(
                attendanceSaveStateModel?.apply { successOfSetStamp = isSuccess } ?: return
            )
        )
    }


    private fun setSaveState(
        stamp: JSONObject,
        rewardArrayList: ArrayList<Map<String, StampRewardModel>>,
        userModel: UserModel,
        continueDays: Int,
        msg: String,
        gcode: Int
    ) {
        attendanceSaveStateModel = AttendanceSaveStateModel(
            stamp = stamp,
            rewards = rewardArrayList,
            user = userModel,
            continueDays = continueDays,
            msg = msg,
            gcode = gcode
        )

        savedStateHandle[SAVE_STATE_ATTENDANCE] = attendanceSaveStateModel?.modelToString()
    }

    private fun getSaveState() {

        savedStateHandle.apply {
            get<String>(SAVE_STATE_ATTENDANCE)?.run {
                this.stringToModel<AttendanceSaveStateModel>()?.let {
                    attendanceSaveStateModel = it
                    postAttendanceInfo()
                }
            }
        }
    }

    fun getDailyRewards(context: Context) {
        viewModelScope.launch {
            usersRepository.getDailyRewards(
                listener = listener@ { response ->
                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(context, response) ?: return@listener
                        _errorToast.postValue(Event(responseMsg))
                        return@listener
                    }

                    val gson = IdolGson.getInstance()

                    guideLine = gson.fromJson(
                        response.optString("guideline").toString(),
                        DailyRewardGuidLineModel::class.java,
                    )

                    val listType = object : TypeToken<List<DailyRewardModel>>() {}.type
                    dailyRewardList =
                        gson.fromJson(response.optJSONArray("objects")?.toString(), listType)

                    with(dailyRewardList) {
                        //출석 헤더위치 dummy값
                        add(0, DailyRewardModel(key = "attendance_graph"))
                        //유의사항 dummy값
                        add(
                            DailyRewardModel(
                                key = "attendance_warning",
                                title = guideLine?.title,
                                desc = guideLine?.desc,
                            ),
                        )
                    }

                    _getDailyRewards.postValue(Event(dailyRewardList.distinctBy { it.key }))
                }, { throwable ->
                    _errorToast.postValue(Event(throwable.message ?: ""))
                }
            )
        }
    }

    fun postDailyRewards(context: Context, key: String, link: String? = null) {
        Logger.v("DailyRewards::key ${key}")
        viewModelScope.launch {
            usersRepository.postDailyReward(
                key,
                { response ->
                    Logger.v("DailyRewards::response${response.modelToString()}")

                    if (!response.optBoolean("success")) {
                        Util.closeProgress() // 88888로 오는 경우 대비
                        val responseMsg = ErrorControl.parseError(context, response) ?: return@postDailyReward
                        _errorToast.postValue(Event(responseMsg))
                        _getDailyRewards.postValue(Event(dailyRewardList.distinctBy { it.key }))
                        return@postDailyReward
                    }

                    val gson = IdolGson.getInstance()
                    val rewardedModel = gson.fromJson(
                        response.optJSONObject("object")?.toString() ?: return@postDailyReward,
                        DailyRewardModel::class.java
                    )

                    val indexToChange = dailyRewardList.indexOfFirst { item ->
                        item.key == rewardedModel.key
                    }

                    dailyRewardList[indexToChange] = rewardedModel

                    _getDailyRewards.postValue(Event(dailyRewardList.distinctBy { it.key }))
                    _postDailyRewards.postValue(Event(rewardedModel))

                    link?.let {
                        _moveToLink.postValue(Event(link))
                    }
                }, { throwable ->
                    _errorToast.postValue(Event(throwable.message ?: ""))
                    _getDailyRewards.postValue(Event(dailyRewardList.distinctBy { it.key }))
                }
            )
        }
    }

    fun moveScreenToVideo() = viewModelScope.launch(Dispatchers.IO){
        val isEnabled = getIsEnableVideoAdPrefsUseCase()
            .mapDataResource { it }
            .awaitOrThrow()
        _moveScreenToVideo.postValue(Event(isEnabled ?: true))
    }

    companion object {
        const val SAVE_STATE_ATTENDANCE = "save_state_attendance"
    }
}

class AttendanceViewModelFactory(
    private val context: Context,
    private val usersRepository: UsersRepository,
    private val stampsRepository: StampsRepository,
    private val getIsEnableVideoAdPrefsUseCase: GetIsEnableVideoAdPrefsUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            return AttendanceViewModel(usersRepository, stampsRepository, getIsEnableVideoAdPrefsUseCase, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}