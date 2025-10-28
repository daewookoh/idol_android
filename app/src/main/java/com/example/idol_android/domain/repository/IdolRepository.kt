package com.example.idol_android.domain.repository

import com.example.idol_android.data.remote.dto.IdolListResponse
import com.example.idol_android.data.remote.dto.UpdateInfoResponse
import com.example.idol_android.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * Idol Repository 인터페이스
 *
 * Idol 관련 데이터 접근 추상화
 */
interface IdolRepository {

    /**
     * Idol 업데이트 정보 조회
     *
     * @return Flow<ApiResult<UpdateInfoResponse>>
     */
    fun getUpdateInfo(): Flow<ApiResult<UpdateInfoResponse>>

    /**
     * Idol 리스트 조회
     *
     * @param type Idol 타입 (0: all, 1: solo, 2: group, 3: actor)
     * @param category 카테고리 필터
     * @return Flow<ApiResult<IdolListResponse>>
     */
    fun getIdols(type: Int? = null, category: String? = null): Flow<ApiResult<IdolListResponse>>
}
