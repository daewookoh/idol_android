package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ImagePickIdolModel
import net.ib.mn.domain.model.ImagePickModel

/**
 * 이미지픽 결과 데이터
 * 서버 응답에서 이미지픽 기본 정보와 결과 순위를 모두 포함
 */
data class ImagePickResultData(
    val candidates: List<ImagePickIdolModel>,
    val date: String,
    val vote: String,
    val dimension: Int,
    // 이미지픽 기본 정보 (서버에서 반환하는 경우)
    val title: String? = null,
    val subtitle: String? = null,
    val status: Int? = null,
    val count: Int? = null,
    val createdAt: String? = null,
    val expiredAt: String? = null,
    val alarm: Boolean = false
)

interface ImagepickRepository {
    /**
     * 이미지픽 목록 조회
     * @param offset 페이지 offset
     * @param limit 페이지 limit
     */
    fun getImagePickList(offset: Int, limit: Int): Flow<ApiResult<List<ImagePickModel>>>

    /**
     * 특정 이미지픽 상세 조회
     * @param id 이미지픽 ID
     */
    fun getImagePick(id: Int): Flow<ApiResult<ImagePickModel>>

    /**
     * 이미지픽 결과/순위 조회
     * 투표 전 후보 목록 또는 결과 순위를 조회
     * @param id 이미지픽 ID
     */
    fun getImagePickResult(id: Int): Flow<ApiResult<ImagePickResultData>>

    /**
     * 이미지픽 투표
     * @param id 이미지픽 ID
     * @param voteIds 투표한 후보 ID들 (콤마로 구분)
     * @param voteType 투표 타입
     */
    fun voteImagePick(id: Int, voteIds: String, voteType: String): Flow<ApiResult<Boolean>>

    /**
     * 이미지픽 개설 알림 설정
     * @param id 이미지픽 ID
     */
    fun setImagePickAlarm(id: Int): Flow<ApiResult<Boolean>>
}
