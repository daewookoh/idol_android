package net.ib.mn.feature.generic

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.domain.usecase.GetChartIdolIdsUseCase
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.domain.usecase.GetIdolsByIdsUseCase
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.safeActivity
import net.ib.mn.utils.livedata.Event

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: chartId 사용하는 실시간 랭킹 화면 ViewModel
 *
 * */

class GenericRankingViewModel(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val getChartIdolIdsUseCase: GetChartIdolIdsUseCase,
    private val getIdolsByIdsUseCase: GetIdolsByIdsUseCase,
    private val usersRepository: UsersRepository,
    private val chartCode: String
) : BaseViewModel() {

    private val _rankingList = MutableLiveData<Event<ArrayList<IdolModel>>>()
    val rankingList: LiveData<Event<ArrayList<IdolModel>>> = _rankingList
    private val _inActiveVote = MutableLiveData<Event<Pair<String, String>>>()
    val inActiveVote: LiveData<Event<Pair<String, String>>> = _inActiveVote
    private val _voteHeart = MutableLiveData<Event<Triple<IdolModel, Long, Long>>>()
    val voteHeart: LiveData<Event<Triple<IdolModel, Long, Long>>> = _voteHeart

    private val lock = Any()
    private var idList = arrayListOf<Int>()
    private var isUpdate = false

    init {
        genericChartIds()
    }

    fun refreshIfNeeded() {
        if (idList.isEmpty()) {
            genericChartIds()
        }
    }

    private fun genericChartIds() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val objects = getGenericChartIdsResponse()

            if (objects.isNotEmpty()) {
                val idolData = getIdolsByIdsUseCase(objects)
                    .mapListDataResource { it.toPresentation() }
                    .awaitOrThrow()

                idolData?.let {
                    idolData.forEachIndexed { i, item ->
                        item.apply {
                            rank = if (i > 0 && idolData[i - 1].heart == heart) {
                                idolData[i - 1].rank
                            } else {
                                i
                            }
                        }
                    }
                } ?: Log.d("!!!!", "3333 check data is null")

                idolData?.forEach { idol -> idList.add(idol.getId()) }

                _rankingList.postValue(Event(ArrayList(idolData)))

                isUpdate = true
            } else {
                // TODO 실패 처리
                _rankingList.postValue(Event(arrayListOf()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getGenericChartIdsResponse(): List<Int> = coroutineScope {
        val responseList = getChartIdolIdsUseCase(chartCode)
        responseList.mapNotNull { response ->
            response.data.also {
                response.message?.let { _errorToast.postValue(Event(it)) }
            }
        }.firstOrNull() ?: emptyList()
    }

    fun updateData(getIdolsByIdsUseCase: GetIdolsByIdsUseCase) = viewModelScope.launch(Dispatchers.IO) {
        if (!isUpdate) return@launch

        val idols = getIdolsByIdsUseCase(idList)
            .mapListDataResource { it.toPresentation() }
            .awaitOrThrow()

        idols?.let {
            idols.forEachIndexed { i, item ->
                item.apply {
                    rank = if (i > 0 && idols[i - 1].heart == heart) {
                        idols[i - 1].rank
                    } else {
                        i
                    }
                }
            }

            _rankingList.postValue(Event(ArrayList(idols)))
        }
    }

    fun onVote(idol: IdolModel) {
        if (Util.mayShowLoginPopup(context.safeActivity)) {
            return
        }

        Util.showProgress(context)
        viewModelScope.launch {
            usersRepository.isActiveTime(
                { response ->
                    Util.closeProgress()

                    if (!response.optBoolean("success")) {
                        _errorToast.postValue(Event(ErrorControl.parseError(context, response) ?: ""))
                        return@isActiveTime
                    }

                    if (response.optString("active") != Const.RESPONSE_Y) {
                        val start = Util.convertTimeAsTimezone(response.optString("begin"))
                        val end = Util.convertTimeAsTimezone(response.optString("end"))

                        _inActiveVote.postValue(Event(Pair(start, end)))
                        return@isActiveTime
                    }

                    val gcode = response.optInt("gcode")
                    if (response.optInt("total_heart") == 0) {
                        Util.showChargeHeartDialog(context.safeActivity)
                    } else {
                        if (response.optString("vote_able")
                                .equals(Const.RESPONSE_Y, ignoreCase = true)
                        ) {
                            _voteHeart.postValue(
                                Event(
                                    Triple(
                                        idol,
                                        response.optLong("total_heart"),
                                        response.optLong("free_heart")
                                    )
                                )
                            )
                        } else {
                            _errorToastWithCode.postValue(Event(gcode))
                        }
                    }
                }, {
                }
            )
        }
    }
}

class GenericRankingViewModelFactory(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val getChartIdolIdsUseCase: GetChartIdolIdsUseCase,
    private val getIdolsByIdsUseCase: GetIdolsByIdsUseCase,
    private val usersRepository: UsersRepository,
    private val chartCode: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GenericRankingViewModel::class.java)) {
            return GenericRankingViewModel(context, savedStateHandle, getChartIdolIdsUseCase, getIdolsByIdsUseCase, usersRepository, chartCode) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
