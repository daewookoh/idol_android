package net.ib.mn.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.ib.mn.data.local.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인증 정보 관리 Repository
 *
 * 역할:
 * 1. 인증 데이터 인메모리 캐싱 (email, domain, token)
 * 2. DataStore에 인증 정보 저장/로드
 * 3. AuthInterceptor에 동기적 Getter 제공
 *
 * 장점:
 * - 관심사 분리: AuthInterceptor는 HTTP 요청만, AuthRepository는 데이터 관리만
 * - ANR 방지: init 블록에서 runBlocking 제거
 * - 성능 향상: 인메모리 캐시로 매번 DataStore I/O 방지
 * - 테스트 용이성: AuthRepository를 독립적으로 모킹 가능
 */
@Singleton
class AuthRepository @Inject constructor(
    private val preferencesManager: PreferencesManager
) {

    // 인메모리 캐시 (프로세스 생명주기 동안 유지)
    @Volatile
    private var email: String? = null

    @Volatile
    private var domain: String? = null

    @Volatile
    private var token: String? = null

    /**
     * 로그인 시 인증 정보 설정 및 DataStore에 저장
     * StartUpViewModel, LoginViewModel 등에서 호출
     *
     * @param email 사용자 이메일
     * @param domain 로그인 도메인 (kakao, google, line, facebook, local)
     * @param token 액세스 토큰
     */
    suspend fun login(email: String, domain: String, token: String) {
        android.util.Log.d("USER_INFO", "[AuthRepository] login() called")
        android.util.Log.d("USER_INFO", "[AuthRepository]   - Email: $email")
        android.util.Log.d("USER_INFO", "[AuthRepository]   - Domain: $domain")
        android.util.Log.d("USER_INFO", "[AuthRepository]   - Token: ${token.take(20)}...")

        // 인메모리 캐시 설정
        this.email = email
        this.domain = domain
        this.token = token

        // DataStore에 비동기 저장
        preferencesManager.setLoginEmail(email)
        preferencesManager.setLoginDomain(domain)
        preferencesManager.setAccessToken(token)

        android.util.Log.d("USER_INFO", "[AuthRepository] ✓ Auth credentials saved to memory and DataStore")
    }

    /**
     * 로그아웃 시 인증 정보 초기화 및 DataStore에서 삭제
     *
     * NOTE: preferencesManager.clearAll()은 모든 데이터를 삭제하므로
     * 필요한 경우 개별 삭제 메서드를 사용하거나 clearAllExceptXXX() 메서드 사용
     */
    suspend fun logout() {
        android.util.Log.d("USER_INFO", "[AuthRepository] logout() called")

        // 인메모리 캐시 초기화
        this.email = null
        this.domain = null
        this.token = null

        // DataStore에서 인증 정보 삭제
        preferencesManager.setAccessToken("")
        preferencesManager.setLoginEmail("")
        preferencesManager.setLoginDomain("")

        android.util.Log.d("USER_INFO", "[AuthRepository] ✓ Auth credentials cleared from memory and DataStore")
    }

    /**
     * 동기적으로 이메일 반환
     * AuthInterceptor에서 호출 (OkHttp 백그라운드 스레드)
     *
     * 로직:
     * 1. 캐시가 있으면 바로 반환 (빠름)
     * 2. 캐시가 없으면 DataStore에서 동기적으로 로드 (프로세스 데스 직후)
     *
     * @return 이메일 또는 null
     */
    fun getEmail(): String? {
        // 캐시가 있으면 바로 반환
        if (email != null) return email

        // 캐시가 없으면 DataStore에서 동기적으로 로드
        // runBlocking은 OkHttp 백그라운드 스레드에서 실행되므로 안전함
        return runBlocking {
            val loaded = preferencesManager.loginEmail.first()
            if (loaded != null) {
                android.util.Log.d("USER_INFO", "[AuthRepository] getEmail() - Loaded from DataStore: $loaded")
            }
            email = loaded // 로드한 값을 캐시에 저장
            loaded
        }
    }

    /**
     * 동기적으로 도메인 반환
     * AuthInterceptor에서 호출 (OkHttp 백그라운드 스레드)
     *
     * @return 도메인 또는 null
     */
    fun getDomain(): String? {
        if (domain != null) return domain

        return runBlocking {
            val loaded = preferencesManager.loginDomain.first()
            if (loaded != null) {
                android.util.Log.d("USER_INFO", "[AuthRepository] getDomain() - Loaded from DataStore: $loaded")
            }
            domain = loaded
            loaded
        }
    }

    /**
     * 동기적으로 액세스 토큰 반환
     * AuthInterceptor에서 호출 (OkHttp 백그라운드 스레드)
     *
     * @return 액세스 토큰 또는 null
     */
    fun getAccessToken(): String? {
        if (token != null) return token

        return runBlocking {
            val loaded = preferencesManager.accessToken.first()
            if (loaded != null) {
                android.util.Log.d("USER_INFO", "[AuthRepository] getAccessToken() - Loaded from DataStore: ${loaded.take(20)}...")
            }
            token = loaded
            loaded
        }
    }

    /**
     * 인증 정보가 완전한지 확인
     * StartUpViewModel에서 사용
     *
     * @return 이메일, 도메인, 토큰이 모두 존재하면 true
     */
    fun hasValidCredentials(): Boolean {
        return getEmail() != null && getDomain() != null && getAccessToken() != null
    }

    /**
     * 인증 정보 존재 여부를 비동기적으로 확인 (DataStore에서)
     * ViewModel에서 Flow를 사용할 때 호출
     *
     * @return 이메일, 도메인, 토큰이 모두 존재하면 true
     */
    suspend fun hasValidCredentialsAsync(): Boolean {
        val savedEmail = preferencesManager.loginEmail.first()
        val savedDomain = preferencesManager.loginDomain.first()
        val savedToken = preferencesManager.accessToken.first()

        return savedEmail != null && savedDomain != null && savedToken != null
    }
}
