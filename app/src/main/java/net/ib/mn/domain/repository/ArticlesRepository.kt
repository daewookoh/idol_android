package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel

/**
 * Articles Repository 인터페이스
 *
 * 자유게시판 게시글 관련 API 호출
 */
interface ArticlesRepository {

    /**
     * 자게 인기글 (HOT) 조회
     *
     * @param orderBy 정렬 기준 (예: "-created_at", "-num_comments", "-like_count", "-view_count")
     * @param keyword 검색어
     * @param locale 언어 필터
     * @param limit 한 번에 가져올 개수
     * @param offset 시작 위치
     */
    fun getFreeBoardHot(
        orderBy: String,
        keyword: String? = null,
        locale: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Flow<ApiResult<ArticlesResponse>>

    /**
     * 자게 모든 게시물 (ALL) 조회
     *
     * @param orderBy 정렬 기준
     * @param keyword 검색어
     * @param locale 언어 필터
     * @param limit 한 번에 가져올 개수
     * @param offset 시작 위치
     */
    fun getFreeBoardAll(
        orderBy: String,
        keyword: String? = null,
        locale: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Flow<ApiResult<ArticlesResponse>>

    /**
     * 태그별 게시물 조회
     *
     * @param idolId 아이돌 ID (자게의 경우 고정값 사용)
     * @param orderBy 정렬 기준
     * @param tags 태그 ID (쉼표로 구분)
     * @param keyword 검색어
     * @param locale 언어 필터
     * @param isPopular 인기글 필터
     */
    fun getArticles(
        idolId: Int,
        orderBy: String,
        tags: String? = null,
        keyword: String? = null,
        locale: String? = null,
        isPopular: String? = null
    ): Flow<ApiResult<ArticlesResponse>>

    /**
     * 다음 페이지 게시물 조회 (페이징)
     *
     * @param nextUrl 다음 페이지 URL
     */
    fun getArticlesNext(nextUrl: String): Flow<ApiResult<ArticlesResponse>>
}

/**
 * 게시글 목록 응답 모델
 */
data class ArticlesResponse(
    val articles: List<ArticleModel>,
    val totalCount: Int,
    val nextUrl: String?
)
