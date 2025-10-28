package net.ib.mn.feature.friend

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.PresignedUrlService.Companion.CALL_CHECK_READY_DELAY
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.data.model.RewardModel
import net.ib.mn.core.domain.usecase.RecommendRewardUseCase
import net.ib.mn.domain.usecase.GetIdolsByTypeAndCategoryUseCase
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.livedata.Event
import javax.inject.Inject

@HiltViewModel
class FriendInviteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getIdolsByTypeAndCategoryUseCase: GetIdolsByTypeAndCategoryUseCase,
    private val recommendRewardUseCase: RecommendRewardUseCase
) : BaseViewModel() {

    private val _inviteMsg = MutableLiveData<Event<String>>()
    val inviteMsg: LiveData<Event<String>> = _inviteMsg
    private val _reward = MutableLiveData<Event<Pair<Int, Int>>>()
    val reward: LiveData<Event<Pair<Int, Int>>> = _reward

    init {
        getReward()
    }

    fun getInviteMsg() = viewModelScope.launch {
        _inviteMsg.value =
            Event(UtilK.getFriendInviteMsg(context, getIdolsByTypeAndCategoryUseCase))
    }

    fun getReward() = viewModelScope.launch {
        try {
            recommendRewardUseCase().collect { response ->
                if (response.gcode == 8100) {
                    Log.d("@@@@", "RecommendRewardResponse: $response")
                    return@collect
                }
                if (!response.success) {
                    response.msg?.let {
                        _errorToast.postValue(Event(it))
                    }
                    return@collect
                }

                _reward.postValue(Event(Pair(response.missionReward?.heart ?: 0, response.allClearReward?.heart ?: 0)))
            }
        } catch (e: Exception) {
            _errorToast.postValue(Event(e.message ?: ""))
        }
    }
}