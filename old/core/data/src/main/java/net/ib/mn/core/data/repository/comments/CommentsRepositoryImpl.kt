/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.comments

import net.ib.mn.core.data.api.CommentsApi
import net.ib.mn.core.data.dto.WriteCommentDTO
import net.ib.mn.core.data.repository.BaseRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

class CommentsRepositoryImpl @Inject constructor(
    private val commentsApi: CommentsApi
) : CommentsRepository, BaseRepository() {
    val NO_EMOTICON_ID = -100

    override suspend fun writeComment(
        articleId: Long,
        emoticonId: Int,
        content: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val dto = WriteCommentDTO(
                articleId,
                if( emoticonId != NO_EMOTICON_ID) emoticonId else null,
                content
            )
            val response = commentsApi.writeComment(dto)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun writeCommentMultipart(
        articleId: Long,
        emoticonId: Int,
        content: String,
        image: ByteArray?,
        imageUrl: String?,
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
            val partImageUrl =
                if( imageUrl != null)
                    MultipartBody.Part.createFormData("image_url", imageUrl)
                else null
            val response = commentsApi.writeCommentMultipart(
                articleId = MultipartBody.Part.createFormData("article_id", articleId.toString()),
                emoticon = partEmoticon,
                content = MultipartBody.Part.createFormData("content", content),
                imageUrl = partImageUrl,
                image = imagePart
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getCommentsCursor(
        articleId: Long,
        cursor: String?,
        limit: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = commentsApi.getCommentsCursor(articleId, cursor, limit)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getComment(
        commentId: Long,
        translate: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = commentsApi.getComment(commentId, translate)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun checkHash(
        hash: String?,
        idolId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = commentsApi.checkHash(hash, idolId)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun deleteComment(
        resourceUri: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = commentsApi.deleteComment(resourceUri)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}