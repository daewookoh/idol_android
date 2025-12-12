package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ib.mn.data.remote.api.ThemepickApi
import net.ib.mn.data.remote.dto.ThemePickVoteRequest
import net.ib.mn.data.remote.dto.OpenNotificationRequest
import net.ib.mn.domain.model.ApiError
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ThemePickModel
import net.ib.mn.domain.repository.ThemepickRepository
import javax.inject.Inject

/**
 * ThemepickRepository 구현체
 *
 * BaseRepository의 safeApiCall + extractObject/extractList 패턴 사용
 */
class ThemepickRepositoryImpl @Inject constructor(
    private val themepickApi: ThemepickApi
) : BaseRepository(), ThemepickRepository {

    override fun getThemePickList(offset: Int, limit: Int): Flow<ApiResult<List<ThemePickModel>>> =
        safeApiCall { themepickApi.getThemePickList(offset, limit) }
            .extractList { it.objects }

    override fun getThemePick(id: Int): Flow<ApiResult<ThemePickModel>> =
        safeApiCall { themepickApi.getThemePick(id) }
            .map { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val response = result.data
                        if (response.success) {
                            ApiResult.Success(response.toModel())
                        } else {
                            ApiResult.Error(ApiError.Business(
                                gcode = 0,
                                message = "Theme pick not found"
                            ))
                        }
                    }
                    is ApiResult.Error -> result
                    is ApiResult.Loading -> result
                }
            }

    override fun vote(id: Int, idolId: Int, voteType: String): Flow<ApiResult<Boolean>> =
        safeApiCall { themepickApi.vote(ThemePickVoteRequest(id, idolId, voteType)) }
            .map { result ->
                when (result) {
                    is ApiResult.Success -> ApiResult.Success(result.data.success)
                    is ApiResult.Error -> result
                    is ApiResult.Loading -> result
                }
            }

    override fun postOpenNotification(id: Int): Flow<ApiResult<Boolean>> =
        safeApiCall { themepickApi.postOpenNotification(OpenNotificationRequest(id)) }
            .map { result ->
                when (result) {
                    is ApiResult.Success -> ApiResult.Success(result.data.success)
                    is ApiResult.Error -> result
                    is ApiResult.Loading -> result
                }
            }
}
