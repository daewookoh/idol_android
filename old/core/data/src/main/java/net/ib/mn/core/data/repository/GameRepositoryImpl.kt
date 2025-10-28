/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author DaewooKoh <daewoo@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.api.GameApi
import net.ib.mn.core.data.dto.GameFeeDTO
import net.ib.mn.core.data.dto.GameRewardDTO
import org.json.JSONObject
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    private val gameApi: GameApi
) : GameRepository, BaseRepository() {
    
    override suspend fun payGameFee(
        heart: Int,
        gameId: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val request = GameFeeDTO(heart, gameId)
            val response = gameApi.payGameFee(request)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }
    
    override suspend fun claimGameReward(
        gameId: String,
        score: Int,
        logHeartId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val request = GameRewardDTO(gameId, score, logHeartId)
            val response = gameApi.claimGameReward(request)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }
}
