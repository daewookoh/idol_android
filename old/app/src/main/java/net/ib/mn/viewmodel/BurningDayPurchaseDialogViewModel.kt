package net.ib.mn.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.ItemShopRepositoryImpl
import net.ib.mn.core.domain.usecase.GetConfigSelfUseCase
import net.ib.mn.domain.usecase.IdolUpsertUseCase
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.ItemShopModel
import net.ib.mn.model.toDomain
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.livedata.Event
import org.json.JSONArray
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class BurningDayPurchaseDialogViewModel @Inject constructor(
    private val idolUpsertUseCase: IdolUpsertUseCase
) : ViewModel() {

    private var itemLevel = 0
    private var burningDay:String? = ""
    private var diamond: Int = 0
    private val dfYmd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _callConfigSelf = MutableLiveData<Event<Boolean>>()
    val callConfigSelf: LiveData<Event<Boolean>> = _callConfigSelf

    private val _daysList = MutableLiveData<Event<Map<String,Any>>>()
    val daysList: LiveData<Event<Map<String,Any>>> = _daysList

    private val _purchaseBurningDay = MutableLiveData<Event<String?>>()
    val purchaseBurningDay: LiveData<Event<String?>> = _purchaseBurningDay

    private var isLoadingPurchasingBurning = false

    @Inject
    lateinit var itemShopRepository: ItemShopRepositoryImpl
    @Inject
    lateinit var getConfigSelfUseCase : GetConfigSelfUseCase

    fun getItemLevel() = itemLevel

    fun getBurningDay() = burningDay

    fun setBurningDay(burningDay: String?) {
        this.burningDay = burningDay
    }

    fun getDiamond() = diamond

    fun getDfYmd() = dfYmd

    fun burningDay(day: String?) {
        burningDay = day
    }

    fun setItemLevel(context: Context?) {
        itemLevel = ConfigModel.getInstance(context).itemLevel
    }

    fun getItemShopList(context: Context?) {
        viewModelScope.launch {
            itemShopRepository.getItemShopList({ response ->
                if (response.optBoolean("success")) {
                    val array: JSONArray
                    try {
                        array = response.getJSONArray("objects")
                        val gson = IdolGson.getInstance()
                        for (i in 0 until array.length()) {
                            val item = gson.fromJson(
                                array.getJSONObject(i).toString(),
                                ItemShopModel::class.java
                            )
                            if (item.market_no == 4) {
                                diamond = item.diamond_value
                                MainScope().launch {
                                    _callConfigSelf.value = Event(true)
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                } else {
                    UtilK.handleCommonError(context, response)
                }
            }, { throwable ->
                Toast.makeText(context, R.string.error_abnormal_exception, Toast.LENGTH_SHORT)
                    .show()
            })
        }
    }

    fun getConfigSelf(context: Context?) {
        viewModelScope.launch {
            val result = getConfigSelfUseCase().first()
            Util.closeProgress()
            if(result.success) {
                ConfigModel.getInstance(context).parse(result.data)
                itemLevel = ConfigModel.getInstance(context).itemLevel

                val days = ArrayList<String?>()
                val daysYmd = ArrayList<String>()
                val calendar = Calendar.getInstance()
                val dfMd = SimpleDateFormat(context?.getString(R.string.burning_day_format),
                    Locale.getDefault()
                )
                dfMd.timeZone =
                    TimeZone.getTimeZone("Asia/Seoul")
                dfYmd.timeZone =
                    TimeZone.getTimeZone("Asia/Seoul") // 혹시몰라서 이것도 위에거랑 동일하게 KST로 맞춤.
                for (i in 0..9) {
                    val day = dfMd.format(calendar.time)
                    days.add(day)
                    daysYmd.add(dfYmd.format(calendar.time))
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                _daysList.postValue(
                    Event(
                        mapOf(
                            "days" to days,
                            "daysYmd" to daysYmd,
                        )
                    )
                )
            } else {
                val responseMsg =
                    ErrorControl.parseError(context, result.data)
                if (responseMsg != null) {
                    Toast.makeText(
                        context,
                        responseMsg,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun purchaseItemBurningDay(context: Context?) {
        if (isLoadingPurchasingBurning) {
            return
        }
        isLoadingPurchasingBurning = true

        viewModelScope.launch {
            itemShopRepository.purchaseItemBurningDay(
                burningDay!!,
                { response ->
                    MainScope().launch {
                        Util.closeProgress()
                        isLoadingPurchasingBurning = false
                        if (response.optBoolean("success")) {
                            _purchaseBurningDay.value = Event(burningDay)
                        } else {
                            Util.showDefaultIdolDialogWithBtn1(
                                context,
                                null,
                                response.optString("msg"),
                            ) { Util.closeIdolDialog() }
                        }
                    }
                }, { throwable ->
                    MainScope().launch {
                        Util.closeProgress()
                        Toast.makeText(
                            context,
                            R.string.error_abnormal_exception,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }

    fun upsertIdol(idol: IdolModel) = viewModelScope.launch(Dispatchers.IO) {
        idolUpsertUseCase(idol.toDomain()).first()
    }
}