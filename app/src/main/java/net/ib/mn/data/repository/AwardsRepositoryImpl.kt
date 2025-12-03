package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.AwardsApi
import net.ib.mn.data.remote.dto.AwardIdolsResponse
import net.ib.mn.data.remote.dto.AwardRankResponse
import net.ib.mn.data.remote.dto.AwardsCurrentResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.AwardsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AwardsRepositoryImpl - 어워즈 관련 Repository 구현체
 *
 * BaseRepository를 상속받아 safeApiCall 사용
 */
@Singleton
class AwardsRepositoryImpl @Inject constructor(
    private val awardsApi: AwardsApi
) : BaseRepository(), AwardsRepository {

    override fun getCurrent(): Flow<ApiResult<AwardsCurrentResponse>> =
        safeApiCall { awardsApi.getCurrent() }

    override fun getAwardRank(chartCode: String?, event: String?): Flow<ApiResult<AwardRankResponse>> =
        safeApiCall { awardsApi.getAwardRank(chartCode, event) }

    override fun getAwardIdols(chartCode: String?, fields: String?): Flow<ApiResult<AwardIdolsResponse>> =
        safeApiCall { awardsApi.getAwardIdols(chartCode, fields) }
}
