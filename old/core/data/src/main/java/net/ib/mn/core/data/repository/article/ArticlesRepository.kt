/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.article

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.data.dto.CreateArticleDTO
import net.ib.mn.core.data.dto.InsertArticleDTO
import net.ib.mn.core.data.dto.UpdateArticleDTO
import net.ib.mn.core.data.model.ArticleCheckReadyResponse
import net.ib.mn.core.data.model.ArticleLikeModel
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.data.model.GiveHeartModel
import net.ib.mn.core.data.model.GenericArticleResponse
import net.ib.mn.core.data.model.ObjectsBaseDataModel
import org.json.JSONObject


/**
 * @see
 * */

interface ArticlesRepository {
    suspend fun deleteArticle(id: Long): Flow<BaseModel<String>>
    suspend fun getArticles(
        idolId: Int,
        isMost: Boolean,
        orderBy: String,
        keyword: String? = null,
        tags: String? = null,
        primaryFileType: String? = null,
        imageOnly: String? = null,
        isPopular: Boolean? = null,
        locale: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getArticles(
        url: String,
        isMost: Boolean,
        keyword: String? = null,
        tags: String? = null,
        primaryFileType: String? = null,
        imageOnly: String? = null,
        isPopular: Boolean? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getSmallTalkInventory(
        idolId: Int,
        isMost: Boolean,
        type: String,
        orderBy: String,
        limit: Int,
        keyword: String? = null,
        locale: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun postArticleLike(
        articleId: String,
        like: Boolean
    ): Flow<ArticleLikeModel>
    suspend fun postArticleGiveHeart(
        articleId: String,
        hearts: Long,
        listener: (GiveHeartModel) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun createArticle(
        article: CreateArticleDTO
    ): Flow<GenericArticleResponse>
    suspend fun insertArticle(
        article: InsertArticleDTO
    ): Flow<GenericArticleResponse>
    suspend fun updateArticle(
        article: UpdateArticleDTO,
        listener: (GenericArticleResponse) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun checkReady(
        articleId: Long
    ): Flow<ArticleCheckReadyResponse>
    suspend fun downloadArticle(
        articleId: Long,
        seq: Int
    )
    suspend fun viewCount(
        articleId: Long
    )
    suspend fun getFeedActivity(
        userId: Long,
        type: String,
        offset: Int,
        limit: Int,
        isSelf: Boolean,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getArticle(
        articleId: Long,
        translate: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getFreeBoardHot(
        orderBy: String,
        keyword: String?,
        locale: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getFreeBoardHot(
        url: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getArticle(
        url: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getFreeBoardAll(
        orderBy: String,
        keyword: String?,
        locale: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getFreeBoardAll(
        url: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}