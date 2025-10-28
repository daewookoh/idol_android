package com.example.idol_android.domain.repository

import com.example.idol_android.data.remote.dto.TimezoneUpdateResponse
import com.example.idol_android.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow

interface UtilityRepository {
    fun updateTimezone(timezone: String): Flow<ApiResult<TimezoneUpdateResponse>>
}
