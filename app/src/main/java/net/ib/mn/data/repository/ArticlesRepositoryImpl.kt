package net.ib.mn.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.ib.mn.data.remote.api.ArticlesApi
import net.ib.mn.data.remote.dto.ArticleLikeRequest
import net.ib.mn.data.remote.dto.ArticleLikeResponse
import net.ib.mn.data.remote.dto.ArticleVoteRequest
import net.ib.mn.data.remote.dto.ArticleVoteResponse
import net.ib.mn.data.remote.dto.CreateArticleRequest
import net.ib.mn.data.remote.dto.FileData
import net.ib.mn.data.remote.dto.InsertArticleRequest
import net.ib.mn.data.remote.dto.UpdateArticleRequest
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.NoticeModel
import net.ib.mn.domain.repository.ArticlesRepository
import net.ib.mn.domain.repository.ArticlesResponse
import net.ib.mn.domain.repository.CheckReadyResult
import net.ib.mn.domain.repository.CreateArticleResult
import net.ib.mn.domain.repository.FileUploadData
import net.ib.mn.domain.repository.UpdateArticleResult
import net.ib.mn.util.logE
import org.json.JSONObject
import javax.inject.Inject

private const val TAG = "ArticlesRepository"

/**
 * ArticlesRepository 구현체
 *
 * BaseRepository의 safeApiCallWithJsonString 패턴 사용
 */
class ArticlesRepositoryImpl @Inject constructor(
    private val articlesApi: ArticlesApi,
    private val gson: Gson
) : BaseRepository(), ArticlesRepository {

    override fun getFreeBoardHot(
        orderBy: String,
        keyword: String?,
        locale: String?,
        limit: Int,
        offset: Int
    ): Flow<ApiResult<ArticlesResponse>> = safeApiCallWithJsonString(
        apiCall = {
            articlesApi.getFreeBoardHot(
                orderBy = orderBy,
                keyword = keyword,
                locale = locale,
                limit = limit,
                offset = offset
            )
        },
        parser = { json -> parseArticlesResponse(json) }
    )

    override fun getFreeBoardAll(
        orderBy: String,
        keyword: String?,
        locale: String?,
        limit: Int,
        offset: Int
    ): Flow<ApiResult<ArticlesResponse>> = safeApiCallWithJsonString(
        apiCall = {
            articlesApi.getFreeBoardAll(
                orderBy = orderBy,
                keyword = keyword,
                locale = locale,
                limit = limit,
                offset = offset
            )
        },
        parser = { json -> parseArticlesResponse(json) }
    )

    override fun getArticles(
        idolId: Int,
        orderBy: String,
        tags: String?,
        keyword: String?,
        locale: String?,
        isPopular: String?
    ): Flow<ApiResult<ArticlesResponse>> = safeApiCallWithJsonString(
        apiCall = {
            articlesApi.getArticles(
                idolId = idolId,
                orderBy = orderBy,
                tags = tags,
                keyword = keyword,
                locale = locale,
                isPopular = isPopular
            )
        },
        parser = { json -> parseArticlesResponse(json) }
    )

    override fun getArticlesNext(nextUrl: String): Flow<ApiResult<ArticlesResponse>> =
        safeApiCallWithJsonString(
            apiCall = { articlesApi.getFreeBoardHotNext(nextUrl) },
            parser = { json -> parseArticlesResponse(json) }
        )

    override fun getMyFavoriteArticles(
        idolId: Int,
        orderBy: String,
        keyword: String?,
        locale: String?,
        limit: Int
    ): Flow<ApiResult<ArticlesResponse>> = safeApiCallWithJsonString(
        apiCall = {
            articlesApi.getSmallTalkInventory(
                idolId = idolId,
                isMost = "Y",
                type = "M",
                orderBy = orderBy,
                limit = limit,
                keyword = keyword,
                locale = locale
            )
        },
        parser = { json -> parseArticlesResponse(json) }
    )

    override fun getCommunityFeed(
        idolId: Int,
        isMost: Boolean,
        orderBy: String,
        imageOnly: String?,
        primaryFileType: String?
    ): Flow<ApiResult<ArticlesResponse>> = safeApiCallWithJsonString(
        apiCall = {
            articlesApi.getArticles(
                idolId = idolId,
                orderBy = orderBy,
                isMost = if (isMost) "Y" else "N",
                primaryFileType = primaryFileType,
                imageOnly = imageOnly
            )
        },
        parser = { json -> parseArticlesResponse(json) }
    )

    override fun getCommunityFeedNext(
        nextUrl: String,
        isMost: Boolean,
        imageOnly: String?,
        primaryFileType: String?
    ): Flow<ApiResult<ArticlesResponse>> = safeApiCallWithJsonString(
        apiCall = {
            articlesApi.getArticlesNext(
                url = nextUrl,
                isMost = if (isMost) "Y" else "N",
                primaryFileType = primaryFileType,
                imageOnly = imageOnly
            )
        },
        parser = { json -> parseArticlesResponse(json) }
    )

    override suspend fun voteArticle(articleId: String, hearts: Long): ArticleVoteResponse {
        return articlesApi.voteArticle(
            ArticleVoteRequest(articleId = articleId, hearts = hearts)
        )
    }

    override suspend fun likeArticle(articleId: String, like: Boolean): ArticleLikeResponse {
        return articlesApi.likeArticle(
            ArticleLikeRequest(articleId = articleId, like = like)
        )
    }

    override fun getFeedActivity(
        userId: Int,
        type: String,
        offset: Int,
        limit: Int,
        isSelf: Boolean
    ): Flow<ApiResult<ArticlesResponse>> = safeApiCallWithJsonString(
        apiCall = {
            articlesApi.getFeedActivity(
                userId = userId,
                type = type,
                limit = limit,
                offset = offset,
                isSelf = if (isSelf) true else null
            )
        },
        parser = { json -> parseFeedActivityResponse(json) }
    )

    // ============================================================
    // Private Parsers
    // ============================================================

    private fun parseFeedActivityResponse(bodyString: String): ArticlesResponse {
        val jsonObject = JSONObject(bodyString)
        val success = jsonObject.optBoolean("success", false)
        val feedIsViewable = jsonObject.optString("feed_is_viewable", "Y")
        val objectsArray = jsonObject.optJSONArray("objects")

        if (!success) {
            return ArticlesResponse(emptyList(), emptyList(), 0, null)
        }

        // 비공개 피드 (feed_is_viewable: "N")
        if (feedIsViewable == "N") {
            return ArticlesResponse(emptyList(), emptyList(), -1, null)
        }

        // 게시글 파싱 (이미지 있는 것만)
        val articles = if (objectsArray != null && objectsArray.length() > 0) {
            try {
                val listType = object : TypeToken<List<ArticleModel>>() {}.type
                gson.fromJson<List<ArticleModel>>(objectsArray.toString(), listType)
                    .filter { !it.imageUrl.isNullOrEmpty() || it.files.isNotEmpty() }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        return ArticlesResponse(emptyList(), articles, articles.size, null)
    }

    private fun parseArticlesResponse(bodyString: String): ArticlesResponse {
        val jsonObject = JSONObject(bodyString)
        val meta = jsonObject.optJSONObject("meta")
        val objectsArray = jsonObject.optJSONArray("objects")
        val topNoticesArray = jsonObject.optJSONArray("top_notices")

        val totalCount = meta?.optInt("total_count", 0) ?: 0
        val nextUrl = meta?.optString("next")?.takeIf { it.isNotEmpty() && it != "null" }

        // 공지사항/고정글 파싱
        val notices = if (topNoticesArray != null && topNoticesArray.length() > 0) {
            try {
                val noticeList = mutableListOf<NoticeModel>()
                for (i in 0 until topNoticesArray.length()) {
                    val noticeJson = topNoticesArray.getJSONObject(i)
                    val notice = NoticeModel(
                        id = noticeJson.optString("id", ""),
                        title = noticeJson.optString("title", ""),
                        content = noticeJson.optString("content", ""),
                        contentHtml = noticeJson.optString("content_html", ""),
                        createdAt = noticeJson.optLong("created_at", 0L),
                        isOpen = noticeJson.optBoolean("is_open", false)
                    )
                    noticeList.add(notice)
                }
                noticeList
            } catch (e: Exception) {
                logE(TAG, "parseArticlesResponse: Failed to parse notices", e)
                emptyList()
            }
        } else {
            emptyList()
        }

        // 게시글 파싱
        val articles = if (objectsArray != null && objectsArray.length() > 0) {
            try {
                val listType = object : TypeToken<List<ArticleModel>>() {}.type
                gson.fromJson<List<ArticleModel>>(objectsArray.toString(), listType)
            } catch (e: Exception) {
                logE(TAG, "parseArticlesResponse: Failed to parse articles", e)
                emptyList()
            }
        } else {
            emptyList()
        }

        return ArticlesResponse(
            notices = notices,
            articles = articles,
            totalCount = totalCount,
            nextUrl = nextUrl
        )
    }

    /**
     * 게시글 삭제
     */
    override fun deleteArticle(articleId: Long): Flow<ApiResult<Boolean>> =
        safeApiCallWithJsonString(
            apiCall = { articlesApi.deleteArticle(articleId) },
            parser = { json ->
                val jsonObject = JSONObject(json)
                jsonObject.optBoolean("success", false)
            }
        )

    /**
     * 게시글 상세 조회
     */
    override suspend fun getArticle(articleId: Long): ArticleModel {
        val response = articlesApi.getArticle(articleId)
        val json = response.body()?.string() ?: throw Exception("Response body is null")
        val jsonObject = JSONObject(json)
        val articleJson = jsonObject.optJSONObject("article") ?: jsonObject
        return gson.fromJson(articleJson.toString(), ArticleModel::class.java)
    }

    /**
     * 게시글 번역
     * Old 프로젝트의 ITranslation.translateArticle과 동일
     *
     * @param articleId 게시글 ID
     * @return 번역된 게시글
     */
    override fun translateArticle(articleId: Long): Flow<ApiResult<ArticleModel>> =
        safeApiCallWithJsonString(
            apiCall = { articlesApi.getArticle(articleId, translate = "auto") },
            parser = { json ->
                val jsonObject = JSONObject(json)
                val articleJson = jsonObject.optJSONObject("article") ?: jsonObject
                gson.fromJson(articleJson.toString(), ArticleModel::class.java)
            }
        )

    /**
     * 공지사항 상세 조회
     */
    override fun getNotice(noticeId: Int, isDarkMode: Boolean): Flow<ApiResult<NoticeModel>> =
        safeApiCallWithJsonString(
            apiCall = {
                val mode = if (isDarkMode) "dark" else null
                articlesApi.getNotice(noticeId, mode)
            },
            parser = { json -> parseNoticeResponse(json) }
        )

    private fun parseNoticeResponse(bodyString: String): NoticeModel {
        val jsonObject = JSONObject(bodyString)
        if (!jsonObject.optBoolean("success", false)) {
            throw Exception(jsonObject.optString("msg", "Failed to get notice"))
        }
        val obj = jsonObject.optJSONObject("object")
            ?: throw Exception("No object in response")
        return NoticeModel(
            id = obj.optString("id", ""),
            title = obj.optString("title", ""),
            content = obj.optString("content", ""),
            contentHtml = obj.optString("content_html", ""),
            createdAt = obj.optLong("created_at", 0L),
            isOpen = obj.optBoolean("is_open", false)
        )
    }

    /**
     * 게시글 작성
     * Old 프로젝트의 createArticle과 동일
     */
    override fun createArticle(
        idolId: Int,
        content: String,
        title: String,
        tagId: String,
        show: String,
        files: List<FileUploadData>,
        linkTitle: String,
        linkDesc: String,
        linkUrl: String
    ): Flow<ApiResult<CreateArticleResult>> = flow {
        emit(ApiResult.Loading)
        try {
            val request = CreateArticleRequest(
                title = title,
                content = content,
                idolId = idolId.toString(),
                linkTitle = linkTitle,
                linkDesc = linkDesc,
                linkUrl = linkUrl,
                show = show,
                files = files.map { FileData(seq = it.seq, size = it.size, savedFilename = it.savedFilename, originName = it.originName) },
                tagId = tagId
            )
            val response = articlesApi.createArticle(request)
            if (response.success) {
                emit(ApiResult.Success(CreateArticleResult(
                    gcode = response.gcode,
                    provide = response.provide,
                    articleId = response.articleId
                )))
            } else {
                emit(ApiResult.Error.create(
                    exception = Exception(response.msg ?: "게시글 작성에 실패했습니다."),
                    message = response.msg ?: "게시글 작성에 실패했습니다."
                ))
            }
        } catch (e: Exception) {
            logE(TAG, "createArticle", e)
            emit(ApiResult.Error.create(
                exception = e,
                message = e.message ?: "게시글 작성에 실패했습니다."
            ))
        }
    }.catch { e ->
        emit(ApiResult.Error.create(
            exception = e as? Exception ?: Exception(e.message),
            message = e.message ?: "게시글 작성에 실패했습니다."
        ))
    }

    /**
     * 게시글 작성 (덕질게시판/팬톡)
     * Old 프로젝트의 insertArticle과 동일
     * API: POST articles/insert/
     */
    override fun insertArticle(
        idolId: Int,
        content: String,
        title: String,
        showScope: String,
        files: List<FileUploadData>,
        linkTitle: String,
        linkDesc: String,
        linkUrl: String
    ): Flow<ApiResult<CreateArticleResult>> = flow {
        emit(ApiResult.Loading)
        try {
            val request = InsertArticleRequest(
                title = title,
                content = content,
                idolId = idolId.toString(),
                linkTitle = linkTitle,
                linkDesc = linkDesc,
                linkUrl = linkUrl,
                showScope = showScope,
                files = files.map { FileData(seq = it.seq, size = it.size, savedFilename = it.savedFilename, originName = it.originName) }
            )
            val response = articlesApi.insertArticle(request)
            if (response.success) {
                emit(ApiResult.Success(CreateArticleResult(
                    gcode = response.gcode,
                    provide = response.provide,
                    articleId = response.articleId
                )))
            } else {
                emit(ApiResult.Error.create(
                    exception = Exception(response.msg ?: "게시글 작성에 실패했습니다."),
                    message = response.msg ?: "게시글 작성에 실패했습니다."
                ))
            }
        } catch (e: Exception) {
            logE(TAG, "insertArticle", e)
            emit(ApiResult.Error.create(
                exception = e,
                message = e.message ?: "게시글 작성에 실패했습니다."
            ))
        }
    }.catch { e ->
        emit(ApiResult.Error.create(
            exception = e as? Exception ?: Exception(e.message),
            message = e.message ?: "게시글 작성에 실패했습니다."
        ))
    }

    /**
     * 게시글 이미지 처리 완료 확인
     */
    override suspend fun checkReady(articleId: Long): CheckReadyResult {
        return try {
            val response = articlesApi.checkReady(articleId)
            val bodyString = response.body()?.string()

            if (bodyString.isNullOrEmpty()) {
                return CheckReadyResult(success = false)
            }

            val jsonObject = JSONObject(bodyString)
            CheckReadyResult(
                success = jsonObject.optBoolean("success", false),
                gcode = jsonObject.optInt("gcode", 0),
                reward = jsonObject.optInt("reward", 0),
                msg = jsonObject.optString("msg", null)
            )
        } catch (e: Exception) {
            logE(TAG, "checkReady", e)
            CheckReadyResult(success = false)
        }
    }

    /**
     * 게시글 수정
     * Old 프로젝트의 updateArticle과 동일
     */
    override fun updateArticle(
        articleId: String,
        content: String,
        title: String?,
        show: String,
        tagId: String?,
        linkTitle: String?,
        linkDesc: String?,
        linkUrl: String?
    ): Flow<ApiResult<UpdateArticleResult>> = flow {
        emit(ApiResult.Loading)
        try {
            val request = UpdateArticleRequest(
                articleId = articleId,
                content = content,
                title = title,
                linkTitle = linkTitle,
                linkDesc = linkDesc,
                linkUrl = linkUrl,
                show = show,
                tagId = tagId
            )
            val response = articlesApi.updateArticle(request)
            if (response.success) {
                emit(ApiResult.Success(UpdateArticleResult(
                    gcode = response.gcode,
                    success = response.success,
                    provide = response.provide,
                    msg = response.msg
                )))
            } else {
                emit(ApiResult.Error.create(
                    exception = Exception(response.msg ?: "게시글 수정에 실패했습니다."),
                    message = response.msg ?: "게시글 수정에 실패했습니다."
                ))
            }
        } catch (e: Exception) {
            logE(TAG, "updateArticle", e)
            emit(ApiResult.Error.create(
                exception = e,
                message = e.message ?: "게시글 수정에 실패했습니다."
            ))
        }
    }.catch { e ->
        emit(ApiResult.Error.create(
            exception = e as? Exception ?: Exception(e.message),
            message = e.message ?: "게시글 수정에 실패했습니다."
        ))
    }
}
