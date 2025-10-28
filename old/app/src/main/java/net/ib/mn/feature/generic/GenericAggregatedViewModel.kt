package net.ib.mn.feature.generic

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.core.domain.usecase.GetChartRanksUseCase
import net.ib.mn.utils.livedata.Event

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: chartId 사용하는 누적 순위 화면 ViewModel
 *
 * */

class GenericAggregatedViewModel(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val getChartRanksUseCase: GetChartRanksUseCase,
    private val chartCode: String
): BaseViewModel() {

    private val _rankingList = MutableLiveData<ArrayList<AggregateRankModel>>()
    val rankingList: LiveData<ArrayList<AggregateRankModel>> = _rankingList

    fun getAggregatedRanking() = viewModelScope.launch(Dispatchers.IO) {
        getChartRanksUseCase(chartCode).collectLatest { response ->
            response.message?.let {
                _errorToast.postValue(Event(it))
                return@collectLatest
            }

            response.data?.let { data ->
                val idols = data.toCollection(ArrayList())
                idols.sortBy { it.scoreRank }
                _rankingList.postValue(idols)
            }

            return@collectLatest
        }
    }
}

class GenericAggregatedViewModelFactory(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val getChartRanksUseCase: GetChartRanksUseCase,
    private val chartCode: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GenericAggregatedViewModel::class.java)) {
            return GenericAggregatedViewModel(context, savedStateHandle, getChartRanksUseCase, chartCode) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}