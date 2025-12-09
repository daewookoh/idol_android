package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ib.mn.data.remote.api.SearchApi
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.SearchResultModel
import net.ib.mn.domain.model.SearchSuggestModel
import net.ib.mn.domain.model.SearchTrendModel
import net.ib.mn.domain.model.toDomain
import net.ib.mn.domain.repository.SearchRepository
import javax.inject.Inject

/**
 * SearchRepository 구현체
 *
 * 검색 API 호출 및 응답 변환 처리
 */
class SearchRepositoryImpl @Inject constructor(
    private val searchApi: SearchApi
) : SearchRepository, BaseRepository() {

    override fun search(
        keyword: String,
        category: String?,
        offset: Int,
        limit: Int
    ): Flow<ApiResult<SearchResultModel>> {
        return safeApiCall {
            searchApi.search(
                keyword = keyword,
                category = category,
                offset = offset,
                limit = limit
            )
        }.map { result ->
            when (result) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.success) {
                        ApiResult.Success(
                            SearchResultModel(
                                idols = response.idols?.map { it.toDomain() } ?: emptyList(),
                                supports = response.supports?.map { it.toDomain() } ?: emptyList(),
                                wallpapers = response.wallpapers?.map { it.toDomain() } ?: emptyList(),
                                articles = response.articles ?: emptyList(),
                                smallTalks = response.articlev2s ?: emptyList(),
                                smallTalkOffset = response.articlev2Offset,
                                smallTalkTotal = response.articlev2Total,
                                smallTalkLimit = response.articlev2Limit
                            )
                        )
                    } else {
                        ApiResult.Error(
                            net.ib.mn.domain.model.ApiError.Business(
                                gcode = response.gcode ?: 0,
                                message = response.msg ?: "Search failed"
                            )
                        )
                    }
                }
                is ApiResult.Error -> result
                is ApiResult.Loading -> ApiResult.Loading
            }
        }
    }

    override fun getTrends(): Flow<ApiResult<List<SearchTrendModel>>> {
        return safeApiCall {
            searchApi.getTrend()
        }.map { result ->
            when (result) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.success) {
                        ApiResult.Success(
                            response.objects?.mapIndexed { index, dto ->
                                dto.toDomain().copy(rank = index + 1)
                            } ?: emptyList()
                        )
                    } else {
                        ApiResult.Error(
                            net.ib.mn.domain.model.ApiError.Business(
                                gcode = response.gcode ?: 0,
                                message = response.msg ?: "Failed to get trends"
                            )
                        )
                    }
                }
                is ApiResult.Error -> result
                is ApiResult.Loading -> ApiResult.Loading
            }
        }
    }

    override fun getSuggestions(query: String): Flow<ApiResult<List<SearchSuggestModel>>> {
        return safeApiCall {
            searchApi.getSuggest(query)
        }.map { result ->
            when (result) {
                is ApiResult.Success -> {
                    val response = result.data
                    if (response.success) {
                        ApiResult.Success(
                            response.objects?.map { it.toDomain() } ?: emptyList()
                        )
                    } else {
                        ApiResult.Error(
                            net.ib.mn.domain.model.ApiError.Business(
                                gcode = response.gcode ?: 0,
                                message = response.msg ?: "Failed to get suggestions"
                            )
                        )
                    }
                }
                is ApiResult.Error -> result
                is ApiResult.Loading -> ApiResult.Loading
            }
        }
    }
}
