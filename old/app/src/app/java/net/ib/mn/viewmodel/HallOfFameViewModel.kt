package net.ib.mn.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.volley.toolbox.RequestFuture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.addon.IdolGson
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.core.data.model.ChartCodeInfo
import net.ib.mn.core.data.repository.HofsRepository
import net.ib.mn.core.domain.usecase.GetChartRanksUseCase
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.model.HallHistoryModel
import net.ib.mn.model.HallModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Toast
import net.ib.mn.utils.livedata.Event
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class HallOfFameViewModel @Inject constructor(
    private val getChartRanksUseCase: GetChartRanksUseCase,
    private val getIdolByIdUseCase: GetIdolByIdUseCase
) : BaseViewModel() {

    private val _dayHofList = MutableLiveData<List<HallModel>>()
    val dayHofList: LiveData<List<HallModel>> = _dayHofList

    private val _tagList = MutableLiveData<List<String?>>()
    val tagList: LiveData<List<String?>> = _tagList

    private val _historyList = MutableLiveData<List<HallHistoryModel>>()
    val historyList: LiveData<List<HallHistoryModel>> = _historyList

    private val _setPrevNextVisibility = MutableLiveData<Event<Boolean>>()
    val setPrevNextVisibility: LiveData<Event<Boolean>> = _setPrevNextVisibility

    private val presentDayHofList = mutableListOf<HallModel>()
    private val presentHistoryList = mutableListOf<HallHistoryModel>()
    private val presentTagList = mutableListOf<String?>()

    private val _rankingList = MutableLiveData<Event<ArrayList<AggregateRankModel>>>()
    val rankingList: LiveData<Event<ArrayList<AggregateRankModel>>> = _rankingList

    private val _idol = MutableLiveData<Event<IdolModel>>()
    val idol: LiveData<Event<IdolModel>> = _idol

    private var maleChartCodes = arrayListOf<ChartCodeInfo>()
    private var feMaleChartCodes = arrayListOf<ChartCodeInfo>()

    private var cacheAggRankList = mutableMapOf<String, ArrayList<AggregateRankModel>>()

    @Inject
    lateinit var hofsRepository: HofsRepository

    fun setChartCodes(male: ArrayList<ChartCodeInfo>, female: ArrayList<ChartCodeInfo>) {
        maleChartCodes.addAll(male)
        feMaleChartCodes.addAll(female)
    }

    fun getMaleChartCodes() = maleChartCodes

    fun getFemaleChartCodes() = feMaleChartCodes

    fun getChartCode(position: Int, isMale: Boolean): String {
        if (getMaleChartCodes().isEmpty() || getFemaleChartCodes().isEmpty()) {
            return ""
        } else {
            return if (isMale) {
                getMaleChartCodes()[position].code
            } else {
                getFemaleChartCodes()[position].code
            }
        }
    }

    fun getHofDayData(context: Context, chartCode: String, historyParam: String?) {
        viewModelScope.launch {

            _setPrevNextVisibility.postValue(Event(true))
            val future = RequestFuture.newFuture<JSONObject>()

            hofsRepository.get(code = chartCode,
                historyParam = historyParam,
                listener = { response ->
                    if (!response.optBoolean("success")) {

                        val responseMsg = ErrorControl.parseError(context, response) ?: return@get
                        CoroutineScope(Dispatchers.Main).launch {
                            _errorToast.value = Event(responseMsg)
                        }
                        return@get
                    }

                    val array = response.getJSONArray("objects")
                    val gson = IdolGson.getInstance(true)
                    presentDayHofList.clear()
                    for (i in 0 until array.length()) {
                        presentDayHofList.add(
                            gson.fromJson(
                                array.getJSONObject(i).toString(),
                                HallModel::class.java
                            )
                        )
                    }

                    val top3 = presentDayHofList.sortedByDescending { it.heart }.take(3)
                    val rankMap = top3.mapIndexed { index, hallModel -> hallModel to index }.toMap()

                    presentDayHofList.forEach { hallModel ->
                        rankMap[hallModel]?.let { rank ->
                            hallModel.rank = rank
                        }
                    }


                    _dayHofList.postValue(presentDayHofList.reversed())

                    val historyJosnArray = response.getJSONArray("history")

                    // historyParam이 없을 때에만 historyArray를 갱신. 이렇게 안하면 다른달 것 볼 때 월 목록이 이상해짐.
                    if (historyParam != null) {
                        return@get
                    }

                    presentHistoryList.clear()
                    presentTagList.clear()
                    for (i in 0 until historyJosnArray.length()) {
                        presentHistoryList.add(
                            gson.fromJson(
                                historyJosnArray.getJSONObject(i).toString(),
                                HallHistoryModel::class.java
                            )
                        )
                    }

                    presentTagList.add(null)

                    presentHistoryList.reverse()
                    for (i in presentHistoryList.indices) {
                        // 서버에서 내려준 날짜 기간
                        val tag = ("&" + presentHistoryList[i].historyParam
                            + "&" + presentHistoryList[i].nextHistoryParam)
                        presentTagList.add(tag)
                    }

                    _historyList.postValue(presentHistoryList.toList())
                    _tagList.postValue(presentTagList.toList())
                },
                errorListener = { throwable ->
                    Toast.makeText(
                        context,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
            })
        }
    }

    fun getAggregatedRanking(
        context: Context,
        chartCode: String
    ) {
        if (cacheAggRankList[chartCode] != null) {
            _rankingList.postValue(Event(cacheAggRankList[chartCode] ?: arrayListOf()))
            return
        }

        try {
            viewModelScope.launch {
                getChartRanksUseCase(chartCode).collectLatest { response ->
                    response.message?.let {
                        _errorToast.postValue(Event(it))
                        return@collectLatest
                    }

                    response.data?.let { data ->
                        val idols = data.toCollection(ArrayList())
                        idols.sortBy { it.scoreRank }

                        val sortedByDiff = idols.sortedWith(Comparator { lhs, rhs ->
                            when {
                                lhs.difference > rhs.difference -> -1
                                lhs.difference < rhs.difference -> 1
                                else -> lhs.scoreRank.compareTo(rhs.scoreRank)
                            }
                        })

                        val topDifferenceItems = mutableListOf<AggregateRankModel>()

                        for (i in sortedByDiff.indices) {
                            val item = sortedByDiff[i]

                            if (item.status.equals("increase", ignoreCase = true)) {
                                // 최초 설정 또는 공동 차이의 항목일 경우 추가
                                if (topDifferenceItems.isEmpty() || item.difference == topDifferenceItems[0].difference) {
                                    topDifferenceItems.add(item)
                                } else {
                                    break // 차이가 달라지면 루프 중단
                                }
                            }
                        }

                        topDifferenceItems.forEach { item ->
                            idols.find { it == item }?.apply {
                                this.suddenIncrease = true
                            }
                        }

                        for (i in idols.indices) {
                            val item = idols[i]
                            if (i > 0 && idols[i - 1].score == item.score) {
                                item.scoreRank = idols[i - 1].scoreRank
                            } else {
                                item.scoreRank = i
                            }
                        }

                        cacheAggRankList[chartCode] = idols

                        _rankingList.postValue(Event(idols))
                    }

                    return@collectLatest
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getIdolInfo(idolId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val idol = getIdolByIdUseCase(idolId)
            .mapDataResource { it }
            .awaitOrThrow()

        idol?.let {
            _idol.postValue(Event(idol.toPresentation()))
        }
    }
}