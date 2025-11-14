package net.ib.mn.data.remote.interceptor

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.ib.mn.BuildConfig
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.util.Constants
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale
import javax.inject.Inject

/**
 * 인증 Interceptor
 *
 * Authorization 헤더를 자동으로 추가 (Basic 인증)
 * NOTE: old 프로젝트와 동일하게 "email:domain:token" 형식의 Basic 인증 사용
 * - 로그인 성공 시: setAuthCredentials()로 email, domain, token 설정
 * - 앱 시작 시: StartUpViewModel에서 로드하여 setAuthCredentials() 호출
 * - 프로세스 재시작 시: init 블록에서 DataStore로부터 자동 복원
 */
class AuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : Interceptor {

    @Volatile
    private var email: String? = null

    @Volatile
    private var domain: String? = null

    @Volatile
    private var token: String? = null

    init {
        // 프로세스 재시작 시 DataStore에서 인증 정보 자동 복원
        // 이렇게 하면 앱이 백그라운드에서 종료된 후 재실행될 때도 인증 정보가 유지됨
        runBlocking {
            val savedEmail = preferencesManager.loginEmail.first()
            val savedDomain = preferencesManager.loginDomain.first()
            val savedToken = preferencesManager.accessToken.first()

            if (savedEmail != null && savedDomain != null && savedToken != null) {
                android.util.Log.d("USER_INFO", "[AuthInterceptor] init: Restoring credentials from DataStore")
                android.util.Log.d("USER_INFO", "[AuthInterceptor]   - Email: $savedEmail")
                android.util.Log.d("USER_INFO", "[AuthInterceptor]   - Domain: $savedDomain")
                android.util.Log.d("USER_INFO", "[AuthInterceptor]   - Token: ${savedToken.take(20)}...")

                setAuthCredentials(savedEmail, savedDomain, savedToken)
            } else {
                android.util.Log.d("USER_INFO", "[AuthInterceptor] init: No saved credentials found in DataStore")
            }
        }
    }

    /**
     * old 프로젝트와 동일: email, domain, token을 설정
     */
    fun setAuthCredentials(email: String?, domain: String?, token: String?) {
        android.util.Log.d("USER_INFO", "[AuthInterceptor] setAuthCredentials() called")
        android.util.Log.d("USER_INFO", "[AuthInterceptor]   - Email: $email")
        android.util.Log.d("USER_INFO", "[AuthInterceptor]   - Domain: $domain")
        android.util.Log.d("USER_INFO", "[AuthInterceptor]   - Token: ${if (token != null) "${token.take(20)}..." else "null"}")

        this.email = email
        this.domain = domain
        this.token = token

        android.util.Log.d("USER_INFO", "[AuthInterceptor] ✓ Auth credentials updated")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        val method = originalRequest.method

        // 디버깅: 모든 요청 로깅
        android.util.Log.d("AuthInterceptor", "========================================")
        android.util.Log.d("AuthInterceptor", "Request: $method $url")
        android.util.Log.d("AuthInterceptor", "  Current auth state: email=$email, domain=$domain, token=${token?.take(10)}")
        android.util.Log.d("AuthInterceptor", "========================================")

        // User-Agent 헤더 구성
        val systemUserAgent = System.getProperty("http.agent") ?: ""
        val packageName = context.packageName
        val appVersion = "10.10.0"
        val versionCode = BuildConfig.VERSION_CODE
        val userAgent = "$systemUserAgent ($packageName/$appVersion/$versionCode)"
        val systemLanguage = Locale.getDefault().language

        val requestBuilder = originalRequest.newBuilder()
            .header("User-Agent", userAgent)
            .header("X-HTTP-APPID", Constants.APP_ID)
            .header("X-HTTP-VERSION", appVersion)
            .header("X-HTTP-NATION", systemLanguage)

        // Old 프로젝트와 동일: POST/DELETE 요청 시 X-Nonce 헤더 추가
        if (method == "POST" || method == "DELETE") {
            requestBuilder.addHeader("X-Nonce", System.nanoTime().toString())
        }

        // 인증이 필요 없는 엔드포인트 목록 (정확한 경로 매칭)
        val noAuthEndpoints = listOf(
            "/users/",              // 회원가입 (POST /users/)
            "/users/email_signin/", // 로그인
            "/users/validate/",     // 사용자 검증
            "/users/find_id/",      // 아이디 찾기
            "/users/find_passwd/",  // 비밀번호 찾기
        )

        // URL 경로만 정확하게 비교 (쿼리 파라미터 제외)
        val path = originalRequest.url.encodedPath
        val requiresAuth = !noAuthEndpoints.contains(path)

        if (originalRequest.header("Authorization") == null && requiresAuth) {
            if (email != null && domain != null && token != null) {
                // old 프로젝트와 동일: "email:domain:token" 형식의 Basic 인증
                val credential = "$email:$domain:$token"
                val authHeader = "Basic ${Base64.encodeToString(credential.toByteArray(), Base64.NO_WRAP)}"

                android.util.Log.d("USER_INFO", "[AuthInterceptor] Adding Authorization header to request")
                android.util.Log.d("USER_INFO", "[AuthInterceptor]   - URL: $url")
                android.util.Log.d("USER_INFO", "[AuthInterceptor]   - Auth type: Basic")
                android.util.Log.d("USER_INFO", "[AuthInterceptor]   - Credential: $email:$domain:${token?.take(10)}...")

                requestBuilder.header("Authorization", authHeader)
            } else {
                android.util.Log.w("USER_INFO", "[AuthInterceptor] ⚠️ No auth credentials available")
                android.util.Log.w("USER_INFO", "[AuthInterceptor]   - URL: $url")
                android.util.Log.w("USER_INFO", "[AuthInterceptor]   - Email: $email, Domain: $domain, Token: ${if (token != null) "present" else "null"}")
                android.util.Log.w("USER_INFO", "[AuthInterceptor]   - This will likely result in 401 Unauthorized")
            }
        } else if (!requiresAuth) {
            android.util.Log.d("USER_INFO", "[AuthInterceptor] Skipping auth for public endpoint: $url")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        // 401 에러 발생 시 로그
        if (response.code == 401) {
            android.util.Log.e("USER_INFO", "[AuthInterceptor] ❌ 401 Unauthorized response received")
            android.util.Log.e("USER_INFO", "[AuthInterceptor]   - URL: $url")
            android.util.Log.e("USER_INFO", "[AuthInterceptor]   - Email was: $email")
            android.util.Log.e("USER_INFO", "[AuthInterceptor]   - Domain was: $domain")
            android.util.Log.e("USER_INFO", "[AuthInterceptor]   - Token was: ${if (token != null) "${token?.take(20)}..." else "null"}")
        }

        return response
    }
}
