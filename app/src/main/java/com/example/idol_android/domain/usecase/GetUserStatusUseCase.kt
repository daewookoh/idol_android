package com.example.idol_android.domain.usecase

import com.example.idol_android.data.remote.dto.UserStatusResponse
import com.example.idol_android.domain.model.ApiResult
import com.example.idol_android.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 사용자 상태 조회 UseCase
 *
 * 비즈니스 로직:
 * 1. 튜토리얼 완료 여부 확인
 * 2. 첫 로그인 여부 확인
 */
class GetUserStatusUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<ApiResult<UserStatusResponse>> {
        return userRepository.getUserStatus()
    }
}
