package net.ib.mn.data.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ib.mn.data.remote.api.ChartsApi
import net.ib.mn.data.remote.api.IdolApi
import net.ib.mn.data.remote.dto.AggregateRankModel
import net.ib.mn.data.remote.dto.VoteResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ranking Repository 구현체
 */
@Singleton
class RankingRepositoryImpl @Inject constructor(
    private val chartsApi: ChartsApi,
    private val idolApi: IdolApi
) : RankingRepository {

    override fun getChartIdolIds(code: String): Flow<ApiResult<List<Int>>> = flow {
        emit(ApiResult.Loading)

        try {
            android.util.Log.d("RankingRepo", "========================================")
            android.util.Log.d("RankingRepo", "🔵 Calling getChartIdolIds API (charts/idol_ids/)")
            android.util.Log.d("RankingRepo", "  - code: $code")
            android.util.Log.d("RankingRepo", "========================================")

            val response = chartsApi.getChartIdolIds(code)

            android.util.Log.d("RankingRepo", "📦 Response received:")
            android.util.Log.d("RankingRepo", "  - HTTP Code: ${response.code()}")
            android.util.Log.d("RankingRepo", "  - isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                android.util.Log.d("RankingRepo", "📋 Parsed body:")
                android.util.Log.d("RankingRepo", "  - success: ${body.success}")
                android.util.Log.d("RankingRepo", "  - data size: ${body.data?.size ?: 0}")

                if (body.success && body.data != null) {
                    android.util.Log.d("RankingRepo", "✅ getChartIdolIds SUCCESS")
                    android.util.Log.d("RankingRepo", "  - Idol IDs: ${body.data.take(10)}...")
                    emit(ApiResult.Success(body.data))
                } else {
                    android.util.Log.e("RankingRepo", "❌ API returned success=false or null data")
                    emit(ApiResult.Error(
                        exception = Exception(body.msg ?: "API returned success=false"),
                        code = response.code(),
                        message = body.msg ?: "Unknown error"
                    ))
                }
            } else {
                android.util.Log.e("RankingRepo", "❌ Response not successful or body null")
                android.util.Log.e("RankingRepo", "  - Error body: ${response.errorBody()?.string()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            android.util.Log.e("RankingRepo", "❌ HttpException: ${e.code()} - ${e.message()}", e)
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            android.util.Log.e("RankingRepo", "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            android.util.Log.e("RankingRepo", "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    override fun getChartRanks(code: String): Flow<ApiResult<List<AggregateRankModel>>> = flow {
        emit(ApiResult.Loading)

        try {
            android.util.Log.d("RankingRepo", "========================================")
            android.util.Log.d("RankingRepo", "🟢 Calling getChartRanks API (charts/ranks/)")
            android.util.Log.d("RankingRepo", "  - code: $code")
            android.util.Log.d("RankingRepo", "========================================")

            val response = chartsApi.getChartRanks(code)

            android.util.Log.d("RankingRepo", "📦 Response received:")
            android.util.Log.d("RankingRepo", "  - HTTP Code: ${response.code()}")
            android.util.Log.d("RankingRepo", "  - isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                android.util.Log.d("RankingRepo", "📋 Parsed body:")
                android.util.Log.d("RankingRepo", "  - success: ${body.success}")
                android.util.Log.d("RankingRepo", "  - objects size: ${body.objects?.size ?: 0}")

                if (body.success && body.objects != null) {
                    android.util.Log.d("RankingRepo", "✅ getChartRanks SUCCESS")
                    android.util.Log.d("RankingRepo", "  - Ranks: ${body.objects.take(5)}")
                    emit(ApiResult.Success(body.objects))
                } else {
                    android.util.Log.e("RankingRepo", "❌ API returned success=false or null data")
                    emit(ApiResult.Error(
                        exception = Exception(body.msg ?: "API returned success=false"),
                        code = response.code(),
                        message = body.msg ?: "Unknown error"
                    ))
                }
            } else {
                android.util.Log.e("RankingRepo", "❌ Response not successful or body null")
                android.util.Log.e("RankingRepo", "  - Error body: ${response.errorBody()?.string()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            android.util.Log.e("RankingRepo", "❌ HttpException: ${e.code()} - ${e.message()}", e)
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            android.util.Log.e("RankingRepo", "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            android.util.Log.e("RankingRepo", "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    override fun voteIdol(idolId: Int, heart: Long): Flow<ApiResult<VoteResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            android.util.Log.d("RankingRepo", "========================================")
            android.util.Log.d("RankingRepo", "💗 Calling voteIdol API (POST idols/give_heart/)")
            android.util.Log.d("RankingRepo", "  - idolId: $idolId")
            android.util.Log.d("RankingRepo", "  - heart: $heart")
            android.util.Log.d("RankingRepo", "========================================")

            // old 프로젝트와 동일: {"idol_id": "123", "number": 100}
            val response = idolApi.voteIdol(
                net.ib.mn.data.remote.dto.VoteRequest(
                    idolId = idolId.toString(),
                    number = heart
                )
            )

            android.util.Log.d("RankingRepo", "📦 Response received:")
            android.util.Log.d("RankingRepo", "  - HTTP Code: ${response.code()}")
            android.util.Log.d("RankingRepo", "  - isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                android.util.Log.d("RankingRepo", "📋 Parsed body:")
                android.util.Log.d("RankingRepo", "  - success: ${body.success}")
                android.util.Log.d("RankingRepo", "  - msg: ${body.msg}")
                android.util.Log.d("RankingRepo", "  - bonusHeart: ${body.bonusHeart}")

                if (body.success) {
                    android.util.Log.d("RankingRepo", "✅ voteIdol SUCCESS")
                    emit(ApiResult.Success(body))
                } else {
                    android.util.Log.e("RankingRepo", "❌ API returned success=false")
                    emit(ApiResult.Error(
                        exception = Exception(body.msg ?: "Vote failed"),
                        code = response.code(),
                        message = body.msg ?: "Unknown error"
                    ))
                }
            } else {
                android.util.Log.e("RankingRepo", "❌ Response not successful or body null")
                android.util.Log.e("RankingRepo", "  - Error body: ${response.errorBody()?.string()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            android.util.Log.e("RankingRepo", "❌ HttpException: ${e.code()} - ${e.message()}", e)
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            android.util.Log.e("RankingRepo", "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            android.util.Log.e("RankingRepo", "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    override fun getHofs(code: String, historyParam: String?): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)

        try {
            android.util.Log.d("RankingRepo", "========================================")
            android.util.Log.d("RankingRepo", "🟣 Calling getHofs API (hofs/)")
            android.util.Log.d("RankingRepo", "  - code: $code")
            android.util.Log.d("RankingRepo", "  - historyParam: $historyParam")
            android.util.Log.d("RankingRepo", "========================================")

            // Query parameter 맵 생성
            val params = mutableMapOf<String, String>()
            params["code"] = code

            // historyParam을 URI로 파싱하여 개별 쿼리 파라미터로 분해
            // OLD 프로젝트 HofsRepositoryImpl과 동일한 로직
            historyParam?.let {
                android.util.Log.d("RankingRepo", "🔍 Parsing historyParam: $it")
                // 앞에 ? 가 있어야 parse 되므로 ?를 붙여서 parse
                val uri = Uri.parse("?$it")
                uri.queryParameterNames.forEach { key ->
                    val value = uri.getQueryParameter(key)
                    if (value?.isNotEmpty() == true) {
                        params[key] = value
                        android.util.Log.d("RankingRepo", "  ✓ Added param: $key = $value")
                    }
                }
            }

            android.util.Log.d("RankingRepo", "📤 Final params: $params")

            val response = chartsApi.getHofs(params)

            android.util.Log.d("RankingRepo", "📦 Response received:")
            android.util.Log.d("RankingRepo", "  - HTTP Code: ${response.code()}")
            android.util.Log.d("RankingRepo", "  - isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val jsonString = response.body()!!.string()

                android.util.Log.d("RankingRepo", "✅ getHofs SUCCESS")
                android.util.Log.d("RankingRepo", "  - JSON length: ${jsonString.length}")
                android.util.Log.d("RankingRepo", "  - JSON preview: ${jsonString.take(200)}")

                emit(ApiResult.Success(jsonString))
            } else {
                android.util.Log.e("RankingRepo", "❌ Response not successful or body null")
                android.util.Log.e("RankingRepo", "  - Error body: ${response.errorBody()?.string()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            android.util.Log.e("RankingRepo", "❌ HttpException: ${e.code()} - ${e.message()}", e)
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            android.util.Log.e("RankingRepo", "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            android.util.Log.e("RankingRepo", "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    override fun getIdolChartCodes(): Flow<ApiResult<Map<String, List<String>>>> = flow {
        emit(ApiResult.Loading)

        try {
            android.util.Log.d("RankingRepo", "========================================")
            android.util.Log.d("RankingRepo", "🟡 Calling getIdolChartCodes API (charts/list_per_idol/)")
            android.util.Log.d("RankingRepo", "========================================")

            val response = chartsApi.getIdolChartCodes()

            android.util.Log.d("RankingRepo", "📦 Response received:")
            android.util.Log.d("RankingRepo", "  - HTTP Code: ${response.code()}")
            android.util.Log.d("RankingRepo", "  - isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val jsonString = response.body()!!.string()

                android.util.Log.d("RankingRepo", "📋 JSON Response:")
                android.util.Log.d("RankingRepo", jsonString)

                // JSON 파싱
                val gson = com.google.gson.Gson()
                val responseType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                val responseMap: Map<String, Any> = gson.fromJson(jsonString, responseType)

                // API 응답에서 object 필드 확인 (OLD 프로젝트와 동일)
                @Suppress("UNCHECKED_CAST")
                val objectMap = responseMap["object"] as? Map<String, List<String>> ?: emptyMap()

                android.util.Log.d("RankingRepo", "✅ getIdolChartCodes SUCCESS")
                android.util.Log.d("RankingRepo", "  - Idol count: ${objectMap.size}")
                android.util.Log.d("RankingRepo", "  - Sample data: ${objectMap.entries.take(5).associate { it.key to it.value }}")

                emit(ApiResult.Success(objectMap))
            } else {
                android.util.Log.e("RankingRepo", "❌ Response not successful or body null")
                android.util.Log.e("RankingRepo", "  - Error body: ${response.errorBody()?.string()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            android.util.Log.e("RankingRepo", "❌ HttpException: ${e.code()} - ${e.message()}", e)
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            android.util.Log.e("RankingRepo", "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            android.util.Log.e("RankingRepo", "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }
}
