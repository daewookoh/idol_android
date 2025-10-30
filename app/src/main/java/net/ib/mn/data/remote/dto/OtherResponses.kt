package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 나머지 API Response 모델들
 *
 * NOTE: 프로젝트 확장 시 각 API마다 별도 파일로 분리하고 상세 필드 추가 권장
 * 현재는 ConfigStartup 플로우 완성을 위한 기본 구조만 제공
 */

// ============================================================
// /configs/self/ - 사용자별 설정
// ============================================================
data class ConfigSelfResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: ConfigSelfData?
)

data class ConfigSelfData(
    @SerializedName("language")
    val language: String?,

    @SerializedName("theme")
    val theme: String?,

    @SerializedName("push_enabled")
    val pushEnabled: Boolean?

    // NOTE: 추가 필드는 실제 API 스펙 확인 후 추가 (old 프로젝트 참조)
)

// ============================================================
// /update/ - Idol 업데이트 정보
// ============================================================
data class UpdateInfoResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: UpdateInfoData?
)

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
data class UserSelfResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: UserSelfData?
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

    @SerializedName("profile_image")
    val profileImage: String?,

    @SerializedName("hearts")
    val hearts: Int?,

    // NOTE: 추가 필드는 실제 API 스펙 확인 후 추가 (old 프로젝트 참조)
)

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

    @SerializedName("data")
    val data: List<AdType>?
)

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
    val success: Boolean,

    @SerializedName("data")
    val data: TimezoneData?
)

data class TimezoneData(
    @SerializedName("timezone")
    val timezone: String
)

// ============================================================
// /idols/ - Idol 리스트
// ============================================================
data class IdolListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<IdolData>?
)

data class IdolData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("type")
    val type: Int, // 0: all, 1: solo, 2: group, 3: actor

    @SerializedName("debut_date")
    val debutDate: String?,

    @SerializedName("group")
    val group: String? = null,

    // NOTE: 추가 필드는 실제 API 스펙 확인 후 추가 (old 프로젝트 참조)
)

/**
 * Extension function to convert IdolData to IdolEntity for Room Database.
 */
fun IdolData.toEntity(): net.ib.mn.data.local.entity.IdolEntity {
    return net.ib.mn.data.local.entity.IdolEntity(
        id = id,
        name = name,
        group = group,
        imageUrl = imageUrl,
        heartCount = 0, // 초기값
        isTop3 = false, // 초기값
        timestamp = System.currentTimeMillis()
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
// Common Response (범용)
// ============================================================
data class CommonResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("code")
    val code: Int? = null,

    @SerializedName("gcode")
    val gcode: Int? = null,

    @SerializedName("mcode")
    val mcode: Int? = null
)
