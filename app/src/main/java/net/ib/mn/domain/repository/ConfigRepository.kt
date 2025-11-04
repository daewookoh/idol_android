package net.ib.mn.domain.repository

import net.ib.mn.data.model.TypeListModel
import net.ib.mn.data.remote.dto.ConfigSelfResponse
import net.ib.mn.data.remote.dto.ConfigStartupResponse
import net.ib.mn.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * Config Repository 인터페이스
 *
 * 앱 설정 관련 데이터 접근 추상화
 */
interface ConfigRepository {

    /**
     * 앱 시작 설정 정보 조회
     *
     * @return Flow<ApiResult<ConfigStartupResponse>>
     */
    fun getConfigStartup(): Flow<ApiResult<ConfigStartupResponse>>

    /**
     * 사용자별 설정 정보 조회
     *
     * @return Flow<ApiResult<ConfigSelfResponse>>
     */
    fun getConfigSelf(): Flow<ApiResult<ConfigSelfResponse>>

    /**
     * 타입 리스트 조회 (캐시 우선)
     * startup에서 이미 호출되었다면 캐시된 값을 반환
     *
     * @param forceRefresh true면 캐시 무시하고 API 호출
     * @return Flow<List<TypeListModel>>
     */
    fun getTypeList(forceRefresh: Boolean = false): Flow<List<TypeListModel>>

    /**
     * 처리된 typeList를 캐시에 저장
     * StartupViewModel에서 API 응답을 가공한 후 캐시 업데이트용
     *
     * @param typeList 처리된 typeList
     */
    fun setTypeListCache(typeList: List<TypeListModel>)

    /**
     * MainChartModel 캐시에 저장
     * charts/current/ API 응답의 main 필드
     *
     * @param mainChartModel MainChartModel
     */
    fun setMainChartModel(mainChartModel: net.ib.mn.data.remote.dto.MainChartModel)

    /**
     * MainChartModel 캐시에서 가져오기
     *
     * @return MainChartModel?
     */
    fun getMainChartModel(): net.ib.mn.data.remote.dto.MainChartModel?

    /**
     * ChartObjects 캐시에 저장
     * charts/current/ API 응답의 objects 필드 (MIRACLE, ROOKIE 등)
     *
     * @param chartObjects List<ChartModel>
     */
    fun setChartObjects(chartObjects: List<net.ib.mn.data.remote.dto.ChartModel>)

    /**
     * ChartObjects 캐시에서 가져오기
     *
     * @return List<ChartModel>?
     */
    fun getChartObjects(): List<net.ib.mn.data.remote.dto.ChartModel>?
}
