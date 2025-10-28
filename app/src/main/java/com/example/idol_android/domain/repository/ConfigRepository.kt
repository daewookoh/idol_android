package com.example.idol_android.domain.repository

import com.example.idol_android.data.remote.dto.ConfigSelfResponse
import com.example.idol_android.data.remote.dto.ConfigStartupResponse
import com.example.idol_android.domain.model.ApiResult
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
