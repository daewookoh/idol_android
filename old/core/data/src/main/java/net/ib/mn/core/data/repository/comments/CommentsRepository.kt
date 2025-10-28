/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.comments

import org.json.JSONObject


/**
 * @see
 * */

interface CommentsRepository {

    suspend fun writeComment(
        articleId: Long,
        emoticonId: Int,
        content: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun writeCommentMultipart(
        articleId: Long,
        emoticonId: Int,
        content: String,
        image: ByteArray? = null,
        imageUrl: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getCommentsCursor(
        articleId: Long,
        cursor: String? = null,
        limit: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getComment(
        commentId: Long,
        translate: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun checkHash(
        hash: String?,
        idolId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun deleteComment(
        resourceUri: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}