package net.ib.mn.domain.usecase

import net.ib.mn.data.remote.dto.ConfigStartupResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 앱 시작 설정 조회 UseCase
 *
 * 비즈니스 로직:
 * 1. API 호출하여 startup config 조회
 * 2. 성공 시 데이터 반환
 * 3. 실패 시 에러 처리
 *
 * TODO: 추가 비즈니스 로직
 * - DataStore에 캐싱
 * - 욕설 필터 리스트 파싱 및 저장
 * - 공지사항 로컬 저장
 */
class GetConfigStartupUseCase @Inject constructor(
    private val configRepository: ConfigRepository
) {
    operator fun invoke(): Flow<ApiResult<ConfigStartupResponse>> {
        return configRepository.getConfigStartup()
    }
}
