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
}
