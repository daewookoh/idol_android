package net.ib.mn.data.repository

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.ChartsApi
import net.ib.mn.data.remote.api.IdolApi
import net.ib.mn.data.remote.dto.AggregateRankModel
import net.ib.mn.data.remote.dto.VoteResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RankingRepository 구현체
 *
 * BaseRepository의 safeApiCall + safeApiCallWithJsonString 패턴 사용
 */
@Singleton
class RankingRepositoryImpl @Inject constructor(
    private val chartsApi: ChartsApi,
    private val idolApi: IdolApi
) : BaseRepository(), RankingRepository {

    override fun getChartIdolIds(code: String): Flow<ApiResult<List<Int>>> =
        safeApiCall { chartsApi.getChartIdolIds(code) }
            .extractList { response ->
                if (response.success) response.data else null
            }

    override fun getChartRanks(code: String): Flow<ApiResult<List<AggregateRankModel>>> =
        safeApiCall { chartsApi.getChartRanks(code) }
            .extractList { response ->
                if (response.success) response.objects else null
            }

    override fun voteIdol(idolId: Int, heart: Long): Flow<ApiResult<VoteResponse>> =
        safeApiCall {
            idolApi.voteIdol(
                net.ib.mn.data.remote.dto.VoteRequest(
                    idolId = idolId.toString(),
                    number = heart
                )
            )
        }.validateSuccess()

    override fun getHofs(code: String, historyParam: String?): Flow<ApiResult<String>> =
        safeApiCallWithJsonString(
            apiCall = {
                val params = mutableMapOf<String, String>()
                params["code"] = code

                historyParam?.let {
                    val uri = Uri.parse("?$it")
                    uri.queryParameterNames.forEach { key ->
                        val value = uri.getQueryParameter(key)
                        if (value?.isNotEmpty() == true) {
                            params[key] = value
                        }
                    }
                }

                chartsApi.getHofs(params)
            },
            parser = { json -> json }
        )

    override fun getIdolChartCodes(): Flow<ApiResult<Map<String, List<String>>>> =
        safeApiCallWithJsonString(
            apiCall = { chartsApi.getIdolChartCodes() },
            parser = { json ->
                val gson = Gson()
                val responseType = object : TypeToken<Map<String, Any>>() {}.type
                val responseMap: Map<String, Any> = gson.fromJson(json, responseType)

                @Suppress("UNCHECKED_CAST")
                responseMap["object"] as? Map<String, List<String>> ?: emptyMap()
            }
        )

    override fun getVotesTop100(idolId: Int?): Flow<ApiResult<String>> =
        safeApiCallWithJsonString(
            apiCall = { chartsApi.getVotesTop100() },
            parser = { json -> json }
        )
}
