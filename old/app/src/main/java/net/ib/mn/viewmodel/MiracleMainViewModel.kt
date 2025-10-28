package net.ib.mn.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.domain.usecase.GetChartsUseCase
import net.ib.mn.core.model.ChartModel
import net.ib.mn.utils.livedata.Event
import javax.inject.Inject

@HiltViewModel
class MiracleMainViewModel @Inject constructor(
    private val getChartsUseCase: GetChartsUseCase,
) : BaseViewModel() {

    private val _miracleChartModel = MutableLiveData<Event<List<ChartModel>>>()
    val miracleChartModel: LiveData<Event<List<ChartModel>>> = _miracleChartModel

    init {
        if (BuildConfig.CELEB) getLiveChart()
    }

    private fun getLiveChart() = viewModelScope.launch {
        getChartsUseCase().collect { response ->
            // response.message가 null이 아니면 에러 메시지를 처리
            response.message?.let {
                _errorToast.postValue(Event(it))
                return@collect
            }

            val chartList = response.objects ?: return@collect
            val miracleList = chartList.filter { it.type == MIRACLE_TYPE }
            _miracleChartModel.postValue(Event(miracleList))
        }
    }

    companion object {
        private const val MIRACLE_TYPE = "M"
    }
}
