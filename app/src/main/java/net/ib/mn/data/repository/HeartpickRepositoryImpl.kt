package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.HeartpickApi
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.HeartPickModel
import net.ib.mn.domain.repository.HeartpickRepository
import javax.inject.Inject

/**
 * HeartpickRepository 구현체
 *
 * BaseRepository의 safeApiCall + extractObject/extractList 패턴 사용
 */
class HeartpickRepositoryImpl @Inject constructor(
    private val heartpickApi: HeartpickApi
) : BaseRepository(), HeartpickRepository {

    override fun getHeartPickList(offset: Int, limit: Int): Flow<ApiResult<List<HeartPickModel>>> =
        safeApiCall { heartpickApi.getHeartPickList(offset, limit) }
            .extractList { it.objects }

    override fun getHeartPick(id: Int): Flow<ApiResult<HeartPickModel>> =
        safeApiCall { heartpickApi.getHeartPick(id) }
            .extractObject({ it.`object` }, "Heart pick not found")
}
