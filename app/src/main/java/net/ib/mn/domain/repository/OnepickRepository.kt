package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ImagePickModel

interface OnepickRepository {
    /**
     * 이미지픽 목록 조회
     * @param offset 페이지 offset
     * @param limit 페이지 limit
     */
    fun getImagePickList(offset: Int, limit: Int): Flow<ApiResult<List<ImagePickModel>>>

    /**
     * 특정 이미지픽 상세 조회
     * @param id 이미지픽 ID
     */
    fun getImagePick(id: Int): Flow<ApiResult<ImagePickModel>>
}
