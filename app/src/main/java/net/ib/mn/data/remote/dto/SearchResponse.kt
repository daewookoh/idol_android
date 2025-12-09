package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName
import net.ib.mn.domain.model.ArticleModel

/**
 * 검색 API Response 모델들
 *
 * old 프로젝트의 SearchResultActivity 분석 기반
 * - search/: 통합 검색 결과
 * - search/trend/: 핫 트렌드 (인기 검색어)
 * - search/suggest/: 자동완성 추천
 */

// ============================================================
// /search/ - 통합 검색
// ============================================================
data class SearchResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("idols")
    val idols: List<SearchIdolDto>? = null,

    @SerializedName("supports")
    val supports: List<SearchSupportDto>? = null,

    @SerializedName("wallpapers")
    val wallpapers: List<SearchWallpaperDto>? = null,

    @SerializedName("articles")
    val articles: List<ArticleModel>? = null,

    @SerializedName("articlev2s")
    val articlev2s: List<ArticleModel>? = null,

    @SerializedName("articlev2_offset")
    val articlev2Offset: Int = 0,

    @SerializedName("articlev2_total")
    val articlev2Total: Int = 0,

    @SerializedName("articlev2_limit")
    val articlev2Limit: Int = 10,

    @SerializedName("gcode")
    val gcode: Int? = null,

    @SerializedName("msg")
    val msg: String? = null
)

/**
 * 검색된 아이돌 정보
 * old 프로젝트의 SearchResultActivity.mSearchedIdolList 구조 기반
 */
data class SearchIdolDto(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("name_en")
    val nameEn: String? = null,

    @SerializedName("name_jp")
    val nameJp: String? = null,

    @SerializedName("name_zh")
    val nameZh: String? = null,

    @SerializedName("name_zh_tw")
    val nameZhTw: String? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("category")
    val category: String? = null,

    @SerializedName("group_id")
    val groupId: Int? = null,

    @SerializedName("group_name")
    val groupName: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("image_url2")
    val imageUrl2: String? = null,

    @SerializedName("heart")
    val heart: Long = 0,

    @SerializedName("resource_uri")
    val resourceUri: String? = null,

    // 뱃지 관련
    @SerializedName("birthday")
    val birthday: String? = null,

    @SerializedName("is_lunar_birthday")
    val isLunarBirthday: String? = null,

    @SerializedName("debut_day")
    val debutDay: String? = null,

    @SerializedName("comeback_day")
    val comebackDay: String? = null,

    @SerializedName("burning_day")
    val burningDay: String? = null,

    // 기부 뱃지 카운트
    @SerializedName("angel_count")
    val angelCount: Int = 0,

    @SerializedName("fairy_count")
    val fairyCount: Int = 0,

    @SerializedName("miracle_count")
    val miracleCount: Int = 0,

    @SerializedName("rookie_count")
    val rookieCount: Int = 0,

    // 팬덤명
    @SerializedName("fd_name")
    val fdName: String? = null,

    @SerializedName("fd_name_en")
    val fdNameEn: String? = null,

    // 차트 코드
    @SerializedName("chart_codes")
    val chartCodes: List<String>? = null
)

/**
 * 검색된 서포트 정보
 * old 프로젝트의 SupportInfoModel 구조 기반
 */
data class SearchSupportDto(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("title")
    val title: String = "",

    @SerializedName("content")
    val content: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @SerializedName("goal_heart")
    val goalHeart: Long = 0,

    @SerializedName("current_heart")
    val currentHeart: Long = 0,

    @SerializedName("status")
    val status: String? = null, // "진행중", "성공" 등

    @SerializedName("start_date")
    val startDate: String? = null,

    @SerializedName("end_date")
    val endDate: String? = null,

    @SerializedName("idol")
    val idol: MostIdol? = null,

    @SerializedName("user")
    val user: SearchUserDto? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
) {
    /**
     * 달성률 계산 (0.0 ~ 1.0)
     */
    val progressRatio: Float
        get() = if (goalHeart > 0) {
            (currentHeart.toFloat() / goalHeart).coerceIn(0f, 1f)
        } else 0f

    /**
     * 달성률 퍼센트 (0 ~ 100)
     */
    val progressPercent: Int
        get() = (progressRatio * 100).toInt()
}

/**
 * 검색된 배경화면 정보
 */
data class SearchWallpaperDto(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @SerializedName("idol")
    val idol: MostIdol? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)

/**
 * 검색 결과 내 사용자 정보
 */
data class SearchUserDto(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("nickname")
    val nickname: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("level")
    val level: Int = 0
)

// ============================================================
// /search/trend/ - 핫 트렌드 (인기 검색어)
// ============================================================
data class SearchTrendResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("objects")
    val objects: List<SearchTrendDto>? = null,

    @SerializedName("gcode")
    val gcode: Int? = null,

    @SerializedName("msg")
    val msg: String? = null
)

data class SearchTrendDto(
    @SerializedName("text")
    val text: String = "",

    @SerializedName("rank")
    val rank: Int = 0
)

// ============================================================
// /search/suggest/ - 자동완성 추천
// ============================================================
data class SearchSuggestResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("objects")
    val objects: List<SearchSuggestDto>? = null,

    @SerializedName("gcode")
    val gcode: Int? = null,

    @SerializedName("msg")
    val msg: String? = null
)

data class SearchSuggestDto(
    @SerializedName("text")
    val text: String = "",

    @SerializedName("type")
    val type: String? = null // idol, support 등
)
