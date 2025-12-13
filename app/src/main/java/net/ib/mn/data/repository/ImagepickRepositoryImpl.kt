package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ib.mn.data.remote.api.ImagepickApi
import net.ib.mn.data.remote.dto.ImagePickAlarmRequest
import net.ib.mn.data.remote.dto.ImagePickVoteRequest
import net.ib.mn.domain.model.ApiError
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ImagePickModel
import net.ib.mn.domain.repository.ImagePickResultData
import net.ib.mn.domain.repository.ImagepickRepository
import javax.inject.Inject

/**
 * ImagepickRepository 구현체
 *
 * BaseRepository의 safeApiCall + extractObject/extractList 패턴 사용
 * 서버 엔드포인트: onepick/
 */
class ImagepickRepositoryImpl @Inject constructor(
    private val imagepickApi: ImagepickApi
) : BaseRepository(), ImagepickRepository {

    override fun getImagePickList(offset: Int, limit: Int): Flow<ApiResult<List<ImagePickModel>>> =
        safeApiCall { imagepickApi.getImagePickList(offset, limit) }
            .extractList { it.objects }

    override fun getImagePick(id: Int): Flow<ApiResult<ImagePickModel>> =
        safeApiCall { imagepickApi.getImagePick(id) }
            .extractObject({ it.`object` }, "Image pick not found")

    override fun getImagePickResult(id: Int): Flow<ApiResult<ImagePickResultData>> =
        safeApiCall { imagepickApi.getImagePickResult(id) }
            .map { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val response = result.data
                        if (response.success && response.objects != null) {
                            ApiResult.Success(
                                ImagePickResultData(
                                    candidates = response.objects,
                                    date = response.date ?: "",
                                    vote = response.vote ?: "N",
                                    dimension = response.dimension,
                                    // 기본 정보 (서버에서 반환하는 경우)
                                    title = response.title,
                                    subtitle = response.subtitle,
                                    status = response.status,
                                    count = response.count,
                                    createdAt = response.createdAt,
                                    expiredAt = response.expiredAt,
                                    alarm = response.alarm
                                )
                            )
                        } else {
                            ApiResult.Error(ApiError.Business(gcode = 0, message = response.msg ?: "Failed to load image pick result"))
                        }
                    }
                    is ApiResult.Error -> result
                    is ApiResult.Loading -> ApiResult.Loading
                }
            }

    override fun voteImagePick(id: Int, voteIds: String, voteType: String): Flow<ApiResult<Boolean>> =
        safeApiCall {
            imagepickApi.voteImagePick(
                ImagePickVoteRequest(
                    id = id,
                    voteIds = voteIds,
                    voteType = voteType
                )
            )
        }.map { result ->
            when (result) {
                is ApiResult.Success -> {
                    if (result.data.success) {
                        ApiResult.Success(true)
                    } else {
                        ApiResult.Error(ApiError.Business(gcode = 0, message = result.data.msg ?: "Vote failed"))
                    }
                }
                is ApiResult.Error -> result
                is ApiResult.Loading -> ApiResult.Loading
            }
        }

    override fun setImagePickAlarm(id: Int): Flow<ApiResult<Boolean>> =
        safeApiCall {
            imagepickApi.setImagePickAlarm(ImagePickAlarmRequest(id = id))
        }.map { result ->
            when (result) {
                is ApiResult.Success -> {
                    if (result.data.success) {
                        ApiResult.Success(true)
                    } else {
                        ApiResult.Error(ApiError.Business(gcode = 0, message = result.data.msg ?: "Alarm setting failed"))
                    }
                }
                is ApiResult.Error -> result
                is ApiResult.Loading -> ApiResult.Loading
            }
        }
}
