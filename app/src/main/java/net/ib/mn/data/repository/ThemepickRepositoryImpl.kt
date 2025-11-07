package net.ib.mn.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ib.mn.data.remote.api.ThemepickApi
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ThemePickModel
import net.ib.mn.domain.repository.ThemepickRepository
import javax.inject.Inject

class ThemepickRepositoryImpl @Inject constructor(
    private val themepickApi: ThemepickApi
) : ThemepickRepository {

    override fun getThemePickList(offset: Int, limit: Int): Flow<ApiResult<List<ThemePickModel>>> = flow {
        try {
            emit(ApiResult.Loading)

            val response = themepickApi.getThemePickList(offset, limit)

            if (response.isSuccessful) {
                val themePickList = response.body()?.objects ?: emptyList()
                emit(ApiResult.Success(themePickList))
            } else {
                emit(ApiResult.Error(
                    exception = Exception("Failed to load theme pick list"),
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(exception = e))
        }
    }

    override fun getThemePick(id: Int): Flow<ApiResult<ThemePickModel>> = flow {
        try {
            emit(ApiResult.Loading)

            val response = themepickApi.getThemePick(id)

            if (response.isSuccessful) {
                val themePick = response.body()?.`object`
                if (themePick != null) {
                    emit(ApiResult.Success(themePick))
                } else {
                    emit(ApiResult.Error(exception = Exception("Theme pick not found")))
                }
            } else {
                emit(ApiResult.Error(
                    exception = Exception("Failed to load theme pick"),
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(exception = e))
        }
    }
}
