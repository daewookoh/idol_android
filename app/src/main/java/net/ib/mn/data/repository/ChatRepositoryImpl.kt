package net.ib.mn.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.ChatApi
import net.ib.mn.data.remote.dto.ChatMemberDto
import net.ib.mn.data.remote.dto.ChatRoomCreateRequest as DtoCreateRequest
import net.ib.mn.data.remote.dto.ChatRoomDto
import net.ib.mn.data.remote.dto.ChatRoomJoinRequest
import net.ib.mn.data.remote.dto.ChatRoomLeaveRequest
import net.ib.mn.data.remote.dto.ChatRoomReportRequest
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ChatMemberModel
import net.ib.mn.domain.model.ChatRoomCreateRequest
import net.ib.mn.domain.model.ChatRoomInfoModel
import net.ib.mn.domain.model.ChatRoomJoinResponse
import net.ib.mn.domain.model.ChatRoomLeaveResponse
import net.ib.mn.domain.model.ChatRoomListResponse
import net.ib.mn.domain.model.ChatRoomModel
import net.ib.mn.data.remote.api.ImagesApi
import net.ib.mn.domain.repository.ChatImageUploadResponse
import net.ib.mn.domain.repository.ChatRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatRepository 구현체
 * old 프로젝트: core/data/src/main/java/net/ib/mn/core/data/repository/ChatRepositoryImpl.kt
 *
 * BaseRepository의 safeApiCallWithJsonString 패턴 사용
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val imagesApi: ImagesApi,
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

    override fun createChatRoom(request: ChatRoomCreateRequest): Flow<ApiResult<ChatRoomInfoModel>> =
        safeApiCallWithJsonString(
            apiCall = {
                chatApi.createChatRoom(
                    DtoCreateRequest(
                        idolId = request.idolId,
                        title = request.title,
                        desc = request.desc,
                        locale = request.locale,
                        maxPeople = request.maxPeople,
                        levelLimit = request.levelLimit,
                        isAnonymity = request.isAnonymity,
                        isMostOnly = request.isMostOnly
                    )
                )
            },
            parser = { json -> parseRoomInfoResponse(json) }
        )

    override fun getChatRoomInfo(roomId: Int): Flow<ApiResult<ChatRoomInfoModel>> =
        safeApiCallWithJsonString(
            apiCall = { chatApi.getChatRoomInfo(roomId) },
            parser = { json -> parseRoomInfoResponse(json) }
        )

    override fun deleteChatRoom(roomId: Int): Flow<ApiResult<Boolean>> =
        safeApiCallWithJsonString(
            apiCall = { chatApi.deleteChatRoom(roomId) },
            parser = { json ->
                val jsonObject = JSONObject(json)
                jsonObject.optBoolean("success", false)
            }
        )

    override fun reportChatRoom(roomId: Int, reason: String): Flow<ApiResult<Boolean>> =
        safeApiCallWithJsonString(
            apiCall = { chatApi.reportChatRoom(ChatRoomReportRequest(roomId, reason)) },
            parser = { json ->
                val jsonObject = JSONObject(json)
                jsonObject.optBoolean("success", false)
            }
        )

    override fun isReportedChatRoom(roomId: Int, userId: Long): Flow<ApiResult<Boolean>> =
        safeApiCallWithJsonString(
            apiCall = { chatApi.isReportedChatRoom(roomId, userId) },
            parser = { json ->
                val jsonObject = JSONObject(json)
                jsonObject.optBoolean("is_reported", false)
            }
        )

    override fun getChatMembers(roomId: Int): Flow<ApiResult<List<ChatMemberModel>>> =
        safeApiCallWithJsonString(
            apiCall = { chatApi.getChatMembers(roomId) },
            parser = { json -> parseMembersResponse(json, roomId) }
        )

    override fun getChatMember(roomId: Int, userId: Long): Flow<ApiResult<ChatMemberModel?>> =
        safeApiCallWithJsonString(
            apiCall = { chatApi.getChatMember(roomId, userId) },
            parser = { json ->
                val jsonObject = JSONObject(json)
                if (jsonObject.optBoolean("success", false)) {
                    val memberDto: ChatMemberDto = gson.fromJson(json, ChatMemberDto::class.java)
                    memberDto.toModel(roomId)
                } else {
                    null
                }
            }
        )

    override fun uploadChatImage(
        imageBytes: ByteArray,
        maxSize: Int
    ): Flow<ApiResult<ChatImageUploadResponse>> = safeApiCallWithJsonString(
        apiCall = {
            // old 프로젝트: OtherRepositoryImpl.uploadImage()
            val imagePart = MultipartBody.Part.createFormData(
                "imagebin",
                "image.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            val sizePart = MultipartBody.Part.createFormData("size", maxSize.toString())
            imagesApi.uploadImage(imagePart, sizePart)
        },
        parser = { json ->
            val jsonObject = JSONObject(json)
            if (jsonObject.optBoolean("success", false)) {
                ChatImageUploadResponse(
                    imageUrl = jsonObject.optString("image_url", ""),
                    thumbnailUrl = jsonObject.optString("thumbnail_url", ""),
                    umjjalUrl = jsonObject.optString("umjjal_url", ""),
                    thumbHeight = jsonObject.optInt("thumb_height", 0),
                    thumbWidth = jsonObject.optInt("thumb_width", 0)
                )
            } else {
                throw Exception(jsonObject.optString("msg", "Image upload failed"))
            }
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

    private fun parseRoomInfoResponse(body: String): ChatRoomInfoModel {
        val json = JSONObject(body)
        // old 프로젝트: response.getJSONObject("room") 에서 방 정보를 가져옴
        val room = json.optJSONObject("room") ?: json
        return ChatRoomInfoModel(
            roomId = room.optInt("id", 0),
            title = room.optString("title", null),
            description = room.optString("desc", null),
            idolId = room.optInt("idol_id", 0),
            userId = room.optInt("user_id", 0),
            socketUrl = room.optString("socket_url", null),
            curPeople = room.optInt("cur_people", 0),
            maxPeople = room.optInt("max_people", 0),
            levelLimit = room.optInt("level_limit", 0),
            totalMsgCnt = room.optInt("total_msg_cnt", 0),
            isAnonymity = room.optString("is_anonymity", "N"),
            isDefault = room.optString("is_default", "N"),
            isMostOnly = room.optString("is_most_only", "N"),
            locale = room.optString("locale", null),
            lastMsg = room.optString("last_msg", null),
            lastMsgTime = room.optLong("last_msg_time", 0),
            createdAt = room.optLong("created_at", 0),
            success = json.optBoolean("success", false),
            gcode = json.optInt("gcode", 0)
        )
    }

    private fun parseMembersResponse(body: String, roomId: Int): List<ChatMemberModel> {
        val json = JSONObject(body)

        if (json.optInt("gcode") == 9000) {
            return emptyList()
        }

        // old 프로젝트: response.getJSONArray("users")
        return json.optJSONArray("users")?.let { usersArray ->
            val listType = object : TypeToken<List<ChatMemberDto>>() {}.type
            val dtoList: List<ChatMemberDto> = gson.fromJson(usersArray.toString(), listType)
            dtoList.map { it.toModel(roomId) }
        } ?: emptyList()
    }

    private fun ChatRoomDto.toModel(isJoinedRoom: Boolean) = ChatRoomModel(
        roomId = id,
        title = title ?: "",
        desc = desc,
        idolId = idolId ?: 0,
        isAnonymity = isAnonymity,
        isDefault = isDefault,
        isMostOnly = isMostOnly,
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

    private fun ChatMemberDto.toModel(roomId: Int) = ChatMemberModel(
        id = id,
        roomId = roomId,
        nickname = nickname ?: "",
        imageUrl = imageUrl,
        level = level ?: 0,
        role = role ?: ChatMemberModel.ROLE_NORMAL,
        mostId = mostId ?: -1
    )
}
