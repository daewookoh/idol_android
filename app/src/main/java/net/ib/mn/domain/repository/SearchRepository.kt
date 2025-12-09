package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.SearchResultModel
import net.ib.mn.domain.model.SearchSuggestModel
import net.ib.mn.domain.model.SearchTrendModel

/**
 * Search Repository 인터페이스
 *
 * 검색 관련 API 호출
 * - 통합 검색 (아이돌, 서포트, 배경화면, 게시글 등)
 * - 핫 트렌드 (인기 검색어)
 * - 자동완성 추천
 */
interface SearchRepository {

    /**
     * 통합 검색
     *
     * @param keyword 검색 키워드
     * @param category 카테고리 필터 (null이면 전체, "article"이면 게시글만)
     * @param offset 페이징 오프셋
     * @param limit 페이징 리밋
     * @return SearchResultModel with idols, supports, wallpapers, articles, smallTalks
     */
    fun search(
        keyword: String,
        category: String? = null,
        offset: Int = 0,
        limit: Int = 50
    ): Flow<ApiResult<SearchResultModel>>

    /**
     * 핫 트렌드 (인기 검색어)
     *
     * @return List<SearchTrendModel> with trend keywords
     */
    fun getTrends(): Flow<ApiResult<List<SearchTrendModel>>>

    /**
     * 자동완성 추천
     *
     * @param query 검색어
     * @return List<SearchSuggestModel> with suggest keywords
     */
    fun getSuggestions(query: String): Flow<ApiResult<List<SearchSuggestModel>>>
}
