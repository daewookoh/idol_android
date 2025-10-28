package net.ib.mn.feature.mission

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.MissionsRepository
import net.ib.mn.model.WelcomeMissionModel
import net.ib.mn.model.WelcomeMissionResultModel
import net.ib.mn.utils.livedata.Event
import org.json.JSONException
import org.json.JSONObject

class MissionViewModel(
    private val missionsRepository: MissionsRepository,
) : ViewModel() {

    private val _missionUiState: MutableStateFlow<MissionUiState> = MutableStateFlow(MissionUiState.Loading)
    val missionUiState: StateFlow<MissionUiState> = _missionUiState

    private val _rewardDialog: MutableLiveData<Event<Int>> = MutableLiveData()
    val rewardDialog: LiveData<Event<Int>> get() = _rewardDialog

    private var isAllClear = false

    fun getWelcomeMission() {
        viewModelScope.launch {
            missionsRepository.getWelcomeMission(
                { response ->
                    handlingWelcomeMissionResult(response)
                }, { throwable ->
                    _missionUiState.value = MissionUiState.ERROR(throwable.message ?: "")
                }
            )
        }
    }

    private fun handlingWelcomeMissionResult(response: JSONObject) {
        if (response.optBoolean("success")) {
            try {
                val gson = IdolGson.getInstance(false)
                val objectType = object : TypeToken<WelcomeMissionResultModel>() {}.type
                val jsonObject = response.getJSONObject("object")
                val data: WelcomeMissionResultModel = gson.fromJson(jsonObject.toString(), objectType)

                for (model in data.list) {
                    val matchingMission = MissionItemInfo.entries.find { it.key == model.key }
                    if (matchingMission != null) {
                        model.desc = matchingMission
                    } else {
                        model.desc = MissionItemInfo.WELCOME_ADD_FRIEND
                    }
                }

                _missionUiState.value = MissionUiState.Success(data)
            } catch (e: JSONException) {
                _missionUiState.value = MissionUiState.ERROR(e.message ?: "")
            }
        } else {
            _missionUiState.value = MissionUiState.ERROR("")
        }
    }

    fun requestGetReward(key: String) {
        viewModelScope.launch {
            missionsRepository.claimMissionReward(
                key,
                { response ->
                    handlingRewardResult(key, response)
                }, { throwable ->
                    _missionUiState.value = MissionUiState.ERROR(throwable.message ?: "")
                }
            )
        }
    }

    private fun handlingRewardResult(key: String, response: JSONObject) {
        if (response.optBoolean("success")) {
            try {
                val gson = IdolGson.getInstance(false)
                val objectType = object : TypeToken<WelcomeMissionModel>() {}.type
                val jsonObject = response.getJSONObject("object")
                val data: WelcomeMissionModel = gson.fromJson(jsonObject.toString(), objectType)

                _rewardDialog.postValue(Event(data.amount))

                // 메인 화면 나갔을 때 버튼 VISIBLE 처리
                val isAllClearResult = key == MissionItemInfo.WELCOME_ALL_CLEAR.key
                if (isAllClearResult) {
                    isAllClear = true
                }

                // 친구 신청 때문에 실시간 성을 보장할 수 없어 그냥 데이터 한번 더 부름
                getWelcomeMission()
            } catch (e: JSONException) {
                _missionUiState.value = MissionUiState.ERROR(e.message ?: "")
            }
        } else {
            _missionUiState.value = MissionUiState.ERROR("")
        }
    }

    fun getIsAllClear() = isAllClear
}

class MissionViewModelFactory(
    private val missionsRepository: MissionsRepository,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MissionViewModel::class.java)) {
            return MissionViewModel(missionsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}