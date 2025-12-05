package net.ib.mn.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.EmoticonApi
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.EmoticonDetailResponse
import net.ib.mn.domain.model.EmoticonSetResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 이모티콘 Repository
 */
@Singleton
class EmoticonRepository @Inject constructor(
    private val emoticonApi: EmoticonApi,
    private val gson: Gson
) : BaseRepository() {

    /**
     * 이모티콘 세트 목록 조회
     */
    fun getEmoticonSets(): Flow<ApiResult<EmoticonSetResponse>> = safeApiCallWithJsonString(
        apiCall = { emoticonApi.getEmoticonSets() },
        parser = { json -> gson.fromJson(json, EmoticonSetResponse::class.java) }
    )

    /**
     * 특정 이모티콘 세트 상세 조회
     */
    fun getEmoticonSet(emoticonSetId: Int): Flow<ApiResult<EmoticonDetailResponse>> = safeApiCallWithJsonString(
        apiCall = { emoticonApi.getEmoticonSet(emoticonSetId) },
        parser = { json -> gson.fromJson(json, EmoticonDetailResponse::class.java) }
    )
}
