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

    /**
     * 게시글 번역
     * Old 프로젝트의 ITranslation.translateArticle과 동일
     *
     * @param articleId 게시글 ID
     * @return 번역된 게시글
     */
    fun translateArticle(articleId: Long): Flow<ApiResult<ArticleModel>>

    /**
     * 공지사항 상세 조회
     *
     * @param noticeId 공지사항 ID
     * @param isDarkMode 다크모드 여부 (서버에서 다크모드용 HTML 반환)
     * @return 공지사항 모델 (contentHtml 포함)
     */
    fun getNotice(noticeId: Int, isDarkMode: Boolean = false): Flow<ApiResult<NoticeModel>>

    /**
     * 게시글 작성 (피드/자유게시판)
     * Old 프로젝트의 createArticle과 동일
     * API: POST articles/create/
     *
     * @param idolId 아이돌 ID
     * @param content 내용
     * @param title 제목
     * @param tagId 태그 ID (기본값: "1" = 자유)
     * @param show 공개 범위 ("A" = 전체공개, "M" = 최애공개)
     * @param files 첨부 파일 목록 (이미지/동영상)
     * @param linkTitle 링크 제목
     * @param linkDesc 링크 설명
     * @param linkUrl 링크 URL
     * @return 작성 결과 (gcode, provide 포함)
     */
    fun createArticle(
        idolId: Int,
        content: String,
        title: String = "",
        tagId: String = "1",
        show: String = "A",
        files: List<FileUploadData> = emptyList(),
        linkTitle: String = "",
        linkDesc: String = "",
        linkUrl: String = ""
    ): Flow<ApiResult<CreateArticleResult>>

    /**
     * 게시글 작성 (덕질게시판/팬톡)
     * Old 프로젝트의 insertArticle과 동일
     * API: POST articles/insert/
     *
     * @param idolId 아이돌 ID
     * @param content 내용
     * @param title 제목
     * @param showScope 공개 범위 ("A" = 전체공개, "M" = 최애공개)
     * @param files 첨부 파일 목록 (이미지/동영상)
     * @param linkTitle 링크 제목
     * @param linkDesc 링크 설명
     * @param linkUrl 링크 URL
     * @return 작성 결과 (gcode, provide 포함)
     */
    fun insertArticle(
        idolId: Int,
        content: String,
        title: String = "",
        showScope: String = "A",
        files: List<FileUploadData> = emptyList(),
        linkTitle: String = "",
        linkDesc: String = "",
        linkUrl: String = ""
    ): Flow<ApiResult<CreateArticleResult>>

    /**
     * 게시글 이미지 처리 완료 확인
     * Old 프로젝트의 checkReady와 동일
     * API: GET articles/check_ready/
     *
     * 이미지가 포함된 게시글 작성 후 호출하여 처리 완료 대기
     *
     * @param articleId 게시글 ID
     * @return 처리 결과 (success, reward 포함)
     */
    suspend fun checkReady(articleId: Long): CheckReadyResult
}

/**
 * 파일 업로드 데이터
 * 이미지/동영상을 S3에 업로드 후 받은 정보
 */
data class FileUploadData(
    val seq: Int,
    val size: Long,
    val savedFilename: String,
    val originName: String
)

/**
 * 게시글 작성 결과
 */
data class CreateArticleResult(
    val gcode: Int,
    val provide: Long = 0,  // 하트 보상
    val articleId: Long? = null
)

/**
 * 이미지 처리 완료 확인 결과
 */
data class CheckReadyResult(
    val success: Boolean,
    val gcode: Int = 0,
    val reward: Int = 0,
    val msg: String? = null
) {
    companion object {
        const val GCODE_UPLOAD_FAILED = 3902  // 업로드 실패
    }
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
