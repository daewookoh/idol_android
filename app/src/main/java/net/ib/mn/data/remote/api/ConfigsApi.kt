package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.ConfigSelfResponse
import net.ib.mn.data.remote.dto.ConfigStartupResponse
import net.ib.mn.data.remote.dto.TypeListResponse
import okhttp3.ResponseBody
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
     * UDP URL, CDN URL 등 사용자별 설정 포함
     */
    @GET("configs/self/")
    suspend fun getConfigSelf(): Response<ConfigSelfResponse>

    /**
     * 타입 리스트 조회 (랭킹 탭 정보)
     */
    @GET("configs/typelist/")
    suspend fun getTypeList(): Response<TypeListResponse>

    /**
     * In-app 배너 조회
     * old 프로젝트와 동일하게 ResponseBody로 받아서 직접 파싱
     */
    @GET("idol_supports/inapp_banner/")
    suspend fun getInAppBanner(): Response<ResponseBody>
}
