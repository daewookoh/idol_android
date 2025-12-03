package net.ib.mn.data.repository

import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.model.TypeListModel
import net.ib.mn.data.remote.api.ConfigsApi
import net.ib.mn.data.remote.dto.ConfigSelfResponse
import net.ib.mn.data.remote.dto.ConfigStartupResponse
import net.ib.mn.domain.model.ApiError
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.GCode
import net.ib.mn.domain.repository.ConfigRepository
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import net.ib.mn.util.logW
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
 * BaseRepository를 상속받아 공통 API 처리 로직 사용
 *
 * 실시간 데이터 최적화:
 * - StateFlow를 통한 reactive data stream
 * - 캐시 업데이트 시 자동으로 모든 구독자에게 알림
 * - 메모리 캐시와 StateFlow 동기화
 */
class ConfigRepositoryImpl @Inject constructor(
    private val configsApi: ConfigsApi,
    private val preferencesManager: PreferencesManager
) : BaseRepository(), ConfigRepository {

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

    // ConfigSelf 캐시 (메모리 캐시) - Old: ConfigModel
    @Volatile
    private var cachedConfigSelf: ConfigSelfResponse? = null

    override fun getConfigStartup(): Flow<ApiResult<ConfigStartupResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            logD("ConfigRepo", "🔵 Calling ConfigStartup API")

            val response = configsApi.getConfigStartup()

            logD("ConfigRepo", "📦 Response: HTTP ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // gcode 검사 (점검 상태 확인)
                if (GCode.isMaintenance(body.gcode)) {
                    logW("ConfigRepo", "⚠️ Server maintenance (gcode: ${body.gcode})")
                    emit(ApiResult.Error(ApiError.Maintenance(body.msg)))
                    return@flow
                }

                // gcode 에러 검사
                if (!GCode.isSuccess(body.gcode) && body.gcode != 0) {
                    logE("ConfigRepo", "❌ Business error (gcode: ${body.gcode})")
                    emit(ApiResult.Error(ApiError.fromGcode(body.gcode, body.msg)))
                    return@flow
                }

                if (body.success) {
                    logD("ConfigRepo", "✅ ConfigStartup SUCCESS")
                    emit(ApiResult.Success(body))
                } else {
                    logE("ConfigRepo", "❌ API returned success=false")
                    emit(ApiResult.Error(ApiError.Business(
                        gcode = body.gcode,
                        message = body.msg ?: "Server returned success=false"
                    )))
                }
            } else {
                logE("ConfigRepo", "❌ HTTP Error: ${response.code()}")
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            logE("ConfigRepo", "❌ HttpException: ${e.code()}", e)
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: IOException) {
            logE("ConfigRepo", "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(ApiError.Network(exception = e)))
        } catch (e: Exception) {
            logE("ConfigRepo", "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }

    override fun getConfigSelf(): Flow<ApiResult<ConfigSelfResponse>> = flow {
        // 캐시가 있으면 캐시 반환 (Old: ConfigModel은 앱 시작 시 한 번만 로드)
        cachedConfigSelf?.let {
            logD("ConfigRepo", "✓ Returning cached ConfigSelf")
            emit(ApiResult.Success(it))
            return@flow
        }

        emit(ApiResult.Loading)

        try {
            logD("ConfigRepo", "🔵 Calling ConfigSelf API")

            val response = configsApi.getConfigSelf()

            logD("ConfigRepo", "📦 Response: HTTP ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                logD("ConfigRepo", "✅ ConfigSelf SUCCESS")

                // 캐시 저장
                cachedConfigSelf = body

                emit(ApiResult.Success(body))
            } else {
                logE("ConfigRepo", "❌ HTTP Error: ${response.code()}")
                emit(ApiResult.Error(ApiError.fromHttpCode(response.code(), response.message())))
            }
        } catch (e: HttpException) {
            logE("ConfigRepo", "❌ HttpException: ${e.code()}", e)
            emit(ApiResult.Error(ApiError.fromHttpCode(e.code(), e.message())))
        } catch (e: IOException) {
            logE("ConfigRepo", "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(ApiError.Network(exception = e)))
        } catch (e: Exception) {
            logE("ConfigRepo", "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(ApiError.Unknown(exception = e)))
        }
    }

    /**
     * ConfigSelf 캐시에서 reportHeart 가져오기
     * Old: configModel.reportHeart
     */
    override fun getReportHeart(): Int {
        return cachedConfigSelf?.reportHeart ?: 0
    }

    /**
     * TypeList 조회 (캐시 우선)
     * startup에서 호출되어 캐시된 경우 API 호출하지 않음
     */
    override fun getTypeList(forceRefresh: Boolean): Flow<List<TypeListModel>> = flow {
        logD("API_RESPONSE", "========================================")
        logD("API_RESPONSE", "[ConfigRepository] getTypeList called")
        logD("API_RESPONSE", "  - forceRefresh: $forceRefresh")
        logD("API_RESPONSE", "  - cachedTypeList: ${cachedTypeList?.size ?: 0} items")

        // 캐시가 있고 forceRefresh가 false면 캐시 반환
        if (!forceRefresh && cachedTypeList != null) {
            logD("API_RESPONSE", "✓ Returning cached typeList (${cachedTypeList!!.size} items)")
            logD("API_RESPONSE", "========================================")
            emit(cachedTypeList!!)
            return@flow
        }

        // API 호출
        logD("API_RESPONSE", "Calling TypeList API: GET configs/typelist/")
        try {
            val response = configsApi.getTypeList()

            logD("API_RESPONSE", "Response Code: ${response.code()}")
            logD("API_RESPONSE", "Response Success: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    val typeListData = body.objects

                    logD("API_RESPONSE", "TypeList API Response:")
                    logD("API_RESPONSE", "Success: ${body.success}")
                    logD("API_RESPONSE", "Total types: ${typeListData.size}")

                    typeListData.forEachIndexed { index, type ->
                        logD("API_RESPONSE", "  [$index] id=${type.id}, name=${type.name}, type=${type.type}, isDivided=${type.isDivided}, isFemale=${type.isFemale}")
                    }

                    // 캐시 저장
                    cachedTypeList = typeListData
                    logD("API_RESPONSE", "✓ TypeList cached successfully")

                    emit(typeListData)
                } else {
                    logE("API_RESPONSE", "Error: API returned success=false")
                    emit(emptyList())
                }
            } else {
                logE("API_RESPONSE", "Error: HTTP ${response.code()}")
                logE("API_RESPONSE", "Error body: ${response.errorBody()?.string()}")
                emit(emptyList())
            }
        } catch (e: Exception) {
            logE("API_RESPONSE", "Exception: ${e.message}", e)
            emit(emptyList())
        }

        logD("API_RESPONSE", "========================================")
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
        logD("API_RESPONSE", "========================================")
        logD("API_RESPONSE", "[ConfigRepository] setTypeListCache called")
        logD("API_RESPONSE", "  - typeList size: ${typeList.size}")

        cachedTypeList = typeList
        _typeListFlow.value = typeList // StateFlow 업데이트 -> 모든 구독자에게 자동 알림

        logD("API_RESPONSE", "✓ TypeList cache & StateFlow updated")
        logD("API_RESPONSE", "========================================")
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
        logD("API_RESPONSE", "========================================")
        logD("API_RESPONSE", "[ConfigRepository] setMainChartModel called")
        logD("API_RESPONSE", "  - males: ${mainChartModel.males?.size ?: 0}")
        logD("API_RESPONSE", "  - females: ${mainChartModel.females?.size ?: 0}")

        cachedMainChartModel = mainChartModel
        _mainChartModelFlow.value = mainChartModel // StateFlow 업데이트

        logD("API_RESPONSE", "✓ MainChartModel cache & StateFlow updated")
        logD("API_RESPONSE", "========================================")
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
        logD("API_RESPONSE", "========================================")
        logD("API_RESPONSE", "[ConfigRepository] setChartObjects called")
        logD("API_RESPONSE", "  - objects size: ${chartObjects.size}")

        cachedChartObjects = chartObjects
        _chartObjectsFlow.value = chartObjects // StateFlow 업데이트

        logD("API_RESPONSE", "✓ ChartObjects cache & StateFlow updated")
        logD("API_RESPONSE", "========================================")
    }

    /**
     * ChartObjects 캐시에서 가져오기
     */
    override fun getChartObjects(): List<net.ib.mn.data.remote.dto.ChartModel>? {
        return cachedChartObjects
    }

    /**
     * 기본 차트 코드 가져오기 (앱 첫 실행 시 초기 탭 선택용)
     */
    override suspend fun getDefaultChartCode(): String? {
        return preferencesManager.defaultChartCode.first()
    }

    /**
     * 모든 캐시 데이터 삭제 (서버 URL 변경 시 사용)
     * 메모리 캐시와 StateFlow를 모두 초기화
     */
    override fun clearAllCache() {
        logD("ConfigRepo", "========================================")
        logD("ConfigRepo", "🗑️ Clearing all cache data")
        logD("ConfigRepo", "========================================")

        // 메모리 캐시 초기화
        cachedTypeList = null
        cachedMainChartModel = null
        cachedChartObjects = null
        cachedConfigSelf = null

        // StateFlow 초기화
        _typeListFlow.value = emptyList()
        _mainChartModelFlow.value = null
        _chartObjectsFlow.value = emptyList()

        logD("ConfigRepo", "✅ All cache cleared")
    }
}
