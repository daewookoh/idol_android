package net.ib.mn.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.data_resource.DataResource
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.DeleteAllIdolUseCase
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.domain.usecase.datastore.InitAdDataPrefsUseCase
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.getKSTMidnightEpochTime
import net.ib.mn.utils.livedata.Event
import javax.inject.Inject

@HiltViewModel
class MainRankingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deleteAllIdolUseCase: DeleteAllIdolUseCase,
    private val getIdolByIdUseCase: GetIdolByIdUseCase,
    private val initAdDataPrefsUseCase: InitAdDataPrefsUseCase
) : ViewModel() {

    private val _idol = MutableLiveData<Event<IdolModel>>()
    val idol: LiveData<Event<IdolModel>> get() = _idol

    init {
        initAdData()
    }

    private fun initAdData() = viewModelScope.launch {
        val todayEpochTime = getKSTMidnightEpochTime()
        initAdDataPrefsUseCase(todayEpochTime).collectLatest { result ->
            when (result) {
                is DataResource.Success -> {
                    // no-op
                }
                is DataResource.Error -> {
                    // no-op
                }
                is DataResource.Loading -> {
                    // no-op
                }
            }
        }
    }

    fun clearDatabase() = viewModelScope.launch(Dispatchers.IO) {
        deleteAllIdolUseCase().first()
    }

    fun getIdolById(idolId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val idol = getIdolByIdUseCase(idolId)
            .mapDataResource { it?.toPresentation() }
            .awaitOrThrow()
        idol?.let {
            withContext(Dispatchers.Main) {
                _idol.postValue(Event(it))
            }
        }
    }
}