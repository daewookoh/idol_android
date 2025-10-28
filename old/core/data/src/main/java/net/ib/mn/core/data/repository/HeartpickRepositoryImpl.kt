/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.api.HeartpickApi
import net.ib.mn.core.data.dto.HeartpickVoteDTO
import net.ib.mn.core.data.dto.OpenNotificationDTO
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

class HeartpickRepositoryImpl @Inject constructor(
    private val heartpickApi: HeartpickApi
) : HeartpickRepository, BaseRepository() {
    val NO_EMOTICON_ID = -100

    override suspend fun get(
        id: Int,
        offset: Int,
        limit: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            if(id == 0) {
                val response = heartpickApi.get(offset, limit)
                processResponse(response, listener, errorListener)
            } else {
                val response = heartpickApi.get(id, offset, limit)
                processResponse(response, listener, errorListener)
            }
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun vote(
        id: Int,
        idolId: Int,
        num: Long,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = heartpickApi.vote(HeartpickVoteDTO(id, idolId, num))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getReplies(
        id: Int,
        limit: Int,
        cursor: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = heartpickApi.getReplies(id, limit, cursor)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getReply(
        id: Int,
        translate: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = heartpickApi.getReply(id, translate)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun postReplyMultipart(
        heartPickId: Int,
        emoticonId: Int?,
        content: String,
        imageUrl: String?,
        image: ByteArray?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            var imagePart: MultipartBody.Part? = null
            image?.let {
                val requestFile = image.toRequestBody("image/jpeg".toMediaTypeOrNull())
                imagePart = MultipartBody.Part.createFormData("imagebin", "image.jpg", requestFile)
            }
            val partEmoticon =
                if( emoticonId != NO_EMOTICON_ID)
                    MultipartBody.Part.createFormData("emoticon", emoticonId.toString())
                else null

            val response = heartpickApi.postReplyMultipart(
                id = MultipartBody.Part.createFormData("id", heartPickId.toString()),
                emoticon = partEmoticon,
                content = MultipartBody.Part.createFormData("content", content),
                image_url = imageUrl?.let { MultipartBody.Part.createFormData("image_url", it) },
                image = imagePart
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun postOpenHeartPickNotification(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = heartpickApi.postOpenHeartPickNotification(OpenNotificationDTO(id = id))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getOpenHeartPickNotification(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = heartpickApi.getOpenHeartPickNotification(id)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }
}
