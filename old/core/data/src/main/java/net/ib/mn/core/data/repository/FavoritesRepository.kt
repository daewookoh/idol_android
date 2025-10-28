/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import org.json.JSONObject


interface FavoritesRepository {
    suspend fun getFavoritesSelf(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun addFavorite(
        idolId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun removeFavorite(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun deleteCache(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
