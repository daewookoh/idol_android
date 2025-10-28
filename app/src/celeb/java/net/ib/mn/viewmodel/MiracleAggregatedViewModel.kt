package net.ib.mn.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.core.domain.usecase.GetChartRanksUseCase
import net.ib.mn.utils.livedata.Event
import javax.inject.Inject

class MiracleAggregatedViewModel(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val chartCode: String
): BaseViewModel() {
    @Inject
    lateinit var getChartRanksUseCase: GetChartRanksUseCase

    private val _rankingList = MutableLiveData<ArrayList<AggregateRankModel>>()
    val rankingList: LiveData<ArrayList<AggregateRankModel>> = _rankingList

    init {
        getAggregatedRanking()
    }

    fun getAggregatedRanking() {
        try {
            viewModelScope.launch {
                getChartRanksUseCase(chartCode).collectLatest { response ->
                    response.message?.let {
                        _errorToast.postValue(Event(it))
                        return@collectLatest
                    }

                    val objects = response.data?.toCollection(ArrayList()) ?: return@collectLatest
                    val idols = arrayListOf<AggregateRankModel>()

                    for (i in 0 until objects.size) {
                        val model = objects[i]
                        idols.add(model)
                    }

                    idols.sortBy { it.scoreRank }

                    _rankingList.postValue(idols)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class MiracleAggregatedViewModelFactory(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val chartCode: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MiracleAggregatedViewModel::class.java)) {
            return MiracleAggregatedViewModel(context, savedStateHandle, chartCode) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}