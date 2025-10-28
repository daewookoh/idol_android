/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.config

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.model.AwardModel
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.model.CouponModel
import net.ib.mn.core.model.IdolApiModel
import net.ib.mn.core.model.ObjectsModel
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.core.model.UpdateInfoModel
import org.json.JSONArray
import org.json.JSONObject


/**
 * @see
 * */

interface ConfigRepository {
    suspend fun getConfigStartup(): Flow<BaseModel<ObjectsModel>>
    suspend fun getUpdate() : Flow<BaseModel<UpdateInfoModel>>
    suspend fun getAdTypeList() : Flow<BaseModel<List<SupportAdTypeListModel>>>
    suspend fun updateTimeZone(currentTime : Map<String, String>) : Flow<BaseModel<String>>
    suspend fun getConfigSelf() : Flow<BaseModel<JSONObject>>
    suspend fun getAwardData(): Flow<AwardModel>
    suspend fun getAwardIdol(chartCode: String) : Flow<BaseModel<List<IdolApiModel>>>
    suspend fun getMessages(type: String, after: String?) : Flow<BaseModel<List<CouponModel>>>
    suspend fun getUserSelf(ts: Int) : Flow<BaseModel<JSONObject>>
    suspend fun getUserStatus() : Flow<BaseModel<JSONObject>>
    suspend fun getOfferWall(to : String) : Flow<BaseModel<String>>
    suspend fun getTypeList() : Flow<BaseModel<List<TypeListModel>>>
    suspend fun getBlocks(idOnly: String) : Flow<BaseModel<JSONObject>>
    suspend fun postVideoAdNotification(): Flow<BaseModel<Unit>>
}