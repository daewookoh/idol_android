package net.ib.mn.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ib.mn.data.remote.api.ArticlesApi
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.NoticeModel
import net.ib.mn.domain.repository.ArticlesRepository
import net.ib.mn.domain.repository.ArticlesResponse
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val TAG = "ArticlesRepository"

/**
 * Articles Repository 구현체
 */
class ArticlesRepositoryImpl @Inject constructor(
    private val articlesApi: ArticlesApi,
    private val gson: Gson
) : ArticlesRepository {

    override fun getFreeBoardHot(
        orderBy: String,
        keyword: String?,
        locale: String?,
        limit: Int,
        offset: Int
    ): Flow<ApiResult<ArticlesResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val response = articlesApi.getFreeBoardHot(
                orderBy = orderBy,
                keyword = keyword,
                locale = locale,
                limit = limit,
                offset = offset
            )

            if (response.isSuccessful && response.body() != null) {
                val bodyString = response.body()!!.string()
                val articlesResponse = parseArticlesResponse(bodyString)
                emit(ApiResult.Success(articlesResponse))
            } else {
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    override fun getFreeBoardAll(
        orderBy: String,
        keyword: String?,
        locale: String?,
        limit: Int,
        offset: Int
    ): Flow<ApiResult<ArticlesResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val response = articlesApi.getFreeBoardAll(
                orderBy = orderBy,
                keyword = keyword,
                locale = locale,
                limit = limit,
                offset = offset
            )

            if (response.isSuccessful && response.body() != null) {
                val bodyString = response.body()!!.string()
                val articlesResponse = parseArticlesResponse(bodyString)
                emit(ApiResult.Success(articlesResponse))
            } else {
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    override fun getArticles(
        idolId: Int,
        orderBy: String,
        tags: String?,
        keyword: String?,
        locale: String?,
        isPopular: String?
    ): Flow<ApiResult<ArticlesResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val response = articlesApi.getArticles(
                idolId = idolId,
                orderBy = orderBy,
                tags = tags,
                keyword = keyword,
                locale = locale,
                isPopular = isPopular
            )

            if (response.isSuccessful && response.body() != null) {
                val bodyString = response.body()!!.string()
                val articlesResponse = parseArticlesResponse(bodyString)
                emit(ApiResult.Success(articlesResponse))
            } else {
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    override fun getArticlesNext(nextUrl: String): Flow<ApiResult<ArticlesResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val response = articlesApi.getFreeBoardHotNext(nextUrl)

            if (response.isSuccessful && response.body() != null) {
                val bodyString = response.body()!!.string()
                val articlesResponse = parseArticlesResponse(bodyString)
                emit(ApiResult.Success(articlesResponse))
            } else {
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    override fun getMyFavoriteArticles(
        idolId: Int,
        orderBy: String,
        keyword: String?,
        locale: String?,
        limit: Int
    ): Flow<ApiResult<ArticlesResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            Log.d(TAG, "getMyFavoriteArticles: idolId=$idolId, orderBy=$orderBy, keyword=$keyword, locale=$locale")
            val response = articlesApi.getSmallTalkInventory(
                idolId = idolId,
                isMost = "Y",
                type = "M",
                orderBy = orderBy,
                limit = limit,
                keyword = keyword,
                locale = locale
            )

            if (response.isSuccessful && response.body() != null) {
                val bodyString = response.body()!!.string()
                val articlesResponse = parseArticlesResponse(bodyString)
                emit(ApiResult.Success(articlesResponse))
            } else {
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    /**
     * API 응답 JSON을 ArticlesResponse로 파싱
     */
    private fun parseArticlesResponse(bodyString: String): ArticlesResponse {
        Log.d(TAG, "parseArticlesResponse: bodyString length=${bodyString.length}")
        Log.d(TAG, "parseArticlesResponse: bodyString=${bodyString.take(500)}")

        val jsonObject = JSONObject(bodyString)
        val meta = jsonObject.optJSONObject("meta")
        val objectsArray = jsonObject.optJSONArray("objects")
        val topNoticesArray = jsonObject.optJSONArray("top_notices")

        Log.d(TAG, "parseArticlesResponse: meta=$meta, objectsArray size=${objectsArray?.length()}, topNotices size=${topNoticesArray?.length()}")

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
                Log.d(TAG, "parseArticlesResponse: parsed ${noticeList.size} notices")
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
                val parsed = gson.fromJson<List<ArticleModel>>(objectsArray.toString(), listType)
                Log.d(TAG, "parseArticlesResponse: parsed ${parsed.size} articles")
                parsed
            } catch (e: Exception) {
                Log.e(TAG, "parseArticlesResponse: Failed to parse articles", e)
                emptyList()
            }
        } else {
            Log.d(TAG, "parseArticlesResponse: objectsArray is null or empty")
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
