package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.ThemepickApi
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
            .extractObject({ it.`object` }, "Theme pick not found")
}
