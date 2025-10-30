package net.ib.mn.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.dto.CommonResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 회원가입 UseCase
 *
 * old 프로젝트의 UsersRepository.signUp() 호출
 */
class SignUpUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(
        email: String,
        password: String,
        nickname: String,
        domain: String,
        recommenderCode: String,
        appId: String
    ): Flow<ApiResult<CommonResponse>> {
        return userRepository.signUp(
            email = email,
            password = password,
            nickname = nickname,
            domain = domain,
            recommenderCode = recommenderCode,
            appId = appId
        )
    }
}
