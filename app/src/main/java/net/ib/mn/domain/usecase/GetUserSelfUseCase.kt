package net.ib.mn.domain.usecase

import net.ib.mn.data.remote.dto.UserSelfResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 사용자 프로필 조회 UseCase
 *
 * 비즈니스 로직:
 * 1. ETag 캐싱 지원
 * 2. HTTP 304 Not Modified 처리
 * 3. 사용자 정보 DataStore 저장
 *
 * @param etag ETag 값 (null이면 캐시 비활성화)
 * @param cacheControl Cache-Control 헤더 값 (캐시 제어용, "no-cache"로 설정 시 캐시 완전 비활성화)
 * @param timestamp 캐시 무효화용 타임스탬프 (null이 아니면 ts에 설정되어 캐시 무효화)
 */
class GetUserSelfUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(
        etag: String? = null,
        cacheControl: String? = null,
        timestamp: Int? = null
    ): Flow<ApiResult<UserSelfResponse>> {
        return userRepository.getUserSelf(etag, cacheControl, timestamp)
    }
}
