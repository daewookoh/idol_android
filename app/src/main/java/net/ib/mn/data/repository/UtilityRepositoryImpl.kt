package net.ib.mn.data.repository

import net.ib.mn.data.remote.api.UtilityApi
import net.ib.mn.data.remote.dto.TimezoneUpdateResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UtilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class UtilityRepositoryImpl @Inject constructor(
    private val utilityApi: UtilityApi
) : UtilityRepository {

    override fun updateTimezone(timezone: String): Flow<ApiResult<TimezoneUpdateResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val token = "Bearer YOUR_TOKEN_HERE"
            val body = mapOf("timezone" to timezone)
            val response = utilityApi.updateTimezone(token, body)

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!

                if (responseBody.success) {
                    emit(ApiResult.Success(responseBody))
                } else {
                    emit(ApiResult.Error(
                        exception = Exception("API returned success=false"),
                        code = response.code()
                    ))
                }
            } else {
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }
}
