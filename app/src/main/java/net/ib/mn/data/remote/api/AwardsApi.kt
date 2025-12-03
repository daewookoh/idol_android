package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.AwardsCurrentResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * Awards API
 * 어워즈/이벤트 관련 API
 */
interface AwardsApi {

    /**
     * 현재 진행중인 어워즈 정보 조회
     * 플로팅 버튼 이미지 URL 포함
     */
    @GET("awards/current/")
    suspend fun getCurrent(): Response<AwardsCurrentResponse>
}
