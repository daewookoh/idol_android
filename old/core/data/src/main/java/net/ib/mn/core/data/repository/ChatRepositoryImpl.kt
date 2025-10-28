/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.api.ChatApi
import net.ib.mn.core.data.dto.ChatRoomDTO
import net.ib.mn.core.data.dto.ReportChatRoomDTO
import net.ib.mn.core.data.model.ChatRoomCreateModel
import org.json.JSONObject
import javax.inject.Inject


class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi
) : ChatRepository, BaseRepository() {
    override suspend fun createChatRoom(
        model: ChatRoomCreateModel,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = chatApi.createChatRoom(model)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getChatRoomInfo(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = chatApi.getChatRoomInfo(id)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun deleteChatRoom(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = chatApi.deleteChatRoom(id)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun reportChatRoom(
        id: Int,
        reason: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = chatApi.reportChatRoom(ReportChatRoomDTO(id, reason))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun isReportChatRoom(
        id: Int,
        userId: Long,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = chatApi.isReportChatRoom(id, userId)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun leaveChatRoom(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = chatApi.leaveChatRoom(ChatRoomDTO(id))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getChatMembers(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = chatApi.getChatMembers(id)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getChatMember(
        id: Int,
        userId: Long,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = chatApi.getChatMember(id, userId)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getChatRoomList(
        idolId: Int,
        locale: String?,
        orderBy: Int?,
        limit: Int?,
        offset: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val order = when (orderBy) {
                0 -> "-id" // 최신순
                1 -> "-total_msg_cnt" // 대화 많은 순
                else -> null
            }
            val response = chatApi.getChatRoomList(idolId, locale, order, limit, offset)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getChatRoomJoinList(
        idolId: Int,
        locale: String?,
        orderBy: Int?,
        limit: Int?,
        offset: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val order = when (orderBy) {
                0 -> "-id" // 최신순
                1 -> "-total_msg_cnt" // 대화 많은 순
                else -> null
            }
            val response = chatApi.getChatRoomJoinList(idolId, locale, order, limit, offset)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun joinChatRoom(
        roomId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = chatApi.joinChatRoom(ChatRoomDTO(roomId))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}
