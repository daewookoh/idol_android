package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.ChartIdolIdsResponse
import net.ib.mn.data.remote.dto.ChartRanksResponse
import net.ib.mn.data.remote.dto.CurrentChartResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

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

    /**
     * 특정 차트 코드의 아이돌 ID 리스트 가져오기
     *
     * old 프로젝트의 getChartIdolIds와 동일
     * 차트에 속한 아이돌들의 ID 리스트만 반환
     *
     * 사용 탭: Solo (개인), Group (그룹)
     *
     * @param code 차트 코드 (예: "PR_S_M", "PR_G_F")
     * @return Response<ChartIdolIdsResponse>
     */
    @GET("charts/idol_ids/")
    suspend fun getChartIdolIds(
        @Query("code") code: String
    ): Response<ChartIdolIdsResponse>

    /**
     * 특정 차트 코드의 누적 랭킹 가져오기
     *
     * old 프로젝트의 getChartRanks와 동일
     * 차트의 누적 집계 결과를 직접 반환 (idol_id, name, score, rank 포함)
     *
     * 사용 탭: Miracle (기적), Rookie (루키), HeartPick (하트픽), OnePick (원픽), HallOfFame (명예전당)
     *
     * @param code 차트 코드 (예: "PR_G_M", "HEARTPICK", "ONEPICK", "HOF")
     * @return Response<ChartRanksResponse>
     */
    @GET("charts/ranks/")
    suspend fun getChartRanks(
        @Query("code") code: String
    ): Response<ChartRanksResponse>

    /**
     * 명예전당 일일 데이터 가져오기
     *
     * old 프로젝트의 hofs/ API와 동일
     * 명예전당 일일 순위 데이터 조회
     *
     * @param params 쿼리 파라미터 맵 (code, created_at__gte, created_at__lt 등)
     * @return Response<ResponseBody> JSON 형식의 응답
     */
    @GET("hofs/")
    suspend fun getHofs(
        @retrofit2.http.QueryMap params: Map<String, String>
    ): Response<ResponseBody>

    /**
     * 아이돌이 속한 차트 코드별 아이돌 ID 리스트 조회
     *
     * old 프로젝트의 charts/list_per_idol/ API와 동일
     * 각 차트 코드에 속한 아이돌 ID 리스트를 반환
     * 최애 화면에서 순위 계산에 사용
     *
     * @return Response<ResponseBody> JSON 형식의 응답
     */
    @GET("charts/list_per_idol/")
    suspend fun getIdolChartCodes(): Response<ResponseBody>
}
