/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.article

import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.api.ArticlesApi
import net.ib.mn.core.data.dto.CreateArticleDTO
import net.ib.mn.core.data.dto.DownloadArticleDTO
import net.ib.mn.core.data.dto.GiveHeartToArticleDTO
import net.ib.mn.core.data.dto.InsertArticleDTO
import net.ib.mn.core.data.dto.LikeArticleDTO
import net.ib.mn.core.data.dto.UpdateArticleDTO
import net.ib.mn.core.data.dto.ViewCountDTO
import net.ib.mn.core.data.model.ArticleCheckReadyResponse
import net.ib.mn.core.data.model.ArticleLikeModel
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.data.model.GiveHeartModel
import net.ib.mn.core.data.model.GenericArticleResponse
import net.ib.mn.core.data.repository.BaseRepository
import org.json.JSONObject


/**
 * @see
 * */

class ArticlesRepositoryImpl @Inject constructor(
    private val articlesApi: ArticlesApi
) : ArticlesRepository, BaseRepository() {
    override suspend fun deleteArticle(id: Long): Flow<BaseModel<String>> = flow {
        try {
            val result = articlesApi.deleteArticle(id)
            when {
                result.success -> emit(BaseModel(data = result.msg, success = true))
                else -> emit(BaseModel(message = result.msg))
            }
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }

    override suspend fun getArticles(
        idolId: Int,
        isMost: Boolean,
        orderBy: String,
        keyword: String?,
        tags: String?,
        primaryFileType: String?,
        imageOnly: String?,
        isPopular: Boolean?,
        locale: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        val isMostValue = if (isMost) "Y" else "N"
        val popular = if (isPopular == true) "Y" else null

        try {
            val response = articlesApi.getArticles(
                idolId = idolId,
                orderBy = orderBy,
                isMost = isMostValue,
                keyword = keyword,
                tags = tags,
                primaryFileType = primaryFileType,
                imageOnly = imageOnly,
                isPopular = popular,
                locale = locale
            )

            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getArticles(
        url: String,
        isMost: Boolean,
        keyword: String?,
        tags: String?,
        primaryFileType: String?,
        imageOnly: String?,
        isPopular: Boolean?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        val isMostValue = if (isMost) "Y" else "N"
        val popular = if (isPopular == true) "Y" else null

        try {
            val response = articlesApi.getArticles(
                url = url,
                isMost = isMostValue,
                keyword = keyword,
                tags = tags,
                primaryFileType = primaryFileType,
                imageOnly = imageOnly,
                isPopular = popular,
            )

            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getSmallTalkInventory(
        idolId: Int,
        isMost: Boolean,
        type: String,
        orderBy: String,
        limit: Int,
        keyword: String?,
        locale: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        val isMostValue = if (isMost) "Y" else "N"

        try {
            val response = articlesApi.getSmallTalkInventory(
                idolId = idolId,
                isMost = isMostValue,
                type = type,
                orderBy = orderBy,
                limit = limit,
                keyword = keyword,
                locale = locale
            )
            processResponse(response, listener, errorListener)

        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun postArticleLike(
        articleId: String,
        like: Boolean
    ) : Flow<ArticleLikeModel> = flow {
        try {
            val body = LikeArticleDTO(articleId, like)
            val response = articlesApi.postLikeArticle(body)

            emit(response)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }.catch { e ->
        emit(ArticleLikeModel(success = false, message = e.message))
    }

    override suspend fun postArticleGiveHeart(
        articleId: String,
        hearts: Long,
        listener: (GiveHeartModel) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val body = GiveHeartToArticleDTO(articleId, hearts)
            val response = articlesApi.postArticleGiveHeart(body)
            listener(response)
        } catch (e: Exception) {
            e.printStackTrace()
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun createArticle(
        article: CreateArticleDTO
    ) : Flow<GenericArticleResponse> = flow {
        try {
            val response = articlesApi.createArticle(article)
            emit(response)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }.catch { e ->
        emit(GenericArticleResponse(success = false, msg = e.message))
    }

    override suspend fun insertArticle(
        article: InsertArticleDTO
    ) : Flow<GenericArticleResponse> = flow {
        try {
            val response = articlesApi.insertArticle(article)
            emit(response)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }.catch { e ->
        emit(GenericArticleResponse(success = false, msg = e.message))
    }

    override suspend fun updateArticle(
        article: UpdateArticleDTO,
        listener: (GenericArticleResponse) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = articlesApi.updateArticle(article)
            listener(response)
        } catch (e: Exception) {
            e.printStackTrace()
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun checkReady(
        articleId: Long
    ) : Flow<ArticleCheckReadyResponse> = flow {
        try {
            val response = articlesApi.checkReady(articleId)
            emit(response)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }.catch { e ->
        emit(ArticleCheckReadyResponse(success = false, msg = e.message))
    }

    override suspend fun downloadArticle(articleId: Long, seq: Int) {
        // download article은 응답을 사용하지 않음
        try {
            val dto = DownloadArticleDTO(articleId, seq)
            articlesApi.downloadArticle(dto)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun viewCount(articleId: Long) {
        try {
            val body = ViewCountDTO(articleId)
            articlesApi.postViewCount(body)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun getFeedActivity(
        userId: Long,
        type: String,
        offset: Int,
        limit: Int,
        isSelf: Boolean,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = articlesApi.getFeedActivity(userId, type, limit, offset, if(isSelf) true else null)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getArticle(
        id: Long,
        translate: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = articlesApi.getArticle(id, translate)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getFreeBoardHot(
        orderBy: String,
        keyword: String?,
        locale: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = articlesApi.getFreeBoardHot(
                orderBy = orderBy,
                keyword = keyword,
                locale = locale,
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getFreeBoardHot(
        url: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = articlesApi.getFreeBoardHot(
                url = url,
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getArticle(
        url: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = articlesApi.getArticle(url)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getFreeBoardAll(
        orderBy: String,
        keyword: String?,
        locale: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = articlesApi.getFreeBoardAll(
                orderBy = orderBy,
                keyword = keyword,
                locale = locale,
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getFreeBoardAll(
        url: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = articlesApi.getFreeBoardAll(
                url = url,
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}