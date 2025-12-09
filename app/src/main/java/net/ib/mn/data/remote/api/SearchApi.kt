package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.SearchResponse
import net.ib.mn.data.remote.dto.SearchSuggestResponse
import net.ib.mn.data.remote.dto.SearchTrendResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 검색 API
 *
 * old 프로젝트의 SearchApi 기반
 * - search/: 통합 검색 (아이돌, 서포트, 배경화면, 게시글 등)
 * - search/trend/: 핫 트렌드 (인기 검색어)
 * - search/suggest/: 자동완성 추천
 */
interface SearchApi {

    /**
     * 통합 검색
     *
     * @param keyword 검색 키워드
     * @param category 카테고리 필터 (null이면 전체, "article"이면 게시글만)
     * @param offset 페이징 오프셋
     * @param limit 페이징 리밋
     * @return SearchResponse with idols, supports, wallpapers, articles, articlev2s
     */
    @GET("search/")
    suspend fun search(
        @Query("q") keyword: String?,
        @Query("category") category: String? = null,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<SearchResponse>

    /**
     * 핫 트렌드 (인기 검색어)
     *
     * @return SearchTrendResponse with trend keywords
     */
    @GET("search/trend/")
    suspend fun getTrend(): Response<SearchTrendResponse>

    /**
     * 자동완성 추천
     *
     * @param query 검색어
     * @return SearchSuggestResponse with suggest keywords
     */
    @GET("search/suggest/")
    suspend fun getSuggest(
        @Query("q") query: String
    ): Response<SearchSuggestResponse>
}
