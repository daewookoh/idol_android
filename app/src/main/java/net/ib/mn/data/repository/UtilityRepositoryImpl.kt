package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.UtilityApi
import net.ib.mn.data.remote.dto.TimezoneUpdateResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UtilityRepository
import javax.inject.Inject

/**
 * UtilityRepository 구현체
 *
 * BaseRepository의 safeApiCall + validateSuccess 패턴 사용
 */
class UtilityRepositoryImpl @Inject constructor(
    private val utilityApi: UtilityApi
) : BaseRepository(), UtilityRepository {

    override fun updateTimezone(timezone: String): Flow<ApiResult<TimezoneUpdateResponse>> =
        safeApiCall { utilityApi.updateTimezone(mapOf("timezone" to timezone)) }
            .validateSuccess()
}
