package net.ib.mn.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Trends API (이붙그램)
 */
interface TrendsApi {

    /**
     * 이붙그램 목록 (개인/그룹 누적순위)
     */
    @GET("trends/recent/")
    suspend fun getRecent(
        @Query("idol_id") idolId: Int,
        @Query("code") chartCode: String? = null,
        @Query("offset") offset: Int? = null
    ): Response<ResponseBody>
}
