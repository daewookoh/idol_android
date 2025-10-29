package net.ib.mn.domain.usecase

import net.ib.mn.data.remote.dto.ValidateResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 사용자 검증 UseCase
 *
 * 이메일이나 닉네임으로 회원 여부를 확인합니다.
 * old 프로젝트의 UsersRepository.validate() 로직을 UseCase 패턴으로 구현.
 *
 * Response 처리:
 * - success = true: 회원이 존재함 (기존 회원)
 * - success = false: 회원이 존재하지 않음 (신규 회원가입 필요)
 *
 * @param type 검증 타입 ("email", "nickname")
 * @param value 검증할 값 (이메일 또는 닉네임)
 * @param appId 앱 ID
 */
class ValidateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(
        type: String,
        value: String,
        appId: String
    ): Flow<ApiResult<ValidateResponse>> {
        return userRepository.validateUser(type, value, appId)
    }
}
