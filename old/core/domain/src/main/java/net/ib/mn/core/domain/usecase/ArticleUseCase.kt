/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author parkboo parkboo@myloveidol.com
 * Description:
 *
 * */

package net.ib.mn.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.data.dto.CreateArticleDTO
import net.ib.mn.core.data.dto.InsertArticleDTO
import net.ib.mn.core.data.model.ArticleCheckReadyResponse
import net.ib.mn.core.data.model.ArticleLikeModel
import net.ib.mn.core.data.repository.article.ArticlesRepository
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.data.model.GiveHeartModel
import net.ib.mn.core.data.model.GenericArticleResponse
import javax.inject.Inject


/**
 * @see
 * */

class LikeArticleUseCase @Inject constructor(
    private val repository: ArticlesRepository
) {
    suspend operator fun invoke(articleId: String, like: Boolean): Flow<ArticleLikeModel> =
        repository.postArticleLike(articleId, like)

}

class DeleteArticleUseCase @Inject constructor(
    private val repository: ArticlesRepository
) {
    suspend operator fun invoke(articleId: Long): Flow<BaseModel<String>> =
        repository.deleteArticle(articleId)

}

class GiveHeartToArticleUseCase @Inject constructor(
    private val repository: ArticlesRepository
) {
    suspend operator fun invoke(articleId: String,
                                hearts: Long,
                                listener: (GiveHeartModel) -> Unit,
                                errorListener: (Throwable) -> Unit) =
        repository.postArticleGiveHeart(articleId, hearts, listener, errorListener)

}

class CreateArticleUseCase @Inject constructor(
    private val repository: ArticlesRepository
) {
    suspend operator fun invoke(article: CreateArticleDTO): Flow<GenericArticleResponse> =
        repository.createArticle(article)

}

class InsertArticleUseCase @Inject constructor(
    private val repository: ArticlesRepository
) {
    suspend operator fun invoke(article: InsertArticleDTO): Flow<GenericArticleResponse> =
        repository.insertArticle(article)

}

class CheckReadyUseCase @Inject constructor(
    private val repository: ArticlesRepository
) {
    suspend operator fun invoke(articleId: Long): Flow<ArticleCheckReadyResponse> =
        repository.checkReady(articleId)

}
