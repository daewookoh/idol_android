package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * NOTE: 각 API를 별도 파일로 분리 권장
 *
 * 현재는 ConfigStartup 플로우 완성을 위해 기본 구조만 제공
 * 프로젝트 확장 시 각 도메인별로 파일 분리 권장:
 * - UserApi.kt
 * - IdolApi.kt
 * - PaymentApi.kt
 * - AdApi.kt
 * 등...
 */

// ============================================================
// User API
// ============================================================
interface UserApi {

    /**
     * 사용자 프로필 조회 (ETag 캐싱 지원)
     *
     * @param ts Timestamp (사용자 정보의 마지막 업데이트 시간)
     * @param etag ETag 값 (캐시 검증용)
     */
    @GET("users/self/")
    suspend fun getUserSelf(
        @Query("ts") ts: Int = 0,
        @Header("If-None-Match") etag: String? = null
    ): Response<UserSelfResponse>

    /**
     * 사용자 상태 조회 (튜토리얼, 첫 로그인 등)
     */
    @GET("users/status/")
    suspend fun getUserStatus(): Response<UserStatusResponse>

    /**
     * IAB 공개키 조회
     */
    @GET("users/iab_key/")
    suspend fun getIabKey(): Response<IabKeyResponse>

    /**
     * 차단 사용자 목록 조회
     */
    @GET("users/blocks/")
    suspend fun getBlocks(): Response<BlockListResponse>

    /**
     * 사용자 검증 (회원 여부 확인)
     * @param params Map with "type" (email, nickname), "value" (검증할 값), "app_id"
     */
    @POST("users/validate/")
    suspend fun validate(
        @Body params: Map<String, String?>
    ): Response<ValidateResponse>

    /**
     * 이메일 로그인
     */
    @POST("users/email_signin/")
    suspend fun signIn(
        @Body body: SignInRequest
    ): Response<SignInResponse>

    // NOTE: 추가 가능한 User 엔드포인트 (필요 시 구현)
    // - POST /users/iab_verify/ - IAB 검증
    // - POST /users/payments/google_item/ - Google Play 아이템 검증
    // - POST /users/payments/google_subscription_check/ - 구독 확인
}

// ============================================================
// Idol API
// ============================================================
interface IdolApi {

    /**
     * Idol 업데이트 정보 조회
     */
    @GET("update/")
    suspend fun getUpdateInfo(): Response<UpdateInfoResponse>

    /**
     * Idol 리스트 조회
     */
    @GET("idols/")
    suspend fun getIdols(
        @Query("type") type: Int? = null,
        @Query("category") category: String? = null
    ): Response<IdolListResponse>

    // NOTE: 추가 가능한 Idol 엔드포인트 (필요 시 구현)
    // - GET /idols/{id}/ - Idol 상세
    // - GET /types/ - Type 리스트 (CELEB flavor)
    // - POST /idols/{id}/vote/ - 투표
}

// ============================================================
// Ad API
// ============================================================
interface AdApi {

    /**
     * 광고 타입 리스트 조회
     */
    @GET("ads/types/")
    suspend fun getAdTypeList(): Response<AdTypeListResponse>

    // NOTE: 추가 가능한 Ad 엔드포인트 (필요 시 구현)
    // - GET /offerwall/reward/ - OfferWall 보상 조회
}

// ============================================================
// Message API
// ============================================================
interface MessageApi {

    /**
     * 쿠폰 메시지 조회
     */
    @GET("messages/coupons/")
    suspend fun getMessageCoupon(): Response<MessageCouponResponse>
}

// ============================================================
// Utility API
// ============================================================
interface UtilityApi {

    /**
     * 타임존 업데이트
     */
    @POST("timezone/update/")
    suspend fun updateTimezone(
        @Body timezone: Map<String, String>
    ): Response<TimezoneUpdateResponse>
}

/*
 * ============================================================
 * API 구현 가이드
 * ============================================================
 *
 * 1. 각 API 인터페이스를 별도 파일로 분리
 * 2. Retrofit annotations 사용:
 *    - @GET, @POST, @PUT, @DELETE
 *    - @Path, @Query, @Body, @Header
 *    - @Multipart (파일 업로드)
 *
 * 3. Response<T> 사용하여 HTTP 메타데이터 접근
 *    - response.code() - HTTP 상태 코드
 *    - response.headers() - 헤더 (ETag 등)
 *    - response.body() - 응답 body
 *
 * 4. suspend 함수로 선언하여 코루틴 지원
 *
 * 5. 에러 처리는 Repository 레이어에서 수행
 */
