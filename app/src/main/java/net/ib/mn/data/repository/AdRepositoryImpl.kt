package net.ib.mn.data.repository

import net.ib.mn.data.remote.api.AdApi
import net.ib.mn.data.remote.dto.AdTypeListResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.AdRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * AdRepository 구현체
 *
 * BaseRepository의 safeApiCall 패턴을 사용하여 중복 코드 제거
 */
class AdRepositoryImpl @Inject constructor(
    private val adApi: AdApi
) : BaseRepository(), AdRepository {

    override fun getAdTypeList(): Flow<ApiResult<AdTypeListResponse>> =
        safeApiCall { adApi.getAdTypeList() }
}
