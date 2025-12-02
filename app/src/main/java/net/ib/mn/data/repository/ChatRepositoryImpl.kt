package net.ib.mn.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.ChatApi
import net.ib.mn.data.remote.dto.ChatRoomDto
import net.ib.mn.data.remote.dto.ChatRoomJoinRequest
import net.ib.mn.data.remote.dto.ChatRoomLeaveRequest
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ChatRoomJoinResponse
import net.ib.mn.domain.model.ChatRoomLeaveResponse
import net.ib.mn.domain.model.ChatRoomListResponse
import net.ib.mn.domain.model.ChatRoomModel
import net.ib.mn.domain.repository.ChatRepository
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatRepository 구현체
 *
 * BaseRepository의 safeApiCallWithJsonString 패턴 사용
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val gson: Gson
) : BaseRepository(), ChatRepository {

    override fun getJoinedChatRooms(
        idolId: Int,
        locale: String?,
        orderBy: Int,
        limit: Int,
        offset: Int
    ): Flow<ApiResult<ChatRoomListResponse>> = safeApiCallWithJsonString(
        apiCall = {
            chatApi.getChatRoomJoinList(idolId, locale, getOrderByString(orderBy), limit, offset)
        },
        parser = { json -> parseRoomListResponse(json, isJoinedRoom = true) }
    )

    override fun getAllChatRooms(
        idolId: Int,
        locale: String?,
        orderBy: Int,
        limit: Int,
        offset: Int
    ): Flow<ApiResult<ChatRoomListResponse>> = safeApiCallWithJsonString(
        apiCall = {
            chatApi.getChatRoomList(idolId, locale, getOrderByString(orderBy), limit, offset)
        },
        parser = { json -> parseRoomListResponse(json, isJoinedRoom = false) }
    )

    override fun joinChatRoom(roomId: Int): Flow<ApiResult<ChatRoomJoinResponse>> =
        safeApiCallWithJsonString(
            apiCall = { chatApi.joinChatRoom(ChatRoomJoinRequest(roomId)) },
            parser = { json ->
                val jsonObject = JSONObject(json)
                ChatRoomJoinResponse(
                    success = jsonObject.optBoolean("success", false),
                    nickname = jsonObject.optString("nickname", null),
                    userId = jsonObject.optInt("user_id", 0),
                    message = jsonObject.optString("msg", null)
                )
            }
        )

    override fun leaveChatRoom(roomId: Int): Flow<ApiResult<ChatRoomLeaveResponse>> =
        safeApiCallWithJsonString(
            apiCall = { chatApi.leaveChatRoom(ChatRoomLeaveRequest(roomId)) },
            parser = { json ->
                val jsonObject = JSONObject(json)
                ChatRoomLeaveResponse(
                    success = jsonObject.optBoolean("success", false),
                    message = jsonObject.optString("msg", null)
                )
            }
        )

    // ============================================================
    // Private Helpers
    // ============================================================

    private fun getOrderByString(orderBy: Int) = when (orderBy) {
        ChatRepository.ORDER_BY_TALK_COUNT -> "-total_msg_cnt"
        else -> "-id"
    }

    private fun parseRoomListResponse(body: String, isJoinedRoom: Boolean): ChatRoomListResponse {
        val json = JSONObject(body)

        if (json.optInt("gcode") == 9000) {
            return ChatRoomListResponse(rooms = emptyList(), totalCount = 0, nextUrl = null)
        }

        val meta = json.optJSONObject("meta")
        val totalCount = meta?.optInt("total_count") ?: 0
        val nextUrl = meta?.optString("next")?.takeIf { it != "null" && it.isNotEmpty() }

        val rooms = json.optJSONArray("objects")?.let { objectsArray ->
            val listType = object : TypeToken<List<ChatRoomDto>>() {}.type
            val dtoList: List<ChatRoomDto> = gson.fromJson(objectsArray.toString(), listType)
            dtoList.map { it.toModel(isJoinedRoom) }
        } ?: emptyList()

        return ChatRoomListResponse(rooms = rooms, totalCount = totalCount, nextUrl = nextUrl)
    }

    private fun ChatRoomDto.toModel(isJoinedRoom: Boolean) = ChatRoomModel(
        roomId = id,
        title = title ?: "",
        desc = desc,
        idolId = idolId ?: 0,
        isAnonymity = isAnonymity == "Y",
        isDefault = isDefault == "Y",
        isMostOnly = isMostOnly == "Y",
        levelLimit = levelLimit ?: 0,
        curPeopleCount = curPeople ?: 0,
        maxPeopleCount = maxPeople ?: 0,
        totalMsgCount = totalMsgCnt ?: 0,
        lastMessage = lastMsg,
        lastMessageTime = lastMsgTime,
        locale = locale,
        createdAt = createdAt,
        updatedAt = updatedAt,
        userId = userId ?: 0,
        role = role,
        nickName = nickname,
        isJoinedRoom = isJoinedRoom
    )
}
