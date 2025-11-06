package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.HeartPickModel

interface HeartpickRepository {
    /**
     * 하트픽 목록 조회
     * @param offset 페이지 offset
     * @param limit 페이지 limit
     */
    fun getHeartPickList(offset: Int, limit: Int): Flow<ApiResult<List<HeartPickModel>>>

    /**
     * 특정 하트픽 상세 조회
     * @param id 하트픽 ID
     */
    fun getHeartPick(id: Int): Flow<ApiResult<HeartPickModel>>
}
