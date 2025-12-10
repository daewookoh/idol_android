package net.ib.mn.domain.model

import net.ib.mn.data.remote.dto.MostIdol
import net.ib.mn.data.remote.dto.SearchIdolDto
import net.ib.mn.data.remote.dto.SearchSupportDto
import net.ib.mn.data.remote.dto.SearchSuggestDto
import net.ib.mn.data.remote.dto.SearchTrendDto
import net.ib.mn.data.remote.dto.SearchWallpaperDto

/**
 * 검색 결과 모델
 *
 * old 프로젝트의 SearchResultActivity 분석 기반
 */
data class SearchResultModel(
    val idols: List<SearchIdolModel> = emptyList(),
    val supports: List<SearchSupportModel> = emptyList(),
    val wallpapers: List<SearchWallpaperModel> = emptyList(),
    val articles: List<ArticleModel> = emptyList(),
    val smallTalks: List<ArticleModel> = emptyList(),
    val smallTalkOffset: Int = 0,
    val smallTalkTotal: Int = 0,
    val smallTalkLimit: Int = 10
) {
    val hasMoreSmallTalks: Boolean
        get() = smallTalkOffset + smallTalks.size < smallTalkTotal
}

/**
 * 검색된 아이돌 모델
 */
data class SearchIdolModel(
    val id: Int,
    val name: String,
    val nameEn: String?,
    val nameJp: String?,
    val nameZh: String?,
    val nameZhTw: String?,
    val type: String?,
    val category: String?,
    val groupId: Int?,
    val groupName: String?,
    val imageUrl: String?,
    val imageUrl2: String?,
    val heart: Long,
    val resourceUri: String?,
    // 뱃지 관련
    val birthday: String?,
    val isLunarBirthday: String?,
    val debutDay: String?,
    val comebackDay: String?,
    val burningDay: String?,
    // 기부 뱃지 카운트
    val angelCount: Int,
    val fairyCount: Int,
    val miracleCount: Int,
    val rookieCount: Int,
    // 팬덤명
    val fdName: String?,
    val fdNameEn: String?,
    // 차트 코드
    val chartCodes: List<String>?,
    // 즐겨찾기 여부 (로컬 상태)
    val isFavorite: Boolean = false,
    // 최애 여부 (로컬 상태)
    val isMost: Boolean = false
) {
    /**
     * 생일 뱃지 표시 여부
     * old 프로젝트의 SearchedAdapter 로직 참고
     */
    val showBirthdayBadge: Boolean
        get() = !birthday.isNullOrEmpty()

    /**
     * 데뷔일 뱃지 표시 여부
     */
    val showDebutBadge: Boolean
        get() = !debutDay.isNullOrEmpty()

    /**
     * 컴백일 뱃지 표시 여부
     */
    val showComebackBadge: Boolean
        get() = !comebackDay.isNullOrEmpty()

    /**
     * 올인데이(버닝데이) 뱃지 표시 여부
     */
    val showAllInDayBadge: Boolean
        get() = !burningDay.isNullOrEmpty()

    /**
     * 기부 뱃지 표시 여부 (천사, 요정, 기적, 루키 중 하나라도 있으면)
     */
    val showDonationBadge: Boolean
        get() = angelCount > 0 || fairyCount > 0 || miracleCount > 0 || rookieCount > 0
}

/**
 * 검색된 서포트 모델
 */
data class SearchSupportModel(
    val id: Int,
    val title: String,
    val content: String?,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val goalHeart: Long,
    val currentHeart: Long,
    val status: String?,
    val startDate: String?,
    val endDate: String?,
    val idol: MostIdol?,
    val createdAt: String?
) {
    /**
     * 달성률 (0.0 ~ 1.0)
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

    /**
     * 진행 중인 서포트 여부
     */
    val isOngoing: Boolean
        get() = status == "진행중" || status == "ongoing"

    /**
     * 성공한 서포트 여부
     */
    val isSuccess: Boolean
        get() = status == "성공" || status == "success"
}

/**
 * 검색된 배경화면 모델
 * old 프로젝트의 WallpaperModel 구조 기반
 * - 아이돌별로 그룹핑된 배경화면 목록
 */
data class SearchWallpaperModel(
    val idolId: Int,
    val imageUrls: List<String>,
    val totalCount: Int,
    // idolName은 별도 API 호출로 가져와야 하므로 nullable
    val idolName: String? = null
)

/**
 * 핫 트렌드 (인기 검색어) 모델
 */
data class SearchTrendModel(
    val text: String,
    val rank: Int
)

/**
 * 자동완성 추천 모델
 */
data class SearchSuggestModel(
    val text: String,
    val type: String?
)

// ============================================================
// DTO → Domain Model 변환 Extension Functions
// ============================================================

fun SearchIdolDto.toDomain(
    isFavorite: Boolean = false,
    isMost: Boolean = false
): SearchIdolModel = SearchIdolModel(
    id = id,
    name = name,
    nameEn = nameEn,
    nameJp = nameJp,
    nameZh = nameZh,
    nameZhTw = nameZhTw,
    type = type,
    category = category,
    groupId = groupId,
    groupName = groupName,
    imageUrl = imageUrl,
    imageUrl2 = imageUrl2,
    heart = heart,
    resourceUri = resourceUri,
    birthday = birthday,
    isLunarBirthday = isLunarBirthday,
    debutDay = debutDay,
    comebackDay = comebackDay,
    burningDay = burningDay,
    angelCount = angelCount,
    fairyCount = fairyCount,
    miracleCount = miracleCount,
    rookieCount = rookieCount,
    fdName = fdName,
    fdNameEn = fdNameEn,
    chartCodes = chartCodes,
    isFavorite = isFavorite,
    isMost = isMost
)

fun SearchSupportDto.toDomain(): SearchSupportModel = SearchSupportModel(
    id = id,
    title = title,
    content = content,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    goalHeart = goalHeart,
    currentHeart = currentHeart,
    status = status,
    startDate = startDate,
    endDate = endDate,
    idol = idol,
    createdAt = createdAt
)

fun SearchWallpaperDto.toDomain(): SearchWallpaperModel = SearchWallpaperModel(
    idolId = idolId,
    imageUrls = imageUrls ?: emptyList(),
    totalCount = totalCount,
    idolName = null // 아이돌 이름은 별도 조회 필요
)

fun SearchTrendDto.toDomain(): SearchTrendModel = SearchTrendModel(
    text = text,
    rank = rank
)

fun SearchSuggestDto.toDomain(): SearchSuggestModel = SearchSuggestModel(
    text = text,
    type = type
)
