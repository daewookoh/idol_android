package net.ib.mn.data.remote.interceptor

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import net.ib.mn.BuildConfig
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
 */
class AuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    @Volatile
    private var email: String? = null

    @Volatile
    private var domain: String? = null

    @Volatile
    private var token: String? = null

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

    /**
     * 하위 호환성을 위한 메서드 (deprecated)
     */
    @Deprecated("Use setAuthCredentials instead")
    fun setToken(newToken: String?) {
        this.token = newToken
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        val method = originalRequest.method

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

        if (originalRequest.header("Authorization") == null) {
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
