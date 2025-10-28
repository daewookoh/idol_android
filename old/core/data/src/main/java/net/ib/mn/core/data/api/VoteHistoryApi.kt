package net.ib.mn.core.data.api

import net.ib.mn.core.data.model.ObjectsBaseDataModel
import net.ib.mn.core.data.model.VoteCertificateResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface VoteHistoryApi {
    // idol 아이디 null 이면 목록 아니면 상세
    @GET("vote_histories/proof/today")
    suspend fun getTodayCertificate(
        @Query("idol_id") idolId: Long? = null,
    ): ObjectsBaseDataModel<List<VoteCertificateResponse>>
}