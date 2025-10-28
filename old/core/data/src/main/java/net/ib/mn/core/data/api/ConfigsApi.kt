/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.OfferWallRewardRequest
import net.ib.mn.core.data.model.BaseDataModel
import net.ib.mn.core.data.model.ObjectsBaseDataModel
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.model.AwardsCurrentModel
import net.ib.mn.core.model.CouponModel
import net.ib.mn.core.model.IdolApiModel
import net.ib.mn.core.model.ObjectsModel
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.core.model.UpdateInfoModel
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


/**
 * @see
 * */

interface ConfigsApi {

    @GET("configs/startup/")
    suspend fun getConfigStartup(): ObjectsBaseDataModel<ObjectsModel>

    @GET("update/")
    suspend fun getUpdateInfo(): UpdateInfoModel

    @GET("idol_supports/type_list/")
    suspend fun getAdTypeList(): ObjectsBaseDataModel<List<SupportAdTypeListModel>>

    @POST("users/timezone_update/")
    suspend fun updateTimeZone(
        @Body timezone: Map<String, String>,
    ): BaseDataModel<String>

    @GET("configs/self/")
    suspend fun getConfigSelf(): String

    @GET("awards/current/")
    suspend fun getAwardData(): AwardsCurrentModel

    @GET("idols/")
    suspend fun getAwardIdol(@Query("chart_code") chartCode: String): ObjectsBaseDataModel<List<IdolApiModel>>

    @GET("messages/")
    suspend fun getMessages(
        @Query("type") type: String,
        @Query("after") after: String?
    ): ObjectsBaseDataModel<List<CouponModel>>

    @GET("users/self/")
    suspend fun getUserSelf(
        @Query("ts") ts: Int
    ): String

    @GET("users/status/")
    suspend fun getUserStatus(): String

    @POST("offerwalls/exodus_reward/")
    suspend fun getOfferWallReward(
        @Body body: OfferWallRewardRequest
    ): BaseDataModel<String>

    @GET("configs/typelist/")
    suspend fun getTypeList(): ObjectsBaseDataModel<List<TypeListModel>>

    @GET("blocks/")
    suspend fun getBlocks(@Query("id_only") idOnly: String): String // data 필드 없이 block_ids로 오기때문에 문자열로 받음

    @POST("offerwalls/videoad/alarm/")
    suspend fun postVideoAdNotification(): BaseDataModel<Unit>
}