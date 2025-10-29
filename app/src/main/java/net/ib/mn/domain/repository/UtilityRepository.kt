package net.ib.mn.domain.repository

import net.ib.mn.data.remote.dto.TimezoneUpdateResponse
import net.ib.mn.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow

interface UtilityRepository {
    fun updateTimezone(timezone: String): Flow<ApiResult<TimezoneUpdateResponse>>
}
