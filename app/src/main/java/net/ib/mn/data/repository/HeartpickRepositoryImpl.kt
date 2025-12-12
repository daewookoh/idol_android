package net.ib.mn.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.HeartpickApi
import net.ib.mn.data.remote.dto.HeartPickVoteDTO
import net.ib.mn.data.remote.dto.HeartPickVoteResponse
import net.ib.mn.data.remote.dto.OpenNotificationDTO
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.CommentModel
import net.ib.mn.domain.model.HeartPickCommentsResponse
import net.ib.mn.domain.model.HeartPickModel
import net.ib.mn.domain.repository.HeartpickRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

/**
 * HeartpickRepository 구현체
 *
 * BaseRepository의 safeApiCall + extractObject/extractList 패턴 사용
 */
class HeartpickRepositoryImpl @Inject constructor(
    private val heartpickApi: HeartpickApi,
    private val gson: Gson
) : BaseRepository(), HeartpickRepository {

    override fun getHeartPickList(offset: Int, limit: Int): Flow<ApiResult<List<HeartPickModel>>> =
        safeApiCall { heartpickApi.getHeartPickList(offset, limit) }
            .extractList { it.objects }

    override fun getHeartPick(id: Int): Flow<ApiResult<HeartPickModel>> =
        safeApiCall { heartpickApi.getHeartPick(id) }
            .extractObject({ it.`object` }, "Heart pick not found")

    override fun getReplies(
        heartPickId: Int,
        limit: Int,
        cursor: String?
    ): Flow<ApiResult<HeartPickCommentsResponse>> = safeApiCallWithJsonString(
        apiCall = { heartpickApi.getReplies(heartPickId, limit, cursor) },
        parser = { json -> parseHeartPickCommentsResponse(json) }
    )

    override fun postReply(
        heartPickId: Int,
        content: String,
        emoticonId: Int?,
        imageBytes: ByteArray?
    ): Flow<ApiResult<Boolean>> = safeApiCallWithJsonString(
        apiCall = {
            val idPart = createPart("id", heartPickId.toString())
            val contentPart = createPart("content", content)
            val emoticonPart = emoticonId?.takeIf { it != CommentModel.NO_EMOTICON_ID }?.let {
                createPart("emoticon", it.toString())
            }
            val imagePart = imageBytes?.let {
                MultipartBody.Part.createFormData(
                    "image",
                    "image.jpg",
                    it.toRequestBody("image/*".toMediaTypeOrNull())
                )
            }
            heartpickApi.postReply(
                id = idPart,
                emoticon = emoticonPart,
                content = contentPart,
                image = imagePart
            )
        },
        parser = { true }
    )

    override fun getOpenHeartPickNotification(heartPickId: Int): Flow<ApiResult<Boolean>> =
        safeApiCallWithJsonString(
            apiCall = { heartpickApi.getOpenHeartPickNotification(heartPickId) },
            parser = { json -> parseNotificationResponse(json, heartPickId) }
        )

    override fun postOpenHeartPickNotification(heartPickId: Int): Flow<ApiResult<Boolean>> =
        safeApiCallWithJsonString(
            apiCall = { heartpickApi.postOpenHeartPickNotification(OpenNotificationDTO(heartPickId)) },
            parser = { true }
        )

    override fun voteHeartPick(
        heartPickId: Int,
        heartPickIdolId: Int,
        number: Long
    ): Flow<ApiResult<HeartPickVoteResponse>> = safeApiCallWithJsonString(
        apiCall = {
            heartpickApi.voteHeartPick(
                HeartPickVoteDTO(
                    heartpickId = heartPickId,
                    heartpickIdolId = heartPickIdolId,
                    number = number
                )
            )
        },
        parser = { json -> parseHeartPickVoteResponse(json) }
    )

    // ============================================================
    // Private Helpers
    // ============================================================

    private fun parseHeartPickCommentsResponse(json: String): HeartPickCommentsResponse {
        return try {
            val jsonObject = JSONObject(json)
            val objectsArray = jsonObject.getJSONArray("objects")
            val listType = object : TypeToken<List<CommentModel>>() {}.type
            val comments: List<CommentModel> = gson.fromJson(objectsArray.toString(), listType)

            val meta = jsonObject.optJSONObject("meta")
            var nextCursor: String? = meta?.optString("next_cursor", "")
            if (nextCursor.isNullOrEmpty() || nextCursor == "null") nextCursor = null

            HeartPickCommentsResponse(
                comments = comments,
                nextCursor = nextCursor,
                hasMore = !nextCursor.isNullOrEmpty()
            )
        } catch (e: Exception) {
            HeartPickCommentsResponse(comments = emptyList())
        }
    }

    private fun parseNotificationResponse(json: String, heartPickId: Int): Boolean {
        return try {
            val jsonObject = JSONObject(json)
            val objectsArray = jsonObject.optJSONArray("objects")
            if (objectsArray != null) {
                val listType = object : TypeToken<List<Int>>() {}.type
                val ids: List<Int> = gson.fromJson(objectsArray.toString(), listType)
                ids.contains(heartPickId)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun parseHeartPickVoteResponse(json: String): HeartPickVoteResponse {
        return try {
            val jsonObject = JSONObject(json)
            HeartPickVoteResponse(
                bonusHeart = jsonObject.optInt("bonus_heart", 0),
                voted = jsonObject.optLong("voted", 0)
            )
        } catch (e: Exception) {
            HeartPickVoteResponse()
        }
    }

    private fun createPart(name: String, value: String): MultipartBody.Part =
        MultipartBody.Part.createFormData(name, value)
}
