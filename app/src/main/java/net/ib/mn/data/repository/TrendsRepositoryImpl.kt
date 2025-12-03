package net.ib.mn.data.repository

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.TrendsApi
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.TrendsModel
import net.ib.mn.domain.repository.TrendsRepository
import net.ib.mn.domain.repository.TrendsResponse
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TrendsRepository 구현체 (이붙그램)
 */
@Singleton
class TrendsRepositoryImpl @Inject constructor(
    private val trendsApi: TrendsApi
) : BaseRepository(), TrendsRepository {

    override fun getRecent(
        idolId: Int,
        offset: Int,
        limit: Int
    ): Flow<ApiResult<TrendsResponse>> =
        safeApiCallWithJsonString(
            apiCall = { trendsApi.getRecent(idolId, null, offset) },
            parser = { json ->
                val jsonObject = JSONObject(json)
                val gson = GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    .create()
                val listType = object : TypeToken<List<TrendsModel>>() {}.type

                val items: List<TrendsModel> = gson.fromJson(
                    jsonObject.optJSONArray("objects")?.toString() ?: "[]",
                    listType
                )

                val meta = jsonObject.optJSONObject("meta")
                val totalCount = meta?.optInt("total_count") ?: 0

                TrendsResponse(
                    items = items,
                    totalCount = totalCount,
                    hasMore = offset + items.size < totalCount
                )
            }
        )
}
