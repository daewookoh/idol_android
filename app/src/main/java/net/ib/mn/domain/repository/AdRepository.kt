package net.ib.mn.domain.repository

import net.ib.mn.data.remote.dto.AdTypeListResponse
import net.ib.mn.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * Ad Repository 인터페이스
 */
interface AdRepository {
    fun getAdTypeList(): Flow<ApiResult<AdTypeListResponse>>
}
