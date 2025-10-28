package net.ib.mn.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.HeartpickRepositoryImpl
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.model.HeartPickModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.Toast
import net.ib.mn.utils.livedata.Event
import javax.inject.Inject

@HiltViewModel
class HeartPickPrelaunchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val heartPickRepository: HeartpickRepositoryImpl,
    private val getIdolByIdUseCase: GetIdolByIdUseCase,
) : ViewModel() {

    private val _heartPick = MutableStateFlow<HeartPickModel?>(null)
    val heartPick: StateFlow<HeartPickModel?> = _heartPick.asStateFlow()
    private val _idol = MutableStateFlow<Event<IdolModel>?>(null)
    val idol: StateFlow<Event<IdolModel>?> = _idol.asStateFlow()
    private val _mostId = MutableStateFlow<Int?>(null)
    val mostId: StateFlow<Int?> = _mostId.asStateFlow()
    private val _isNotify = MutableStateFlow(false)
    val isNotify: StateFlow<Boolean> = _isNotify.asStateFlow()
    private val _isNotifyToast = MutableLiveData<Event<Boolean>>()
    val isNotifyToast: MutableLiveData<Event<Boolean>> = _isNotifyToast

    fun setHeartPick(heartPick: HeartPickModel) {
        _heartPick.value = heartPick
    }

    fun updateMostId(context: Context) {
        _mostId.value = IdolAccount.getAccount(context)?.most?.getId()
    }

    fun getHeartPick(heartPickId: Int) {
        if (heartPick.value != null) return

        viewModelScope.launch {
            heartPickRepository.get(
                heartPickId,
                0,
                1,
                { response ->
                    val gson = IdolGson.getInstance(true)

                    if (heartPickId != 0) {
                        val heartPickModel = gson.fromJson(
                            response.optJSONObject("object")?.toString() ?: "",
                            HeartPickModel::class.java
                        )

                        heartPickModel.heartPickIdols?.shuffle()

                        viewModelScope.launch(Dispatchers.Main) {
                            _heartPick.value = heartPickModel
                        }
                    }
                }, { throwable ->
                    viewModelScope.launch(Dispatchers.Main) {
                        val msg = throwable.message
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    fun getIdolById(idolId: Int) = viewModelScope.launch() {
        val idol = getIdolByIdUseCase(idolId)
            .mapDataResource { it?.toPresentation() }
            .awaitOrThrow()
        idol?.let {
            withContext(Dispatchers.Main) {
                _idol.value = Event(it)
            }
        }
    }

    fun updateCommentCount(count: Int) {
        val currentHeartPick = _heartPick.value
        if (currentHeartPick != null) {
            _heartPick.value = currentHeartPick.copy(numComments = count)
        }
    }

    fun getHeartPickSettingNotification(heartPickId: Int) {
        viewModelScope.launch {
            heartPickRepository.getOpenHeartPickNotification(
                id = heartPickId,
                listener = { response ->
                    val gson = IdolGson.getInstance(true)
                    val jsonArray = response.optJSONArray("objects") ?: listOf<Int>()
                    val ids: List<Int>? =
                        gson.fromJson(jsonArray.toString(), object : TypeToken<List<Int>>() {}.type)

                    _isNotify.value = ids?.contains(heartPickId) ?: false
                },
                errorListener = { throwable ->
                    viewModelScope.launch(Dispatchers.Main) {
                        val msg = throwable.message
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    fun postHeartPickSettingNotification(heartPickId: Int) {
        viewModelScope.launch {
            heartPickRepository.postOpenHeartPickNotification(
                id = heartPickId,
                listener = { _ ->
                    _isNotifyToast.value = Event(true)
                    _isNotify.value = true
                },
                errorListener = { throwable ->
                    viewModelScope.launch(Dispatchers.Main) {
                        val msg = throwable.message
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}