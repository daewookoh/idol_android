package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.OnepickApi
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ImagePickModel
import net.ib.mn.domain.repository.OnepickRepository
import javax.inject.Inject

/**
 * OnepickRepository 구현체
 *
 * BaseRepository의 safeApiCall + extractObject/extractList 패턴 사용
 */
class OnepickRepositoryImpl @Inject constructor(
    private val onepickApi: OnepickApi
) : BaseRepository(), OnepickRepository {

    override fun getImagePickList(offset: Int, limit: Int): Flow<ApiResult<List<ImagePickModel>>> =
        safeApiCall { onepickApi.getImagePickList(offset, limit) }
            .extractList { it.objects }

    override fun getImagePick(id: Int): Flow<ApiResult<ImagePickModel>> =
        safeApiCall { onepickApi.getImagePick(id) }
            .extractObject({ it.`object` }, "Image pick not found")
}
