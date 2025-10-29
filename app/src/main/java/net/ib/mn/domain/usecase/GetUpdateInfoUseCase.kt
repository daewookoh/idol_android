package net.ib.mn.domain.usecase

import net.ib.mn.data.remote.dto.UpdateInfoResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.IdolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Idol 업데이트 정보 조회 UseCase
 *
 * 비즈니스 로직:
 * 1. API 호출하여 업데이트 플래그 조회
 * 2. DataStore의 기존 플래그와 비교
 * 3. 변경 시 Idol 데이터 동기화 필요
 */
class GetUpdateInfoUseCase @Inject constructor(
    private val idolRepository: IdolRepository
) {
    operator fun invoke(): Flow<ApiResult<UpdateInfoResponse>> {
        return idolRepository.getUpdateInfo()
    }
}
