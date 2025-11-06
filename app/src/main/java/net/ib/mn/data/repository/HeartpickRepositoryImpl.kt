package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ib.mn.data.remote.api.HeartpickApi
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.HeartPickModel
import net.ib.mn.domain.repository.HeartpickRepository
import javax.inject.Inject

class HeartpickRepositoryImpl @Inject constructor(
    private val heartpickApi: HeartpickApi
) : HeartpickRepository {

    override fun getHeartPickList(offset: Int, limit: Int): Flow<ApiResult<List<HeartPickModel>>> = flow {
        try {
            emit(ApiResult.Loading)

            val response = heartpickApi.getHeartPickList(offset, limit)

            if (response.isSuccessful) {
                val heartPickList = response.body()?.objects ?: emptyList()
                emit(ApiResult.Success(heartPickList))
            } else {
                emit(ApiResult.Error(
                    exception = Exception("Failed to load heart pick list"),
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(exception = e))
        }
    }

    override fun getHeartPick(id: Int): Flow<ApiResult<HeartPickModel>> = flow {
        try {
            emit(ApiResult.Loading)

            val response = heartpickApi.getHeartPick(id)

            if (response.isSuccessful) {
                val heartPick = response.body()?.`object`
                if (heartPick != null) {
                    emit(ApiResult.Success(heartPick))
                } else {
                    emit(ApiResult.Error(exception = Exception("Heart pick not found")))
                }
            } else {
                emit(ApiResult.Error(
                    exception = Exception("Failed to load heart pick"),
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(exception = e))
        }
    }
}
