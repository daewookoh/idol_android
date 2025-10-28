package com.example.idol_android.data.remote.api

import com.example.idol_android.data.remote.dto.ConfigSelfResponse
import com.example.idol_android.data.remote.dto.ConfigStartupResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * 앱 설정 관련 API
 */
interface ConfigApi {

    /**
     * 앱 시작 시 필요한 설정 정보 조회
     */
    @GET("configs/startup/")
    suspend fun getConfigStartup(): Response<ConfigStartupResponse>

    /**
     * 사용자별 설정 정보 조회
     */
    @GET("configs/self/")
    suspend fun getConfigSelf(
        @Header("Authorization") authorization: String
    ): Response<ConfigSelfResponse>
}
