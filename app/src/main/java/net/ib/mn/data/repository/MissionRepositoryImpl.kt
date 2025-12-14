package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.MissionApi
import net.ib.mn.data.remote.dto.ClaimMissionRewardRequest
import net.ib.mn.data.remote.dto.ClaimMissionRewardResponse
import net.ib.mn.data.remote.dto.WelcomeMissionResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.MissionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MissionRepositoryImpl - 웰컴 미션 관련 Repository 구현체
 *
 * BaseRepository를 상속받아 safeApiCall 사용
 */
@Singleton
class MissionRepositoryImpl @Inject constructor(
    private val missionApi: MissionApi
) : BaseRepository(), MissionRepository {

    override fun getWelcomeMission(): Flow<ApiResult<WelcomeMissionResponse>> =
        safeApiCall { missionApi.getWelcomeMission() }

    override fun claimMissionReward(key: String): Flow<ApiResult<ClaimMissionRewardResponse>> =
        safeApiCall { missionApi.claimMissionReward(ClaimMissionRewardRequest(key)) }
}
