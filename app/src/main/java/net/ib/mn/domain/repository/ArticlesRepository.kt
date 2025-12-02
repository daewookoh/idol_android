package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.dto.ArticleLikeResponse
import net.ib.mn.data.remote.dto.ArticleVoteResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.NoticeModel

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

    /**
     * 덕질게시판 (최애 탭) 게시물 조회
     *
     * @param idolId 최애 아이돌 ID
     * @param orderBy 정렬 기준
     * @param keyword 검색어
     * @param locale 언어 필터
     * @param limit 한 번에 가져올 개수
     */
    fun getMyFavoriteArticles(
        idolId: Int,
        orderBy: String,
        keyword: String? = null,
        locale: String? = null,
        limit: Int = 50
    ): Flow<ApiResult<ArticlesResponse>>

    /**
     * 커뮤니티 피드 게시물 조회
     *
     * @param idolId 아이돌 ID
     * @param isMost 최애 여부
     * @param orderBy 정렬 기준 ("-heart", "-created_at", "-num_comments", "-like_count")
     * @param imageOnly 이미지만 보기 ("Y", "N", null)
     * @param primaryFileType 파일 타입 필터 ("wp" = 배경화면)
     */
    fun getCommunityFeed(
        idolId: Int,
        isMost: Boolean,
        orderBy: String,
        imageOnly: String? = null,
        primaryFileType: String? = null
    ): Flow<ApiResult<ArticlesResponse>>

    /**
     * 커뮤니티 피드 다음 페이지 조회
     *
     * @param nextUrl 다음 페이지 URL
     * @param isMost 최애 여부
     * @param imageOnly 이미지만 보기
     * @param primaryFileType 파일 타입 필터
     */
    fun getCommunityFeedNext(
        nextUrl: String,
        isMost: Boolean,
        imageOnly: String? = null,
        primaryFileType: String? = null
    ): Flow<ApiResult<ArticlesResponse>>

    /**
     * 게시글 투표 (하트 투표)
     *
     * @param articleId 게시글 ID
     * @param hearts 투표할 하트 개수
     * @return 투표 결과
     */
    suspend fun voteArticle(articleId: String, hearts: Long): ArticleVoteResponse

    /**
     * 게시글 좋아요
     *
     * @param articleId 게시글 ID
     * @param like 좋아요 여부 (true: 좋아요, false: 좋아요 취소)
     * @return 좋아요 결과
     */
    suspend fun likeArticle(articleId: String, like: Boolean): ArticleLikeResponse

    /**
     * 유저 피드 활동 조회 (사진/게시글)
     * Old 프로젝트의 FeedActivity.getFeedPhoto() 참고
     *
     * @param userId 유저 ID
     * @param type 타입 ("PHOTO" = 사진, "ALL" = 전체)
     * @param offset 시작 위치
     * @param limit 페이지당 개수
     * @param isSelf 본인 여부
     */
    fun getFeedActivity(
        userId: Int,
        type: String,
        offset: Int,
        limit: Int,
        isSelf: Boolean
    ): Flow<ApiResult<ArticlesResponse>>

    /**
     * 게시글 삭제
     *
     * @param articleId 게시글 ID
     * @return 삭제 결과 (ApiResult<Boolean>)
     */
    fun deleteArticle(articleId: Long): Flow<ApiResult<Boolean>>

    /**
     * 게시글 상세 조회
     *
     * @param articleId 게시글 ID
     * @return 게시글 모델
     */
    suspend fun getArticle(articleId: Long): ArticleModel
}

/**
 * 게시글 목록 응답 모델
 */
data class ArticlesResponse(
    val notices: List<NoticeModel> = emptyList(),  // 공지사항/고정글 (top_notices)
    val articles: List<ArticleModel>,
    val totalCount: Int,
    val nextUrl: String?
)
