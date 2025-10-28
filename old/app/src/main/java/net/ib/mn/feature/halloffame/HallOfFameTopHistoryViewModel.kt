package net.ib.mn.feature.halloffame

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.data.repository.TrendsRepositoryImpl
import net.ib.mn.model.HallTopModel
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Util
import net.ib.mn.utils.livedata.Event
import org.json.JSONArray
import org.json.JSONException
import javax.inject.Inject

@HiltViewModel
class HallOfFameTopHistoryViewModel @Inject constructor(
    private val trendsRepository: TrendsRepositoryImpl
) : BaseViewModel() {

    private val _hofTopList = MutableLiveData<Event<ArrayList<HallTopModel>>>()
    val hofTopList: LiveData<Event<ArrayList<HallTopModel>>> = _hofTopList

    fun getHallTop(
        context: Context,
        type: String,
        category: String,
        dateParam: String,
        chartCode: String
    ) {
        Util.showProgress(context)

        viewModelScope.launch {
            trendsRepository.dailyHistory(
                type,
                category,
                dateParam,
                chartCode,
                { response ->
                    Util.closeProgress()

                    if (response!!.optBoolean("success")) {
                        val items: ArrayList<HallTopModel> = ArrayList()
                        val array: JSONArray

                        try {
                            array = response.getJSONArray("objects")
                            val gson = getInstance(true)
                            for (i in 0 until array.length()) {
                                items.add(
                                    gson.fromJson(
                                        array.getJSONObject(i).toString(),
                                        HallTopModel::class.java
                                    )
                                )
                            }

                            _hofTopList.postValue(Event(items))
                        } catch (e: JSONException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                        }
                    } else {
                        Util.closeProgress()
                        val responseMsg =
                            ErrorControl.parseError(context, response)
                        responseMsg?.let {
                            _errorToast.postValue(Event(responseMsg))
                        }
                    }
                }, { throwable ->
                    _errorToast.postValue(Event(throwable.message ?: ""))
                }
            )
        }
    }
}
