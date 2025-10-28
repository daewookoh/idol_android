package com.example.idol_android.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 인증 Interceptor
 *
 * Authorization 헤더를 자동으로 추가
 * TODO: 실제 토큰 관리 로직 구현 (DataStore 또는 AccountManager)
 */
class AuthInterceptor : Interceptor {

    @Volatile
    private var token: String? = null

    fun setToken(newToken: String?) {
        token = newToken
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // TODO: DAEWOO
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
