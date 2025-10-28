/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author DaewooKoh <daewoo@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import org.json.JSONObject

interface GameRepository {
    suspend fun payGameFee(
        heart: Int,
        gameId: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    
    suspend fun claimGameReward(
        gameId: String,
        score: Int,
        logHeartId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
