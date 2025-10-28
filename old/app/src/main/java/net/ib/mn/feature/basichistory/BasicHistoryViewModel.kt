package net.ib.mn.feature.basichistory

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.common.util.logE
import net.ib.mn.core.data.model.ChartCodeInfo
import net.ib.mn.core.data.repository.HofsRepository
import net.ib.mn.core.domain.usecase.TrendsTop100UseCase
import net.ib.mn.model.HallModel
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * 최다득표 순위 탑100
 * 1등한 횟수
 */
@HiltViewModel
class BasicHistoryViewModel @Inject constructor(
    private val trendsTop100UseCase: TrendsTop100UseCase,
    private val hofsRepository: HofsRepository,
): ViewModel() {

    private val _uiState: MutableStateFlow<BasicHistoryUiState> =
        MutableStateFlow(BasicHistoryUiState.Loading)
    val uiState: StateFlow<BasicHistoryUiState> = _uiState

    fun getHighestTicket(context: Context, chartCodes: ArrayList<ChartCodeInfo>) = viewModelScope.launch {
        val response = getHighestTicketResponse(context)

        try {
            val map = mutableMapOf<String, ArrayList<HallModel>>()
            if (response.optBoolean("success")) {
                chartCodes.forEach {
                    val codeObject = response.getJSONObject(it.code)
                    val dataArray: JSONArray = codeObject.getJSONArray("objects")

                    val gson = getInstance(true)

                    val items = arrayListOf<HallModel>()
                    for (i in 0 until dataArray.length()) {
                        items.add(
                            gson.fromJson(
                                dataArray.getJSONObject(i).toString(),
                                HallModel::class.java
                            )
                        )
                    }

                    items.apply {
                        sortByDescending { item -> item.heart }
                        forEachIndexed { index, item ->
                            if (index > 0 && items[index - 1].heart == item.heart) {
                                item.rank = items[index - 1].rank
                            } else {
                                item.rank = index
                            }
                        }
                    }

                    map[it.code] = items
                }

                _uiState.emit(BasicHistoryUiState.Success(map))
            } else {
            }
        } catch (e: Exception) {
            // TODO: error handling
            logE("${e.printStackTrace()}")
        }
    }

    fun getTop1Count(context: Context, chartCodes: ArrayList<ChartCodeInfo>) = viewModelScope.launch {
        val response = getTop1CountResponse(context)

        try {
            val map = mutableMapOf<String, ArrayList<HallModel>>()
            if (response.optBoolean("success")) {
                chartCodes.forEach {
                    val dataArray: JSONArray = response.getJSONArray(it.code)

                    val gson = getInstance(true)

                    val items = arrayListOf<HallModel>()
                    for (i in 0 until dataArray.length()) {
                        items.add(
                            gson.fromJson(
                                dataArray.getJSONObject(i).toString(),
                                HallModel::class.java
                            )
                        )
                    }

                    items.apply {
                        sortByDescending { item -> item.count }
                        forEachIndexed { index, item ->
                            if (index > 0 && items[index - 1].count == item.count) {
                                item.rank = items[index - 1].rank
                            } else {
                                item.rank = index
                            }
                        }
                    }

                    map[it.code] = items
                }

                _uiState.emit(BasicHistoryUiState.Success(map))
            } else {
                logE("Fail")
            }
        } catch (e: Exception) {
            // TODO: error handling
            logE(e.message ?: "")
        }
    }

    private suspend fun getHighestTicketResponse(context: Context): JSONObject {
        return trendsTop100UseCase().first()
    }

    private suspend fun getTop1CountResponse(context: Context): JSONObject {
        return withContext(Dispatchers.IO) {
            val response = hofsRepository.getTop1Count().first()
            response.data ?: JSONObject()
        }
    }
}