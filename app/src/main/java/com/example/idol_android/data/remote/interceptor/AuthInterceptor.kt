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

        // 이미 Authorization 헤더가 있으면 그대로 진행
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        // 토큰이 없으면 그대로 진행
        val currentToken = token ?: return chain.proceed(originalRequest)

        // Authorization 헤더 추가
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $currentToken")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
