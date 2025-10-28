/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 어워즈 공통 데이터 뷰모델.
 *
 * */

package net.ib.mn.awards.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.json.Json
import net.ib.mn.addon.IdolGson
import net.ib.mn.base.BaseViewModel
import net.ib.mn.core.model.AwardModel
import net.ib.mn.core.model.AwardChartsModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util

class AwardsMainViewModel(
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel() {

    init {
        getSaveState()
    }

    private var requestChartCode: AwardChartsModel? = null
    private var currentStatus: String? = null // 기록실하고 현재 어워드가 같은걸 쓰고있어서 status를 넣음.
    private var awardData: AwardModel? = null

    fun setAwardData(context: Context?) {
//        awardData = IdolGson.getInstance()
//            .fromJson(Util.getPreference(context, Const.AWARD_MODEL), AwardModel::class.java)
        awardData = Json{ignoreUnknownKeys = true}.decodeFromString<AwardModel>(Util.getPreference(context, Const.AWARD_MODEL))
        requestChartCode = awardData?.charts?.get(0)
        currentStatus = awardData?.name
        setSaveState(requestChartCode, currentStatus)
    }

    fun setSaveState(requestChartCodeModel: AwardChartsModel?, currentStatus: String?) {
        this.requestChartCode = requestChartCodeModel
        this.currentStatus = currentStatus

        savedStateHandle[CHART_MODEL] = requestChartCodeModel
        savedStateHandle[CURRENT_STATUS] = currentStatus
    }

    private fun getSaveState() {
        requestChartCode = savedStateHandle[CHART_MODEL]
        currentStatus = savedStateHandle[CURRENT_STATUS]
    }

    fun getAwardData(): AwardModel? = awardData

    fun getRequestChartCodeModel(): AwardChartsModel? = requestChartCode

    fun getCurrentStatus(): String? = currentStatus

    companion object {
        const val CHART_MODEL = "chart_model"
        const val CURRENT_STATUS = "current_status"
    }
}