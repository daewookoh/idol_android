package net.ib.mn.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 인증 Interceptor
 *
 * Authorization 헤더를 자동으로 추가
 * NOTE: 토큰은 PreferencesManager(DataStore)에서 관리됨
 * - 로그인 성공 시: preferencesManager.setAccessToken()으로 저장
 * - 앱 시작 시: StartUpViewModel에서 토큰을 로드하여 setToken() 호출
 */
class AuthInterceptor : Interceptor {

    @Volatile
    private var token: String? = null

    fun setToken(newToken: String?) {
        token = newToken
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // HTTP-X-APPID, HTTP-X-VERSION 헤더 추가 (서버 요구사항)
        val requestBuilder = originalRequest.newBuilder()
            .header("HTTP-X-APPID", "test_android")
            .header("HTTP-X-VERSION", "10.10.99")

        // Authorization 헤더가 없고 토큰이 있으면 추가
        if (originalRequest.header("Authorization") == null) {
            token?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
