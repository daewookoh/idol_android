package net.ib.mn.domain.usecase

import net.ib.mn.data.remote.dto.ConfigSelfResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 사용자별 설정 조회 UseCase
 *
 * 비즈니스 로직:
 * 1. API 호출하여 사용자 설정 조회
 * 2. 성공 시 데이터 반환
 * 3. DataStore에 캐싱
 */
class GetConfigSelfUseCase @Inject constructor(
    private val configRepository: ConfigRepository
) {
    operator fun invoke(): Flow<ApiResult<ConfigSelfResponse>> {
        return configRepository.getConfigSelf()
    }
}
