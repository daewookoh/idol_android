package net.ib.mn.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.dto.VoteResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import javax.inject.Inject

/**
 * 아이돌 하트 투표 UseCase
 *
 * old 프로젝트의 GiveHeartToIdolUseCase와 동일
 *
 * 비즈니스 로직:
 * 1. 아이돌 ID와 하트 개수를 받아 투표 API 호출
 * 2. 투표 결과 반환 (성공 여부, 보너스 하트, 메시지)
 */
class VoteIdolUseCase @Inject constructor(
    private val rankingRepository: RankingRepository
) {
    operator fun invoke(idolId: Int, heart: Long): Flow<ApiResult<VoteResponse>> {
        return rankingRepository.voteIdol(idolId, heart)
    }
}
