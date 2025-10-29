package net.ib.mn.domain.usecase

import net.ib.mn.data.remote.dto.SignInResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 로그인 UseCase
 *
 * 이메일/소셜 로그인을 수행합니다.
 * old 프로젝트의 UsersRepository.signIn() 로직을 UseCase 패턴으로 구현.
 *
 * @param domain 로그인 도메인 (email, kakao, google, line, facebook)
 * @param email 이메일 (소셜 로그인의 경우 "{id}@kakao.com" 형식)
 * @param password 비밀번호 (소셜 로그인의 경우 access token)
 * @param deviceKey 푸시 키 (FCM token)
 * @param gmail Gmail 계정 (없으면 device UUID 사용)
 * @param deviceId 디바이스 고유 ID (UUID)
 * @param appId 앱 ID (Constants.APP_ID)
 */
class SignInUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(
        domain: String,
        email: String,
        password: String,
        deviceKey: String,
        gmail: String,
        deviceId: String,
        appId: String
    ): Flow<ApiResult<SignInResponse>> {
        return userRepository.signIn(
            domain = domain,
            email = email,
            password = password,
            deviceKey = deviceKey,
            gmail = gmail,
            deviceId = deviceId,
            appId = appId
        )
    }
}
