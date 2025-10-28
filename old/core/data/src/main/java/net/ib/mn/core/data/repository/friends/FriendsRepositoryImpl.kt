/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.friends

import net.ib.mn.core.data.api.FriendsApi
import net.ib.mn.core.data.dto.CancelFriendRequestDTO
import net.ib.mn.core.data.dto.DeleteFriendsDTO
import net.ib.mn.core.data.dto.FriendRequestDTO
import net.ib.mn.core.data.dto.GiveHeartDTO
import net.ib.mn.core.data.dto.RespondRequestDTO
import net.ib.mn.core.data.repository.BaseRepository
import org.json.JSONObject
import javax.inject.Inject


/**
 * @see
 * */

class FriendsRepositoryImpl @Inject constructor(
    private val friendsApi: FriendsApi
) : FriendsRepository, BaseRepository() {
    override suspend fun getFriendsSelf(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = friendsApi.getFriendsSelf()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun giveHeart(
        friendId: Long,
        number: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = friendsApi.giveHeart(GiveHeartDTO(friendId, number))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun giveAllHeart(
        number: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = friendsApi.giveAllHeart(GiveHeartDTO(number = number))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun receiveFriendHeart(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = friendsApi.receiveFriendHeart()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun sendFriendRequest(
        partnerId: Long,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = friendsApi.sendFriendRequest(FriendRequestDTO(partnerId))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun respondFriendRequest(
        partnerId: Long,
        ans: Boolean,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = friendsApi.respondFriendRequest(RespondRequestDTO(partnerId, if(ans) "Y" else "N"))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun respondAllFriendRequest(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = friendsApi.respondAllFriendRequest()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun cancelFriendRequest(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = friendsApi.cancelFriendRequest(CancelFriendRequestDTO(id))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun deleteFriends(
        ids: List<Int>,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = friendsApi.deleteFriends(DeleteFriendsDTO(ids))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getFriendInfo(
        userId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = friendsApi.getFriendInfo(userId)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}