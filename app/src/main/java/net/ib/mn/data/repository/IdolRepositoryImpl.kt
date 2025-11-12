package net.ib.mn.data.repository

import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.data.remote.api.IdolApi
import net.ib.mn.data.remote.dto.IdolListResponse
import net.ib.mn.data.remote.dto.UpdateInfoResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.IdolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Idol Repository 구현체
 */
class IdolRepositoryImpl @Inject constructor(
    private val idolApi: IdolApi,
    private val idolDao: IdolDao
) : IdolRepository {

    override fun getUpdateInfo(): Flow<ApiResult<UpdateInfoResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val response = idolApi.getUpdateInfo()

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

    override fun getIdols(type: Int?, category: String?): Flow<ApiResult<IdolListResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            val response = idolApi.getIdols(type, category)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // IdolListResponse는 success 필드가 없고 바로 objects 배열을 가짐
                if (body.data != null) {
                    emit(ApiResult.Success(body))
                } else {
                    emit(ApiResult.Error(
                        exception = Exception("API returned null data"),
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

    override fun getIdolsByIds(ids: List<Int>, fields: String?): Flow<ApiResult<IdolListResponse>> = flow {
        emit(ApiResult.Loading)

        try {
            // ID 리스트를 콤마로 구분된 문자열로 변환
            val idsString = ids.joinToString(",")

            val response = idolApi.getIdolsByIds(idsString, fields)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (body.data != null) {
                    emit(ApiResult.Success(body))
                } else {
                    emit(ApiResult.Error(
                        exception = Exception("API returned null data"),
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

    override suspend fun getIdolById(id: Int): IdolEntity? {
        return idolDao.getIdolById(id)
    }

    override suspend fun getIdolsByTypeAndCategory(type: String, category: String): List<IdolEntity> {
        return idolDao.getIdolByTypeAndCategory(type, category)
    }
}
