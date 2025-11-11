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
    private val gson: Gson
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
}
