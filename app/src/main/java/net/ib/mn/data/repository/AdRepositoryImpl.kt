package net.ib.mn.data.repository

import net.ib.mn.data.remote.api.AdApi
import net.ib.mn.data.remote.dto.AdTypeListResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.AdRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class AdRepositoryImpl @Inject constructor(
    private val adApi: AdApi
) : AdRepository {

    override fun getAdTypeList(): Flow<ApiResult<AdTypeListResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val response = adApi.getAdTypeList()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.success) {
                    emit(ApiResult.Success(body))
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
