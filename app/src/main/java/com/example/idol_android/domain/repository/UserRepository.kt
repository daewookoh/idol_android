package com.example.idol_android.domain.repository

import com.example.idol_android.data.remote.dto.*
import com.example.idol_android.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * User Repository 인터페이스
 *
 * 사용자 관련 데이터 접근 추상화
 */
interface UserRepository {

    /**
     * 사용자 프로필 조회 (ETag 캐싱 지원)
     *
     * @param etag 이전 ETag 값 (캐시 검증용)
     * @return Flow<ApiResult<UserSelfResponse>>
     */
    fun getUserSelf(etag: String? = null): Flow<ApiResult<UserSelfResponse>>

    /**
     * 사용자 상태 조회
     *
     * @return Flow<ApiResult<UserStatusResponse>>
     */
    fun getUserStatus(): Flow<ApiResult<UserStatusResponse>>

    /**
     * IAB 공개키 조회
     *
     * @return Flow<ApiResult<IabKeyResponse>>
     */
    fun getIabKey(): Flow<ApiResult<IabKeyResponse>>

    /**
     * 차단 사용자 목록 조회
     *
     * @return Flow<ApiResult<BlockListResponse>>
     */
    fun getBlocks(): Flow<ApiResult<BlockListResponse>>

    /**
     * 사용자 검증 (회원 여부 확인)
     *
     * @param type 검증 타입 (email, nickname 등)
     * @param value 검증할 값
     * @param appId 앱 ID
     * @return Flow<ApiResult<ValidateResponse>>
     */
    fun validateUser(
        type: String,
        value: String,
        appId: String
    ): Flow<ApiResult<ValidateResponse>>

    /**
     * 로그인
     *
     * @param domain 로그인 도메인 (email, kakao, google, line, facebook)
     * @param email 이메일
     * @param password 비밀번호
     * @param deviceKey 푸시 키
     * @param gmail Gmail 계정
     * @param deviceId 디바이스 ID
     * @param appId 앱 ID
     * @return Flow<ApiResult<SignInResponse>>
     */
    fun signIn(
        domain: String,
        email: String,
        password: String,
        deviceKey: String,
        gmail: String,
        deviceId: String,
        appId: String
    ): Flow<ApiResult<SignInResponse>>
}
