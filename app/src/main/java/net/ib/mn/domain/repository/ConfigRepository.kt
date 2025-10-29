package net.ib.mn.domain.repository

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
}
