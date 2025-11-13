package net.ib.mn.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ib.mn.data.remote.api.AddFavoriteRequest
import net.ib.mn.data.remote.api.FavoritesApi
import net.ib.mn.data.remote.dto.FavoriteDto
import net.ib.mn.data.remote.dto.FavoritesResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.FavoritesRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Favorites Repository 구현체
 */
@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoritesApi: FavoritesApi,
    private val gson: Gson,
    private val idolDao: net.ib.mn.data.local.dao.IdolDao,
    private val userCacheRepository: net.ib.mn.data.repository.UserCacheRepository
) : FavoritesRepository {

    override fun getFavoritesSelf(): Flow<ApiResult<List<FavoriteDto>>> = flow {
        emit(ApiResult.Loading)

        try {
            android.util.Log.d("FavoritesRepo", "========================================")
            android.util.Log.d("FavoritesRepo", "🟢 Calling getFavoritesSelf API (favorites/self/)")
            android.util.Log.d("FavoritesRepo", "========================================")

            val response = favoritesApi.getFavoritesSelf()

            android.util.Log.d("FavoritesRepo", "📦 Response received:")
            android.util.Log.d("FavoritesRepo", "  - HTTP Code: ${response.code()}")
            android.util.Log.d("FavoritesRepo", "  - isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val jsonString = response.body()!!.string()

                android.util.Log.d("FavoritesRepo", "📋 JSON Response:")
                android.util.Log.d("FavoritesRepo", jsonString)

                // JSON 파싱
                val favoritesResponse = gson.fromJson(jsonString, FavoritesResponse::class.java)

                if (favoritesResponse.success) {
                    val favorites = favoritesResponse.objects ?: emptyList()
                    android.util.Log.d("FavoritesRepo", "✅ getFavoritesSelf SUCCESS")
                    android.util.Log.d("FavoritesRepo", "  - Favorites count: ${favorites.size}")

                    emit(ApiResult.Success(favorites))
                } else {
                    android.util.Log.e("FavoritesRepo", "❌ API returned success=false")
                    emit(ApiResult.Error(
                        exception = Exception(favoritesResponse.msg ?: "API returned success=false"),
                        code = response.code(),
                        message = favoritesResponse.msg ?: "Unknown error"
                    ))
                }
            } else {
                android.util.Log.e("FavoritesRepo", "❌ Response not successful or body null")
                android.util.Log.e("FavoritesRepo", "  - Error body: ${response.errorBody()?.string()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: HttpException) {
            android.util.Log.e("FavoritesRepo", "❌ HttpException: ${e.code()} - ${e.message()}", e)
            emit(ApiResult.Error(
                exception = e,
                code = e.code(),
                message = "HTTP ${e.code()}: ${e.message()}"
            ))
        } catch (e: IOException) {
            android.util.Log.e("FavoritesRepo", "❌ IOException: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Network error: ${e.message}"
            ))
        } catch (e: Exception) {
            android.util.Log.e("FavoritesRepo", "❌ Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = "Unknown error: ${e.message}"
            ))
        }
    }

    override fun addFavorite(idolId: Int): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)

        try {
            android.util.Log.d("FavoritesRepo", "🟢 Adding favorite: idolId=$idolId")

            val request = AddFavoriteRequest(idol_id = idolId)
            val response = favoritesApi.addFavorite(request)

            if (response.isSuccessful) {
                android.util.Log.d("FavoritesRepo", "✅ addFavorite SUCCESS")
                emit(ApiResult.Success(Unit))
            } else {
                android.util.Log.e("FavoritesRepo", "❌ addFavorite FAILED: ${response.code()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoritesRepo", "❌ addFavorite Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = e.message
            ))
        }
    }

    override fun removeFavorite(id: Int): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)

        try {
            android.util.Log.d("FavoritesRepo", "🔴 Removing favorite: id=$id")

            val response = favoritesApi.removeFavorite(id)

            if (response.isSuccessful) {
                android.util.Log.d("FavoritesRepo", "✅ removeFavorite SUCCESS")
                emit(ApiResult.Success(Unit))
            } else {
                android.util.Log.e("FavoritesRepo", "❌ removeFavorite FAILED: ${response.code()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoritesRepo", "❌ removeFavorite Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = e.message
            ))
        }
    }

    override fun deleteCache(): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)

        try {
            android.util.Log.d("FavoritesRepo", "🗑️ Deleting cache")

            val response = favoritesApi.deleteCache()

            if (response.isSuccessful) {
                android.util.Log.d("FavoritesRepo", "✅ deleteCache SUCCESS")
                emit(ApiResult.Success(Unit))
            } else {
                android.util.Log.e("FavoritesRepo", "❌ deleteCache FAILED: ${response.code()}")
                emit(ApiResult.Error(
                    exception = HttpException(response),
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoritesRepo", "❌ deleteCache Exception: ${e.message}", e)
            emit(ApiResult.Error(
                exception = e,
                message = e.message
            ))
        }
    }

    override suspend fun loadAndSaveFavoriteSelf(): Result<Boolean> {
        return try {
            var success = false

            getFavoritesSelf().collect { result ->
                when (result) {
                    is ApiResult.Loading -> {}
                    is ApiResult.Success -> {
                        val favorites = result.data

                        // Extract idol IDs from favorites and cache them
                        val idolIds = favorites.map { it.idol.id }
                        userCacheRepository.setFavoriteIdolIds(idolIds)

                        // Save each favorite idol to local DB
                        favorites.forEach { favorite ->
                            val idol = favorite.idol
                            val idolEntity = net.ib.mn.data.local.entity.IdolEntity(
                                id = idol.id,
                                name = idol.name ?: "",
                                category = idol.category ?: "",
                                type = idol.type ?: "",
                                imageUrl = idol.imageUrl,
                                imageUrl2 = idol.imageUrl2,
                                imageUrl3 = idol.imageUrl3,
                                groupId = idol.groupId ?: 0,
                                heart = idol.heart ?: 0L
                            )
                            idolDao.upsert(idolEntity)
                        }

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
