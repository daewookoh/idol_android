package net.ib.mn.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.AddFavoriteRequest
import net.ib.mn.data.remote.api.FavoritesApi
import net.ib.mn.data.remote.dto.FavoriteDto
import net.ib.mn.data.remote.dto.FavoritesResponse
import net.ib.mn.domain.model.ApiError
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.FavoritesRepository
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FavoritesRepository 구현체
 *
 * BaseRepository의 safeApiCallWithJsonString 패턴 사용
 */
@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoritesApi: FavoritesApi,
    private val gson: Gson,
    private val idolDao: net.ib.mn.data.local.dao.IdolDao,
    private val userCacheRepository: net.ib.mn.data.repository.UserCacheRepository
) : BaseRepository(), FavoritesRepository {

    override fun getFavoritesSelf(): Flow<ApiResult<List<FavoriteDto>>> =
        safeApiCallWithJsonString(
            apiCall = { favoritesApi.getFavoritesSelf() },
            parser = { json ->
                val favoritesResponse = gson.fromJson(json, FavoritesResponse::class.java)
                if (favoritesResponse.success) {
                    favoritesResponse.objects ?: emptyList()
                } else {
                    throw Exception(favoritesResponse.msg ?: "API returned success=false")
                }
            }
        )

    override fun addFavorite(idolId: Int): Flow<ApiResult<Int>> =
        safeApiCallWithJsonString(
            apiCall = { favoritesApi.addFavorite(AddFavoriteRequest(idol_id = idolId)) },
            parser = { json ->
                val jsonObject = JSONObject(json)
                val success = jsonObject.optBoolean("success", true)
                if (success) {
                    jsonObject.optInt("id", -1)
                } else {
                    throw Exception(jsonObject.optString("msg", "Unknown error"))
                }
            }
        )

    override fun removeFavorite(id: Int): Flow<ApiResult<Unit>> =
        safeApiCallWithJsonString(
            apiCall = { favoritesApi.removeFavorite(id) },
            parser = { Unit }
        )

    override fun deleteCache(): Flow<ApiResult<Unit>> =
        safeApiCallWithJsonString(
            apiCall = { favoritesApi.deleteCache() },
            parser = { Unit }
        )

    override suspend fun loadAndSaveFavoriteSelf(): Result<Boolean> {
        return try {
            var success = false

            getFavoritesSelf().collect { result ->
                when (result) {
                    is ApiResult.Loading -> {}
                    is ApiResult.Success -> {
                        val favorites = result.data

                        val idolIds = favorites.map { it.idol.id }
                        userCacheRepository.setFavoriteIdolIds(idolIds)

                        val favoriteIdMap = favorites.associate { it.idol.id to it.id }
                        userCacheRepository.setFavoriteIdMap(favoriteIdMap)

                        success = true
                    }
                    is ApiResult.Error -> {}
                }
            }

            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to load favorites"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
