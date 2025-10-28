package net.ib.mn.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.datastore.GetIsEnableVideoAdPrefsUseCase
import net.ib.mn.utils.livedata.Event
import javax.inject.Inject

@HiltViewModel
class HeartPlusViewModel @Inject constructor (
    private val getIsEnableVideoAdPrefsUseCase: GetIsEnableVideoAdPrefsUseCase
): ViewModel() {

    private val _moveScreenToVideo = MutableLiveData<Event<Boolean>>()
    val moveScreenToVideo: LiveData<Event<Boolean>> = _moveScreenToVideo

    fun moveScreenToVideo() = viewModelScope.launch(Dispatchers.IO){
        val isEnabled = getIsEnableVideoAdPrefsUseCase()
            .mapDataResource { it }
            .awaitOrThrow()
        _moveScreenToVideo.postValue(Event(isEnabled ?: true))
    }
}