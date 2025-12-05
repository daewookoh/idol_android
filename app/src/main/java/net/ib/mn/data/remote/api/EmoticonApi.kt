package net.ib.mn.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 이모티콘 API
 */
interface EmoticonApi {

    /**
     * 이모티콘 세트 목록 조회
     */
    @GET("emoticonset/")
    suspend fun getEmoticonSets(): Response<ResponseBody>

    /**
     * 특정 이모티콘 세트 상세 조회
     */
    @GET("emoticonset/{id}/")
    suspend fun getEmoticonSet(
        @Path("id") emoticonSetId: Int
    ): Response<ResponseBody>
}
