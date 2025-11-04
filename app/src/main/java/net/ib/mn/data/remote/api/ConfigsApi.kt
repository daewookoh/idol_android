package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.ConfigStartupResponse
import net.ib.mn.data.remote.dto.TypeListResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * 앱 설정 관련 API
 * old 프로젝트의 ConfigsApi.kt와 일원화
 */
interface ConfigsApi {

    /**
     * 앱 시작 시 필요한 설정 정보 조회
     */
    @GET("configs/startup/")
    suspend fun getConfigStartup(): Response<ConfigStartupResponse>

    /**
     * 사용자별 설정 정보 조회
     */
    @GET("configs/self/")
    suspend fun getConfigSelf(): Response<String>

    /**
     * 타입 리스트 조회 (랭킹 탭 정보)
     */
    @GET("configs/typelist/")
    suspend fun getTypeList(): Response<TypeListResponse>
}
