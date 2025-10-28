/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author DaewooKoh <daewoo@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.GameFeeDTO
import net.ib.mn.core.data.dto.GameRewardDTO
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GameApi {
    
    @POST("game/fee/")
    suspend fun payGameFee(
        @Body body: GameFeeDTO
    ): Response<ResponseBody>
    
    @POST("game/reward/")
    suspend fun claimGameReward(
        @Body body: GameRewardDTO
    ): Response<ResponseBody>
}
