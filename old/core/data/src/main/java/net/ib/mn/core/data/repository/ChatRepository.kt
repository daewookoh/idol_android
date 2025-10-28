/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.model.ChatRoomCreateModel
import org.json.JSONObject


interface ChatRepository {
    suspend fun createChatRoom(
        model: ChatRoomCreateModel,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getChatRoomInfo(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun deleteChatRoom(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun reportChatRoom(
        id: Int,
        reason: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun isReportChatRoom(
        id: Int,
        userId: Long,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun leaveChatRoom(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getChatMembers(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getChatMember(
        id: Int,
        userId: Long,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getChatRoomList(
        idolId: Int,
        locale: String? = null,
        orderBy: Int? = null,
        limit: Int? = null,
        offset: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getChatRoomJoinList(
        idolId: Int,
        locale: String? = null,
        orderBy: Int? = null,
        limit: Int? = null,
        offset: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun joinChatRoom(
        roomId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
