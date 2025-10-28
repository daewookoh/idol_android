package net.ib.mn.viewmodel

import android.content.Context
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
import kotlinx.coroutines.withContext
import net.ib.mn.activity.BaseActivity
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
import net.ib.mn.utils.livedata.Event
import net.ib.mn.utils.sort

class MiracleRankingViewModel(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val chartCode: String,
    private val getIdolsByIdsUseCase: GetIdolsByIdsUseCase,
    private val getChartIdolIdsUseCase: GetChartIdolIdsUseCase,
    private val usersRepository: UsersRepository,
) : BaseViewModel() {

    private val _rankingList = MutableLiveData<Event<ArrayList<IdolModel>>>()
    val rankingList: LiveData<Event<ArrayList<IdolModel>>> = _rankingList
    private val _inActiveVote = MutableLiveData<Event<Pair<String, String>>>()
    val inActiveVote: LiveData<Event<Pair<String, String>>> = _inActiveVote
    private val _voteHeart = MutableLiveData<Event<Triple<IdolModel, Long, Long>>>()
    val voteHeart: LiveData<Event<Triple<IdolModel, Long, Long>>> = _voteHeart

    private var idList = arrayListOf<Int>()
    private var isUpdate = false

    init {
        miracleChartIds()
    }

    private fun miracleChartIds() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val objects = getMiracleChartIdsResponse()

            val ids = objects.toCollection(ArrayList())

            val idols = getIdolsByIdsUseCase(ids)
                .mapListDataResource { it.toPresentation() }
                .awaitOrThrow()?.let {
                    ArrayList(
                        it
                    )
                }

            idols?.let {
                withContext(Dispatchers.Main) {
                    for (i in idols.indices) {
                        val item: IdolModel = idols[i]
                        // 동점자 처리
                        item.rank = if (i > 0 && idols[i - 1].heart == item.heart) {
                            idols[i - 1].rank
                        } else {
                            i
                        }
                    }

                    idols.forEach { idol -> idList.add(idol.getId()) }

                    _rankingList.postValue(Event(sort(context, idols)))

                    isUpdate = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getMiracleChartIdsResponse(): List<Int> = coroutineScope {
        val responseList = getChartIdolIdsUseCase(chartCode)
        responseList.mapNotNull { response ->
            response.data
        }.firstOrNull() ?: emptyList()
    }

    fun updateData() = viewModelScope.launch(Dispatchers.IO) {
        if (!isUpdate) return@launch

        val dbIdols = getIdolsByIdsUseCase(idList)
            .mapListDataResource { it.toPresentation() }
            .awaitOrThrow()
        dbIdols?.let {
            withContext(Dispatchers.Main) {
                _rankingList.postValue(Event(ArrayList(sort(context, ArrayList(it)))))
            }
        }
    }

    fun onVote(idol: IdolModel) {
        if (Util.mayShowLoginPopup(context as BaseActivity)) {
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
                        Util.showChargeHeartDialog(context)
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

class MiracleRankingViewModelFactory(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val chartCode: String,
    private val getIdolsByIdsUseCase: GetIdolsByIdsUseCase,
    private val getChartIdolIdsUseCase: GetChartIdolIdsUseCase,
    private val usersRepository: UsersRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MiracleRankingViewModel::class.java)) {
            return MiracleRankingViewModel(context, savedStateHandle, chartCode, getIdolsByIdsUseCase, getChartIdolIdsUseCase, usersRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}