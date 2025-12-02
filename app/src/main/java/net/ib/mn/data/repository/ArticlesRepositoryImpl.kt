package net.ib.mn.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.ArticlesApi
import net.ib.mn.data.remote.dto.ArticleLikeRequest
import net.ib.mn.data.remote.dto.ArticleLikeResponse
import net.ib.mn.data.remote.dto.ArticleVoteRequest
import net.ib.mn.data.remote.dto.ArticleVoteResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.NoticeModel
import net.ib.mn.domain.repository.ArticlesRepository
import net.ib.mn.domain.repository.ArticlesResponse
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
            Log.d(TAG, "getMyFavoriteArticles: idolId=$idolId, orderBy=$orderBy, keyword=$keyword, locale=$locale")
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
        Log.d(TAG, "voteArticle: articleId=$articleId, hearts=$hearts")
        return articlesApi.voteArticle(
            ArticleVoteRequest(articleId = articleId, hearts = hearts)
        )
    }

    override suspend fun likeArticle(articleId: String, like: Boolean): ArticleLikeResponse {
        Log.d(TAG, "likeArticle: articleId=$articleId, like=$like")
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
            Log.d(TAG, "getFeedActivity: userId=$userId, type=$type, offset=$offset, limit=$limit, isSelf=$isSelf")
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
        Log.d(TAG, "parseArticlesResponse: bodyString length=${bodyString.length}")

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
                Log.e(TAG, "parseArticlesResponse: Failed to parse notices", e)
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
                Log.e(TAG, "parseArticlesResponse: Failed to parse articles", e)
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
}
