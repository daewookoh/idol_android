package com.example.idol_android.domain.usecase

import com.example.idol_android.data.remote.dto.UserSelfResponse
import com.example.idol_android.domain.model.ApiResult
import com.example.idol_android.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 사용자 프로필 조회 UseCase
 *
 * 비즈니스 로직:
 * 1. ETag 캐싱 지원
 * 2. HTTP 304 Not Modified 처리
 * 3. 사용자 정보 DataStore 저장
 */
class GetUserSelfUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(etag: String? = null): Flow<ApiResult<UserSelfResponse>> {
        return userRepository.getUserSelf(etag)
    }
}
