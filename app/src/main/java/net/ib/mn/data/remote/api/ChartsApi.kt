package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.CurrentChartResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * Charts API
 *
 * 차트 관련 API 엔드포인트
 * CELEB이 아닌 앱에서 사용
 */
interface ChartsApi {

    /**
     * 현재 진행중인 차트 정보 가져오기
     *
     * old 프로젝트와 동일
     * CELEB이 아닌 앱에서 랭킹 탭 구성에 사용
     *
     * @return Response<CurrentChartResponse>
     */
    @GET("charts/current/")
    suspend fun getChartsCurrent(): Response<CurrentChartResponse>
}
