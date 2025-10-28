package net.ib.mn.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.addon.IdolGson
import net.ib.mn.base.BaseAndroidViewModel
import net.ib.mn.core.data.repository.HeartpickRepositoryImpl
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.model.HeartPickVoteRewardModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.livedata.Event
import javax.inject.Inject

@HiltViewModel
class HeartPickVoteDialogViewModel @Inject constructor(
    application: Application,
    private val heartpickRepository: HeartpickRepositoryImpl,
    private val savedStateHandle: SavedStateHandle
) : BaseAndroidViewModel(application) {

    private var voteJob: Job? = null

    private val _voteHeart = MutableLiveData<Event<Map<String, Any?>>>()
    val voteHeart: LiveData<Event<Map<String, Any?>>> = _voteHeart

    fun voteHeartPick(
        lifecycleScope: LifecycleCoroutineScope,
        context: Context?,
        heartPickId: Int,
        heartPickIdol: HeartPickIdol?,
        number: Int
    ) {
        if (voteJob?.isActive == true) return

        Util.showProgress(context)

        voteJob = viewModelScope.launch(Dispatchers.IO) {
            heartpickRepository.vote(
                id = heartPickId,
                idolId = heartPickIdol?.id ?: 0,
                num = number.toLong(),
                { response ->
                    val reward = IdolGson.getInstance()
                        .fromJson(response.toString(), HeartPickVoteRewardModel::class.java)
                    lifecycleScope.launch(Dispatchers.Main) {
                        delay(2000L)
                        _voteHeart.value =
                            Event(mapOf("heartPickIdol" to heartPickIdol, "reward" to reward))
                    }
                }, { throwable ->
                    val msg = throwable.message
                    _errorToast.value = Event(msg ?: return@vote)
                }
            )
        }.apply {
            invokeOnCompletion {
                voteJob = null
            }
        }
    }
}