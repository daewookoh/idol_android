package net.ib.mn.domain.repository

import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.data.remote.dto.IdolListResponse
import net.ib.mn.data.remote.dto.UpdateInfoResponse
import net.ib.mn.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * Idol Repository 인터페이스
 *
 * Idol 관련 데이터 접근 추상화
 */
interface IdolRepository {

    /**
     * Idol 업데이트 정보 조회
     *
     * @return Flow<ApiResult<UpdateInfoResponse>>
     */
    fun getUpdateInfo(): Flow<ApiResult<UpdateInfoResponse>>

    /**
     * Idol 리스트 조회
     *
     * @param type Idol 타입 (0: all, 1: solo, 2: group, 3: actor)
     * @param category 카테고리 필터
     * @return Flow<ApiResult<IdolListResponse>>
     */
    fun getIdols(type: Int? = null, category: String? = null): Flow<ApiResult<IdolListResponse>>

    /**
     * ID 리스트로 Idol 조회 (API)
     *
     * UDP에서 info_ver 변경 감지 시 전체 필드 업데이트용
     * old 프로젝트의 idol_by_ids API와 동일
     *
     * @param ids 아이돌 ID 리스트
     * @param fields 조회할 필드 (null이면 모든 필드)
     * @return Flow<ApiResult<IdolListResponse>>
     */
    fun getIdolsByIds(ids: List<Int>, fields: String? = null): Flow<ApiResult<IdolListResponse>>

    /**
     * ID로 특정 Idol 조회 (로컬 DB)
     *
     * @param id Idol ID
     * @return IdolEntity 또는 null
     */
    suspend fun getIdolById(id: Int): IdolEntity?

    /**
     * Type과 Category로 Idol 리스트 조회 (로컬 DB)
     *
     * @param type Idol type ("S" or "G")
     * @param category Category ("M" or "F")
     * @return List<IdolEntity>
     */
    suspend fun getIdolsByTypeAndCategory(type: String, category: String): List<IdolEntity>
}
