package net.ib.mn.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val gson: Gson
) : ChatRepository {

    override fun getJoinedChatRooms(
        idolId: Int,
        locale: String?,
        orderBy: Int,
        limit: Int,
        offset: Int
    ): Flow<ApiResult<ChatRoomListResponse>> = flow {
        emit(ApiResult.Loading)
        try {
            val orderByString = getOrderByString(orderBy)
            val response = chatApi.getChatRoomJoinList(idolId, locale, orderByString, limit, offset)

            if (response.isSuccessful) {
                response.body()?.string()?.let { body ->
                    emit(ApiResult.Success(parseRoomListResponse(body, isJoinedRoom = true)))
                } ?: emit(ApiResult.Error(Exception("Empty response")))
            } else {
                emit(ApiResult.Error(Exception("API Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e))
        }
    }

    override fun getAllChatRooms(
        idolId: Int,
        locale: String?,
        orderBy: Int,
        limit: Int,
        offset: Int
    ): Flow<ApiResult<ChatRoomListResponse>> = flow {
        emit(ApiResult.Loading)
        try {
            val orderByString = getOrderByString(orderBy)
            val response = chatApi.getChatRoomList(idolId, locale, orderByString, limit, offset)

            if (response.isSuccessful) {
                response.body()?.string()?.let { body ->
                    emit(ApiResult.Success(parseRoomListResponse(body, isJoinedRoom = false)))
                } ?: emit(ApiResult.Error(Exception("Empty response")))
            } else {
                emit(ApiResult.Error(Exception("API Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e))
        }
    }

    override fun joinChatRoom(roomId: Int): Flow<ApiResult<ChatRoomJoinResponse>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = chatApi.joinChatRoom(ChatRoomJoinRequest(roomId))

            if (response.isSuccessful) {
                response.body()?.string()?.let { body ->
                    val json = JSONObject(body)
                    emit(ApiResult.Success(ChatRoomJoinResponse(
                        success = json.optBoolean("success", false),
                        nickname = json.optString("nickname", null),
                        userId = json.optInt("user_id", 0),
                        message = json.optString("msg", null)
                    )))
                } ?: emit(ApiResult.Error(Exception("Empty response")))
            } else {
                emit(ApiResult.Error(Exception("API Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e))
        }
    }

    override fun leaveChatRoom(roomId: Int): Flow<ApiResult<ChatRoomLeaveResponse>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = chatApi.leaveChatRoom(ChatRoomLeaveRequest(roomId))

            if (response.isSuccessful) {
                response.body()?.string()?.let { body ->
                    val json = JSONObject(body)
                    emit(ApiResult.Success(ChatRoomLeaveResponse(
                        success = json.optBoolean("success", false),
                        message = json.optString("msg", null)
                    )))
                } ?: emit(ApiResult.Error(Exception("Empty response")))
            } else {
                emit(ApiResult.Error(Exception("API Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e))
        }
    }

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
