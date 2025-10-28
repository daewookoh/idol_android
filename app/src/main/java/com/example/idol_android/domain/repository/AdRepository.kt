package com.example.idol_android.domain.repository

import com.example.idol_android.data.remote.dto.AdTypeListResponse
import com.example.idol_android.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * Ad Repository 인터페이스
 */
interface AdRepository {
    fun getAdTypeList(): Flow<ApiResult<AdTypeListResponse>>
}
