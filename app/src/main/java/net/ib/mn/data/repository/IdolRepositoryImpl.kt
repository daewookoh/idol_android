package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ib.mn.data.local.dao.IdolDao
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.data.remote.api.IdolApi
import net.ib.mn.data.remote.dto.IdolListResponse
import net.ib.mn.data.remote.dto.UpdateInfoResponse
import net.ib.mn.domain.model.ApiError
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.IdolRepository
import javax.inject.Inject

/**
 * IdolRepository 구현체
 *
 * BaseRepository의 safeApiCall 패턴 사용
 */
class IdolRepositoryImpl @Inject constructor(
    private val idolApi: IdolApi,
    private val idolDao: IdolDao
) : BaseRepository(), IdolRepository {

    override fun getUpdateInfo(): Flow<ApiResult<UpdateInfoResponse>> =
        safeApiCall { idolApi.getUpdateInfo() }
            .validateSuccess()

    override fun getIdols(type: Int?, category: String?): Flow<ApiResult<IdolListResponse>> =
        safeApiCall { idolApi.getIdols(type, category) }
            .validateData()

    override fun getIdolsByIds(ids: List<Int>, fields: String?): Flow<ApiResult<IdolListResponse>> =
        safeApiCall { idolApi.getIdolsByIds(ids.joinToString(","), fields) }
            .validateData()

    override suspend fun getIdolById(id: Int): IdolEntity? {
        return idolDao.getIdolById(id)
    }

    override suspend fun getIdolsByTypeAndCategory(type: String, category: String): List<IdolEntity> {
        return idolDao.getIdolByTypeAndCategory(type, category)
    }

    override suspend fun getIdolsByIds(ids: List<Int>): List<IdolEntity> {
        return idolDao.getIdolsByIds(ids)
    }

    /**
     * IdolListResponse의 data 필드 검증
     */
    private fun Flow<ApiResult<IdolListResponse>>.validateData(): Flow<ApiResult<IdolListResponse>> =
        map { result ->
            when (result) {
                is ApiResult.Success -> {
                    if (result.data.data != null) {
                        result
                    } else {
                        ApiResult.Error(ApiError.Business(gcode = 0, message = "API returned null data"))
                    }
                }
                else -> result
            }
        }
}
