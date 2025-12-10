package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 나머지 API Response 모델들
 *
 * NOTE: 프로젝트 확장 시 각 API마다 별도 파일로 분리하고 상세 필드 추가 권장
 * 현재는 ConfigStartup 플로우 완성을 위한 기본 구조만 제공
 *
 * ConfigSelfResponse는 ConfigSelfResponse.kt 파일에 별도로 정의됨
 */

// ============================================================
// /update/ - Idol 업데이트 정보
// ============================================================
data class UpdateInfoResponse(
    @SerializedName("success")
    override val success: Boolean,

    @SerializedName("data")
    val data: UpdateInfoData?
) : net.ib.mn.data.repository.HasSuccess

data class UpdateInfoData(
    @SerializedName("all_idol_update")
    val allIdolUpdate: String?, // ISO 8601 timestamp

    @SerializedName("daily_idol_update")
    val dailyIdolUpdate: String?,

    @SerializedName("sns_channel_update")
    val snsChannelUpdate: String?
)

// ============================================================
// /users/self/ - 사용자 프로필
// ============================================================
/**
 * getUserSelf API Response
 *
 * NOTE: old 프로젝트와 동일한 구조 사용
 * 서버는 {objects: [UserModel], ...} 형식으로 응답합니다
 */
data class UserSelfResponse(
    @SerializedName("objects")
    val objects: List<UserSelfData>,

    @SerializedName("event_list")
    val eventList: String?,

    @SerializedName("article_count")
    val articleCount: Int?,

    @SerializedName("comment_count")
    val commentCount: Int?,

    @SerializedName("need_change_info")
    val needChangeInfo: String?,

    @SerializedName("google_review")
    val googleReview: String?
)

data class UserSelfData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("email")
    val email: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("nickname")
    val nickname: String?,

    @SerializedName("image_url")
    val profileImage: String?,

    @SerializedName("heart")
    val heart: Int?,

    @SerializedName("diamond")
    val diamond: Int?,

    @SerializedName("strong_heart")
    val strongHeart: Long?,

    @SerializedName("weak_heart")
    val weakHeart: Long?,

    @SerializedName("level")
    val level: Int?,

    @SerializedName("level_heart")
    val levelHeart: Long?,

    @SerializedName("power")
    val power: Int?,

    @SerializedName("resource_uri")
    val resourceUri: String?,

    @SerializedName("push_key")
    val pushKey: String?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("push_filter")
    val pushFilter: Int?,

    @SerializedName("status_message")
    val statusMessage: String?,

    @SerializedName("ts")
    val ts: Int?,

    @SerializedName("item_no")
    val itemNo: Int?,

    @SerializedName("domain")
    val domain: String?,

    @SerializedName("give_heart")
    val giveHeart: Int?,

    @SerializedName("most")
    val most: MostIdol?,

    @SerializedName("subscriptions")
    val subscriptions: List<SubscriptionModel>?
)

/**
 * 구독 정보 모델
 * old 프로젝트의 SubscriptionModel과 동일한 구조
 *
 * @param familyappId 앱 ID (1: idol, 2: celeb)
 * @param skuCode 구독 상품 코드 (daily_pack_android, daily_pack_actor_android 등)
 */
data class SubscriptionModel(
    @SerializedName("familyapp_id")
    val familyappId: Int?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("order_id")
    val orderId: String?,

    @SerializedName("package_name")
    val packageName: String?,

    @SerializedName("sku_code")
    val skuCode: String?,

    @SerializedName("subscription_created_at")
    val subscriptionCreatedAt: String?,

    @SerializedName("subscription_expired_at")
    val subscriptionExpiredAt: String?
)

/**
 * 사용자의 최애 아이돌 정보
 * old 프로젝트의 UserModel.most와 동일한 구조
 */
data class MostIdol(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String?,

    @SerializedName("name_en")
    val nameEn: String?,

    @SerializedName("name_jp")
    val nameJp: String?,

    @SerializedName("name_zh")
    val nameZh: String?,

    @SerializedName("name_zh_tw")
    val nameZhTw: String?,

    @SerializedName("type")
    val type: String?,

    @SerializedName("category")
    val category: String?, // "M": male, "F": female

    @SerializedName("group_id")
    val groupId: Int?,

    @SerializedName("group_name")
    val groupName: String? = null,

    @SerializedName("resource_uri")
    val resourceUri: String?,

    @SerializedName("chart_codes")
    val chartCodes: List<String>?,  // 예: ["PR_S_F", "GLOBALS", "MIRACLE"]

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("image_url2")
    val imageUrl2: String?,

    @SerializedName("image_url3")
    val imageUrl3: String?,

    @SerializedName("heart")
    val heart: Long?
)

/**
 * Extension function to convert MostIdol to IdolEntity for Room Database.
 * getUserSelf API로 받은 most 데이터를 로컬 DB에 upsert하기 위한 변환
 */
fun MostIdol.toEntity(): net.ib.mn.data.local.entity.IdolEntity {
    return net.ib.mn.data.local.entity.IdolEntity(
        id = id,
        name = name ?: "",
        nameEn = nameEn ?: "",
        nameJp = nameJp ?: "",
        nameZh = nameZh ?: "",
        nameZhTw = nameZhTw ?: "",
        type = type ?: "",
        category = category ?: "",
        groupId = groupId ?: 0,
        resourceUri = resourceUri ?: "",
        imageUrl = imageUrl,
        imageUrl2 = imageUrl2,
        imageUrl3 = imageUrl3,
        heart = heart ?: 0L,
        // 나머지 필드는 기본값 사용
        miracleCount = 0,
        angelCount = 0,
        rookieCount = 0,
        anniversary = "N",
        anniversaryDays = null,
        birthDay = null,
        burningDay = null,
        comebackDay = null,
        debutDay = null,
        description = "",
        fairyCount = 0,
        isViewable = "Y",
        top3 = null,
        top3Type = null,
        top3Seq = -1,
        top3ImageVer = "",
        infoSeq = -1,
        isLunarBirthday = null,
        mostCount = 0,
        mostCountDesc = null,
        updateTs = (System.currentTimeMillis() / 1000).toInt(),
        sourceApp = null,
        fdName = null,
        fdNameEn = null
    )
}

// ============================================================
// /users/status/ - 사용자 상태 (튜토리얼 등)
// ============================================================
data class UserStatusResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: UserStatusData?
)

data class UserStatusData(
    @SerializedName("tutorial_completed")
    val tutorialCompleted: Boolean?,

    @SerializedName("first_login")
    val firstLogin: Boolean?,

    // NOTE: 추가 필드는 실제 API 스펙 확인 후 추가 (old 프로젝트 참조)
)

// ============================================================
// /ads/types/ - 광고 타입 목록
// ============================================================
data class AdTypeListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("objects")
    val objects: List<SupportAdTypeDto>?,

    @SerializedName("gcode")
    val gcode: Int?,

    @SerializedName("msg")
    val msg: String?
)

/**
 * 서포트 광고 타입 DTO
 * old 프로젝트의 SupportAdTypeListModel과 동일한 구조
 */
data class SupportAdTypeDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("image_url2")
    val imageUrl2: String?,

    @SerializedName("icon_url")
    val iconUrl: String?,

    @SerializedName("name")
    val name: String,

    @SerializedName("period")
    val period: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("location")
    val location: String?,

    @SerializedName("goal")
    val goal: Int?,

    @SerializedName("require")
    val require: Int?,

    @SerializedName("guide")
    val guide: String?,

    @SerializedName("is_viewable")
    val isViewable: String?,

    @SerializedName("category")
    val category: String?,

    @SerializedName("location_image_url")
    val locationImageUrl: String?,

    @SerializedName("location_map_url")
    val locationMapUrl: String?
)

/**
 * SupportAdTypeDto를 SupportAdTypeModel로 변환
 */
fun SupportAdTypeDto.toDomain(): net.ib.mn.domain.model.SupportAdTypeModel {
    return net.ib.mn.domain.model.SupportAdTypeModel(
        id = id,
        imageUrl = imageUrl,
        imageUrl2 = imageUrl2,
        iconUrl = iconUrl,
        name = name,
        period = period,
        description = description ?: "",
        location = location ?: "",
        goal = goal ?: 0,
        require = require ?: 0,
        guide = guide ?: "",
        isViewable = isViewable ?: "Y",
        category = category ?: "",
        locationImageUrl = locationImageUrl,
        locationMapUrl = locationMapUrl
    )
}

data class AdType(
    @SerializedName("id")
    val id: Int,

    @SerializedName("type")
    val type: String,

    @SerializedName("reward")
    val reward: Int?
)

// ============================================================
// /messages/coupons/ - 쿠폰 메시지
// ============================================================
data class MessageCouponResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<CouponMessage>?
)

data class CouponMessage(
    @SerializedName("id")
    val id: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("coupon_code")
    val couponCode: String?
)

// ============================================================
// /timezone/update/ - 타임존 업데이트
// ============================================================
data class TimezoneUpdateResponse(
    @SerializedName("success")
    override val success: Boolean,

    @SerializedName("data")
    val data: TimezoneData?
) : net.ib.mn.data.repository.HasSuccess

data class TimezoneData(
    @SerializedName("timezone")
    val timezone: String
)

// ============================================================
// /idols/ - Idol 리스트
// ============================================================
data class IdolListResponse(
    @SerializedName("all_idol_update")
    val allIdolUpdate: String?,

    @SerializedName("daily_idol_update")
    val dailyIdolUpdate: String?,

    @SerializedName("gcode")
    val gcode: Int?,

    @SerializedName("meta")
    val meta: IdolMeta?,

    @SerializedName("objects")
    val data: List<IdolData>?
)

data class IdolMeta(
    @SerializedName("limit")
    val limit: Int?,

    @SerializedName("next")
    val next: String?,

    @SerializedName("offset")
    val offset: Int?,

    @SerializedName("previous")
    val previous: String?,

    @SerializedName("total_count")
    val totalCount: Int?
)

data class IdolData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("name_en")
    val nameEn: String?,

    @SerializedName("name_jp")
    val nameJp: String?,

    @SerializedName("name_zh")
    val nameZh: String?,

    @SerializedName("name_zh_tw")
    val nameZhTw: String?,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("image_url2")
    val imageUrl2: String?,

    @SerializedName("image_url3")
    val imageUrl3: String?,

    @SerializedName("type")
    val type: String?, // "S": solo, "G": group

    @SerializedName("category")
    val category: String?, // "M": male, "F": female

    @SerializedName("heart")
    val heart: Int?,

    @SerializedName("group_id")
    val groupId: Int?,

    @SerializedName("group_name")
    val groupName: String?,  // 그룹명 추가

    @SerializedName("birthday")
    val birthday: String?,

    @SerializedName("is_lunar_birthday")
    val isLunarBirthday: String?,

    @SerializedName("debut_day")
    val debutDay: String?,

    @SerializedName("comeback_day")
    val comebackDay: String?,

    @SerializedName("anniversary")
    val anniversary: String?,

    @SerializedName("anniversary_days")
    val anniversaryDays: Int?,

    @SerializedName("burning_day")
    val burningDay: String?,

    @SerializedName("deathday")
    val deathday: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("fd_name")
    val fdName: String?,

    @SerializedName("fd_name_en")
    val fdNameEn: String?,

    @SerializedName("is_viewable")
    val isViewable: String?,

    @SerializedName("league")
    val league: String?,

    @SerializedName("most_count")
    val mostCount: Int?,

    @SerializedName("most_count_desc")
    val mostCountDesc: String?,

    @SerializedName("angel_count")
    val angelCount: Int?,

    @SerializedName("fairy_count")
    val fairyCount: Int?,

    @SerializedName("miracle_count")
    val miracleCount: Int?,

    @SerializedName("rookie_count")
    val rookieCount: Int?,

    @SerializedName("top3")
    val top3: String?,

    @SerializedName("top3_image_ver")
    val top3ImageVer: String?,

    @SerializedName("top3_type")
    val top3Type: String?,

    @SerializedName("resource_uri")
    val resourceUri: String?

    // NOTE: 추가 필드는 실제 API 스펙 확인 후 추가 (old 프로젝트 참조)
)

/**
 * Extension function to convert IdolData to IdolEntity for Room Database.
 * old 프로젝트와 완전히 동일한 매핑 (IdolLocal.kt의 toLocal() 참조)
 */
fun IdolData.toEntity(): net.ib.mn.data.local.entity.IdolEntity {
    return net.ib.mn.data.local.entity.IdolEntity(
        id = id,
        miracleCount = miracleCount ?: 0,
        angelCount = angelCount ?: 0,
        rookieCount = rookieCount ?: 0,
        anniversary = anniversary ?: "N",
        anniversaryDays = anniversaryDays,
        birthDay = birthday,
        burningDay = burningDay,
        category = category ?: "",
        comebackDay = comebackDay,
        debutDay = debutDay,
        description = description ?: "",
        fairyCount = fairyCount ?: 0,
        groupId = groupId ?: 0,
        heart = heart?.toLong() ?: 0L,
        imageUrl = imageUrl,
        imageUrl2 = imageUrl2,
        imageUrl3 = imageUrl3,
        isViewable = isViewable ?: "Y",
        name = name ?: "",
        nameEn = nameEn ?: "",
        nameJp = nameJp ?: "",
        nameZh = nameZh ?: "",
        nameZhTw = nameZhTw ?: "",
        resourceUri = resourceUri ?: "",
        top3 = top3,
        top3Type = top3Type,
        top3Seq = -1,  // API에 없음
        top3ImageVer = top3ImageVer ?: "",
        type = type ?: "",
        infoSeq = -1,  // API에 없음
        isLunarBirthday = isLunarBirthday,
        mostCount = mostCount ?: 0,
        mostCountDesc = mostCountDesc,
        updateTs = 0,  // API에 없음
        sourceApp = null,  // API에 없음
        fdName = fdName,
        fdNameEn = fdNameEn
    )
}

// ============================================================
// /users/iab_key/ - IAB 공개키
// ============================================================
data class IabKeyResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("key")
    val key: String? // Encrypted key
)

// ============================================================
// /users/blocks/ - 차단 사용자 목록
// ============================================================
data class BlockListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<BlockedUser>?
)

data class BlockedUser(
    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("username")
    val username: String
)

// ============================================================
// /blocks/add/ - 사용자 차단
// ============================================================
data class BlockUserRequest(
    @SerializedName("target_id")
    val targetId: Int,

    @SerializedName("reason")
    val reason: Int = 0,

    @SerializedName("block")
    val block: String = "Y"  // "Y": 차단, "N": 차단 해제
)

// ============================================================
// /users/provide_heart/ - 하트박스 하트 제공
// ============================================================
data class ProvideHeartRequest(
    @SerializedName("type")
    val type: String  // "heartbox"
)

/**
 * 하트박스 응답 모델
 *
 * @param success API 성공 여부
 * @param viewable 하트박스 계속 표시 여부 (false면 숨김, 비디오 광고 시청 후 true로 복구)
 * @param heart 제공받은 하트 개수
 * @param button 비디오 광고 버튼 표시 여부 (heart=0이고 button=true면 광고 시청 유도)
 */
data class ProvideHeartResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("viewable")
    val viewable: Boolean,

    @SerializedName("heart")
    val heart: Long,

    @SerializedName("button")
    val button: Boolean
)

// ============================================================
// /users/validate/ - 사용자 검증 (회원 여부 확인)
// ============================================================
data class ValidateResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("msg")
    val message: String?,

    @SerializedName("domain")
    val domain: String?, // 가입된 도메인 (email, kakao, google 등)

    @SerializedName("gcode")
    val gcode: Int?,

    @SerializedName("mcode")
    val mcode: Int?
)

// ============================================================
// /users/email_signin/ - 이메일 로그인
// ============================================================
data class SignInRequest(
    @SerializedName("domain")
    val domain: String?, // email, kakao, google, line, facebook

    @SerializedName("email")
    val email: String,

    @SerializedName("passwd")
    val passwd: String,

    @SerializedName("push_key")
    val deviceKey: String,

    @SerializedName("gmail")
    val gmail: String,

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("app_id")
    val appId: String
)

data class SignInResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("msg")
    val message: String?,

    @SerializedName("data")
    val data: SignInData?,

    @SerializedName("gcode")
    val gcode: Int?,

    @SerializedName("mcode")
    val mcode: Int?
)

data class SignInData(
    @SerializedName("token")
    val token: String,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("email")
    val email: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("nickname")
    val nickname: String?,

    @SerializedName("profile_image")
    val profileImage: String?
)

// ============================================================
// /users/ - 회원가입
// ============================================================
data class SignUpRequest(
    @SerializedName("domain")
    val domain: String?, // email, kakao, google, line, facebook

    @SerializedName("email")
    val email: String,

    @SerializedName("passwd")
    val passwd: String,

    @SerializedName("nickname")
    val nickname: String,

    @SerializedName("referral_code")
    val referralCode: String,

    @SerializedName("push_key")
    val pushKey: String, // deviceKey (FCM token)

    @SerializedName("gmail")
    val gmail: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("app_id")
    val appId: String,

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("google_account")
    val googleAccount: String, // "N" for all cases

    @SerializedName("time")
    val time: Long,

    @SerializedName("facebook_id")
    val facebookId: Long? = null
)

// ============================================================
// /users/find_passwd/ - 비밀번호 찾기
// ============================================================
data class FindPasswordRequest(
    @SerializedName("email")
    val email: String
)

/*
 * ============================================================
 * NOTE: 추가 가능한 API Response 모델 (필요 시 구현)
 * ============================================================
 *
 * 1. /offerwall/reward/ - OfferWallRewardResponse
 * 2. /types/ - TypeListResponse (CELEB flavor)
 * 3. /payments/google_item/ - Payment verification
 * 4. /payments/google_subscription_check/ - Subscription check
 * 5. /users/iab_verify/ - IAB verification
 * 6. ... (총 18개 API)
 *
 * 패턴:
 * - data class XXXResponse(success: Boolean, data: T?)
 * - @SerializedName으로 JSON 매핑
 * - nullable 타입 사용 (안전성)
 */

// ============================================================
// /users/check_event/ - 이벤트 체크 (웰컴 미션, 배너 등)
// ============================================================
/**
 * checkEvent API Response
 *
 * old 프로젝트의 MainViewModel.requestEvent()에서 사용
 * show_welcome_mission 필드로 웰컴 미션 버튼 표시 여부 결정
 */
data class CheckEventResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("show_welcome_mission")
    val showWelcomeMission: Boolean?,

    @SerializedName("most_picks")
    val mostPicks: MostPicksData?,

    @SerializedName("guide_url")
    val guideUrl: String?,

    @SerializedName("banners")
    val banners: List<EventBanner>?,

    @SerializedName("progress")
    val progress: String?, // "Y" or "N" - 버닝타임 진행 여부

    @SerializedName("burning_time")
    val burningTime: Int?
)

/**
 * 최애 픽 데이터
 * API 응답: {"heartpick": [116], "miracle": false, "onepick": [678], "themepick": []}
 */
data class MostPicksData(
    @SerializedName("heartpick")
    val heartpick: List<Int>?,

    @SerializedName("miracle")
    val miracle: Boolean?,

    @SerializedName("onepick")
    val onepick: List<Int>?,

    @SerializedName("themepick")
    val themepick: List<Int>?
)

/**
 * 이벤트 배너 정보
 */
data class EventBanner(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("link_url")
    val linkUrl: String?,

    @SerializedName("type")
    val type: String?
)

// ============================================================
// Common Response (범용)
// ============================================================
data class CommonResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("code")
    val code: Int? = null,

    @SerializedName("gcode")
    val gcode: Int? = null,

    @SerializedName("mcode")
    val mcode: Int? = null
)

// ============================================================
// /idols/wiki_name/ - 위키 이름 조회
// ============================================================
data class WikiNameResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("wiki_name")
    val wikiName: String?
)

// ============================================================
// /redirect/ - 리다이렉트 URL 조회
// ============================================================
data class RedirectResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("url")
    val url: String?
)
