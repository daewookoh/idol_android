package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.dto.FavoriteDto
import net.ib.mn.domain.model.ApiResult

/**
 * Favorites Repository Interface
 *
 * 최애 관련 데이터 처리
 */
interface FavoritesRepository {

    /**
     * 내 최애 목록 조회
     *
     * @return Flow<ApiResult<List<FavoriteDto>>>
     */
    fun getFavoritesSelf(): Flow<ApiResult<List<FavoriteDto>>>

    /**
     * 최애 추가
     *
     * @param idolId 아이돌 ID
     * @return Flow<ApiResult<Unit>>
     */
    fun addFavorite(idolId: Int): Flow<ApiResult<Unit>>

    /**
     * 최애 삭제
     *
     * @param id Favorite ID
     * @return Flow<ApiResult<Unit>>
     */
    fun removeFavorite(id: Int): Flow<ApiResult<Unit>>

    /**
     * 최애 캐시 삭제
     *
     * @return Flow<ApiResult<Unit>>
     */
    fun deleteCache(): Flow<ApiResult<Unit>>
}
