package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.dto.AwardIdolsResponse
import net.ib.mn.data.remote.dto.AwardRankResponse
import net.ib.mn.data.remote.dto.AwardsCurrentResponse
import net.ib.mn.domain.model.ApiResult

/**
 * AwardsRepository - 어워즈 관련 Repository 인터페이스
 */
interface AwardsRepository {

    /**
     * 현재 진행중인 어워즈 정보 조회
     */
    fun getCurrent(): Flow<ApiResult<AwardsCurrentResponse>>

    /**
     * 어워즈 누적순위 조회
     *
     * @param chartCode 차트 코드 (예: MSM, FSF, MGM, FGF)
     * @param event 이벤트명 (예: 어워즈 name)
     */
    fun getAwardRank(chartCode: String?, event: String?): Flow<ApiResult<AwardRankResponse>>

    /**
     * 어워즈 실시간 순위 조회 (Daily)
     *
     * @param chartCode 차트 코드 (예: MSM, FSF, MGM, FGF)
     * @param fields 필드 (예: "heart,top3")
     */
    fun getAwardIdols(chartCode: String?, fields: String? = "heart,top3"): Flow<ApiResult<AwardIdolsResponse>>
}
