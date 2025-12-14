package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.dto.ClaimMissionRewardResponse
import net.ib.mn.data.remote.dto.WelcomeMissionResponse
import net.ib.mn.domain.model.ApiResult

/**
 * Mission Repository 인터페이스
 *
 * 웰컴 미션 관련 데이터 접근 추상화
 */
interface MissionRepository {

    /**
     * 웰컴 미션 목록 조회
     *
     * @return Flow<ApiResult<WelcomeMissionResponse>>
     */
    fun getWelcomeMission(): Flow<ApiResult<WelcomeMissionResponse>>

    /**
     * 미션 보상 수령
     *
     * @param key 미션 키 (welcome_join, welcome_set_most, etc.)
     * @return Flow<ApiResult<ClaimMissionRewardResponse>>
     */
    fun claimMissionReward(key: String): Flow<ApiResult<ClaimMissionRewardResponse>>
}
