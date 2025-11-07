package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ib.mn.data.remote.api.OnepickApi
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ImagePickModel
import net.ib.mn.domain.repository.OnepickRepository
import javax.inject.Inject

class OnepickRepositoryImpl @Inject constructor(
    private val onepickApi: OnepickApi
) : OnepickRepository {

    override fun getImagePickList(offset: Int, limit: Int): Flow<ApiResult<List<ImagePickModel>>> = flow {
        try {
            emit(ApiResult.Loading)

            val response = onepickApi.getImagePickList(offset, limit)

            if (response.isSuccessful) {
                val imagePickList = response.body()?.objects ?: emptyList()
                emit(ApiResult.Success(imagePickList))
            } else {
                emit(ApiResult.Error(
                    exception = Exception("Failed to load image pick list"),
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(exception = e))
        }
    }

    override fun getImagePick(id: Int): Flow<ApiResult<ImagePickModel>> = flow {
        try {
            emit(ApiResult.Loading)

            val response = onepickApi.getImagePick(id)

            if (response.isSuccessful) {
                val imagePick = response.body()?.`object`
                if (imagePick != null) {
                    emit(ApiResult.Success(imagePick))
                } else {
                    emit(ApiResult.Error(exception = Exception("Image pick not found")))
                }
            } else {
                emit(ApiResult.Error(
                    exception = Exception("Failed to load image pick"),
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(exception = e))
        }
    }
}
