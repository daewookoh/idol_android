package net.ib.mn.data.repository

import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.model.TypeListModel
import net.ib.mn.data.remote.api.ConfigsApi
import net.ib.mn.data.remote.dto.ConfigSelfResponse
import net.ib.mn.data.remote.dto.ConfigStartupResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Config Repository 구현체
 *
 * Retrofit API를 사용하여 실제 네트워크 요청 수행
 *
 * 실시간 데이터 최적화:
 * - StateFlow를 통한 reactive data stream
 * - 캐시 업데이트 시 자동으로 모든 구독자에게 알림
 * - 메모리 캐시와 StateFlow 동기화
 */
class ConfigRepositoryImpl @Inject constructor(
    private val configsApi: ConfigsApi,
    private val preferencesManager: PreferencesManager
) : ConfigRepository {

    // typeList 캐시 (메모리 캐시)
    @Volatile
    private var cachedTypeList: List<TypeListModel>? = null

    // typeList StateFlow (실시간 업데이트용)
    private val _typeListFlow = MutableStateFlow<List<TypeListModel>>(emptyList())

    // MainChartModel 캐시 (메모리 캐시)
    @Volatile
    private var cachedMainChartModel: net.ib.mn.data.remote.dto.MainChartModel? = null

    // MainChartModel StateFlow (실시간 업데이트용)
    private val _mainChartModelFlow = MutableStateFlow<net.ib.mn.data.remote.dto.MainChartModel?>(null)

    // ChartObjects 캐시 (메모리 캐시)
    @Volatile
    private var cachedChartObjects: List<net.ib.mn.data.remote.dto.ChartModel>? = null

    // ChartObjects StateFlow (실시간 업데이트용)
    private val _chartObjectsFlow = MutableStateFlow<List<net.ib.mn.data.remote.dto.ChartModel>>(emptyList())

    override fun getConfigStartup(): Flow<ApiResult<ConfigStartupResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            android.util.Log.d("ConfigRepo", "========================================")
            android.util.Log.d("ConfigRepo", "🔵 Calling ConfigStartup API")
            android.util.Log.d("ConfigRepo", "========================================")

            val response = configsApi.getConfigStartup()

            android.util.Log.d("ConfigRepo", "📦 Response received:")
            android.util.Log.d("ConfigRepo", "  - HTTP Code: ${response.code()}")
            android.util.Log.d("ConfigRepo", "  - isSuccessful: ${response.isSuccessful}")
            android.util.Log.d("ConfigRepo", "  - Body null: ${response.body() == null}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // Raw JSON 응답 로그 (Gson 사용)
                try {
                    val gson = com.google.gson.Gson()
                    val jsonString = gson.toJson(body)
                    android.util.Log.d("ConfigRepo", "📄 Raw JSON Response:")
                    android.util.Log.d("ConfigRepo", jsonString)
                } catch (e: Exception) {
                    android.util.Log.e("ConfigRepo", "Failed to serialize: ${e.message}")
                }

                android.util.Log.d("ConfigRepo", "📋 Parsed body:")
                android.util.Log.d("ConfigRepo", "  - success: ${body.success}")
                android.util.Log.d("ConfigRepo", "  - data null: ${body.data == null}")

                if (body.data != null) {
                    android.util.Log.d("ConfigRepo", "  - data.badWords size: ${body.data.badWords?.size ?: 0}")
                    android.util.Log.d("ConfigRepo", "  - data.boardTags size: ${body.data.boardTags?.size ?: 0}")
                    android.util.Log.d("ConfigRepo", "  - data.noticeList length: ${body.data.noticeList?.length ?: 0}")
                }

                if (body.success) {
                    android.util.Log.d("ConfigRepo", "✅ ConfigStartup SUCCESS")
                    emit(ApiResult.Success(body))
                } else {
                    android.util.Log.e("ConfigRepo", "❌ API returned success=false")
                    android.util.Log.e("ConfigRepo", "This means server processed request but returned failure")
                    emit(ApiResult.Error(
                        exception = Exception("API returned success=false"),
                        code = response.code(),
                        message = "Server returned success=false"
                    ))
                }
            } else {
                android.util.Log.e("ConfigRepo", "❌ Response not successful or body null")
                android.util.Log.e("ConfigRepo", "  - Error body: ${response.errorBody()?.string()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            android.util.Log.e("ConfigRepo", "❌ HttpException: ${e.code()} - ${e.message()}", e)
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            android.util.Log.e("ConfigRepo", "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            android.util.Log.e("ConfigRepo", "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    override fun getConfigSelf(): Flow<ApiResult<ConfigSelfResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            android.util.Log.d("ConfigRepo", "========================================")
            android.util.Log.d("ConfigRepo", "🔵 Calling ConfigSelf API")
            android.util.Log.d("ConfigRepo", "========================================")

            val response = configsApi.getConfigSelf()

            android.util.Log.d("ConfigRepo", "📦 ConfigSelf Response:")
            android.util.Log.d("ConfigRepo", "  - HTTP Code: ${response.code()}")
            android.util.Log.d("ConfigRepo", "  - isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                android.util.Log.d("ConfigRepo", "✅ ConfigSelf SUCCESS")
                android.util.Log.d("ConfigRepo", "  - udpBroadcastUrl: ${body.udpBroadcastUrl}")
                android.util.Log.d("ConfigRepo", "  - udpStage: ${body.udpStage}")
                android.util.Log.d("ConfigRepo", "  - cdnUrl: ${body.cdnUrl}")

                emit(ApiResult.Success(body))
            } else {
                android.util.Log.e("ConfigRepo", "❌ ConfigSelf failed")
                android.util.Log.e("ConfigRepo", "  - Error body: ${response.errorBody()?.string()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            android.util.Log.e("ConfigRepo", "❌ HttpException: ${e.code()} - ${e.message()}", e)
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            android.util.Log.e("ConfigRepo", "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            android.util.Log.e("ConfigRepo", "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    /**
     * TypeList 조회 (캐시 우선)
     * startup에서 호출되어 캐시된 경우 API 호출하지 않음
     */
    override fun getTypeList(forceRefresh: Boolean): Flow<List<TypeListModel>> = flow {
        android.util.Log.d("API_RESPONSE", "========================================")
        android.util.Log.d("API_RESPONSE", "[ConfigRepository] getTypeList called")
        android.util.Log.d("API_RESPONSE", "  - forceRefresh: $forceRefresh")
        android.util.Log.d("API_RESPONSE", "  - cachedTypeList: ${cachedTypeList?.size ?: 0} items")

        // 캐시가 있고 forceRefresh가 false면 캐시 반환
        if (!forceRefresh && cachedTypeList != null) {
            android.util.Log.d("API_RESPONSE", "✓ Returning cached typeList (${cachedTypeList!!.size} items)")
            android.util.Log.d("API_RESPONSE", "========================================")
            emit(cachedTypeList!!)
            return@flow
        }

        // API 호출
        android.util.Log.d("API_RESPONSE", "Calling TypeList API: GET configs/typelist/")
        try {
            val response = configsApi.getTypeList()

            android.util.Log.d("API_RESPONSE", "Response Code: ${response.code()}")
            android.util.Log.d("API_RESPONSE", "Response Success: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    val typeListData = body.objects

                    android.util.Log.d("API_RESPONSE", "TypeList API Response:")
                    android.util.Log.d("API_RESPONSE", "Success: ${body.success}")
                    android.util.Log.d("API_RESPONSE", "Total types: ${typeListData.size}")

                    typeListData.forEachIndexed { index, type ->
                        android.util.Log.d("API_RESPONSE", "  [$index] id=${type.id}, name=${type.name}, type=${type.type}, isDivided=${type.isDivided}, isFemale=${type.isFemale}")
                    }

                    // 캐시 저장
                    cachedTypeList = typeListData
                    android.util.Log.d("API_RESPONSE", "✓ TypeList cached successfully")

                    emit(typeListData)
                } else {
                    android.util.Log.e("API_RESPONSE", "Error: API returned success=false")
                    emit(emptyList())
                }
            } else {
                android.util.Log.e("API_RESPONSE", "Error: HTTP ${response.code()}")
                android.util.Log.e("API_RESPONSE", "Error body: ${response.errorBody()?.string()}")
                emit(emptyList())
            }
        } catch (e: Exception) {
            android.util.Log.e("API_RESPONSE", "Exception: ${e.message}", e)
            emit(emptyList())
        }

        android.util.Log.d("API_RESPONSE", "========================================")
    }

    /**
     * TypeList StateFlow 노출 (실시간 업데이트)
     */
    override fun observeTypeList(): StateFlow<List<TypeListModel>> = _typeListFlow

    /**
     * 처리된 typeList를 캐시에 저장
     * StartupViewModel에서 API 응답을 가공한 후 캐시 업데이트용
     * StateFlow도 함께 업데이트하여 모든 구독자에게 알림
     */
    override fun setTypeListCache(typeList: List<TypeListModel>) {
        android.util.Log.d("API_RESPONSE", "========================================")
        android.util.Log.d("API_RESPONSE", "[ConfigRepository] setTypeListCache called")
        android.util.Log.d("API_RESPONSE", "  - typeList size: ${typeList.size}")

        cachedTypeList = typeList
        _typeListFlow.value = typeList // StateFlow 업데이트 -> 모든 구독자에게 자동 알림

        android.util.Log.d("API_RESPONSE", "✓ TypeList cache & StateFlow updated")
        android.util.Log.d("API_RESPONSE", "========================================")
    }

    /**
     * MainChartModel StateFlow 노출 (실시간 업데이트)
     */
    override fun observeMainChartModel(): StateFlow<net.ib.mn.data.remote.dto.MainChartModel?> = _mainChartModelFlow

    /**
     * MainChartModel 캐시에 저장
     * charts/current/ API 응답의 main 필드
     * StateFlow도 함께 업데이트하여 모든 구독자에게 알림
     */
    override fun setMainChartModel(mainChartModel: net.ib.mn.data.remote.dto.MainChartModel) {
        android.util.Log.d("API_RESPONSE", "========================================")
        android.util.Log.d("API_RESPONSE", "[ConfigRepository] setMainChartModel called")
        android.util.Log.d("API_RESPONSE", "  - males: ${mainChartModel.males?.size ?: 0}")
        android.util.Log.d("API_RESPONSE", "  - females: ${mainChartModel.females?.size ?: 0}")

        cachedMainChartModel = mainChartModel
        _mainChartModelFlow.value = mainChartModel // StateFlow 업데이트

        android.util.Log.d("API_RESPONSE", "✓ MainChartModel cache & StateFlow updated")
        android.util.Log.d("API_RESPONSE", "========================================")
    }

    /**
     * MainChartModel 캐시에서 가져오기
     */
    override fun getMainChartModel(): net.ib.mn.data.remote.dto.MainChartModel? {
        return cachedMainChartModel
    }

    /**
     * ChartObjects StateFlow 노출 (실시간 업데이트)
     */
    override fun observeChartObjects(): StateFlow<List<net.ib.mn.data.remote.dto.ChartModel>> = _chartObjectsFlow

    /**
     * ChartObjects 캐시에 저장
     * charts/current/ API 응답의 objects 필드
     * StateFlow도 함께 업데이트하여 모든 구독자에게 알림
     */
    override fun setChartObjects(chartObjects: List<net.ib.mn.data.remote.dto.ChartModel>) {
        android.util.Log.d("API_RESPONSE", "========================================")
        android.util.Log.d("API_RESPONSE", "[ConfigRepository] setChartObjects called")
        android.util.Log.d("API_RESPONSE", "  - objects size: ${chartObjects.size}")

        cachedChartObjects = chartObjects
        _chartObjectsFlow.value = chartObjects // StateFlow 업데이트

        android.util.Log.d("API_RESPONSE", "✓ ChartObjects cache & StateFlow updated")
        android.util.Log.d("API_RESPONSE", "========================================")
    }

    /**
     * ChartObjects 캐시에서 가져오기
     */
    override fun getChartObjects(): List<net.ib.mn.data.remote.dto.ChartModel>? {
        return cachedChartObjects
    }
}
