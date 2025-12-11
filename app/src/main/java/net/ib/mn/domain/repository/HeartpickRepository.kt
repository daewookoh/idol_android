package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.HeartPickCommentsResponse
import net.ib.mn.domain.model.HeartPickModel

interface HeartpickRepository {
    /**
     * 하트픽 목록 조회
     * @param offset 페이지 offset
     * @param limit 페이지 limit
     */
    fun getHeartPickList(offset: Int, limit: Int): Flow<ApiResult<List<HeartPickModel>>>

    /**
     * 특정 하트픽 상세 조회
     * @param id 하트픽 ID
     */
    fun getHeartPick(id: Int): Flow<ApiResult<HeartPickModel>>

    /**
     * 하트픽 댓글 목록 조회
     * @param heartPickId 하트픽 ID
     * @param limit 페이지당 개수
     * @param cursor 페이징 커서
     */
    fun getReplies(
        heartPickId: Int,
        limit: Int,
        cursor: String? = null
    ): Flow<ApiResult<HeartPickCommentsResponse>>

    /**
     * 하트픽 댓글 작성
     * @param heartPickId 하트픽 ID
     * @param content 댓글 내용
     * @param emoticonId 이모티콘 ID
     * @param imageBytes 이미지 바이트 배열
     * @return 성공 여부
     */
    fun postReply(
        heartPickId: Int,
        content: String,
        emoticonId: Int? = null,
        imageBytes: ByteArray? = null
    ): Flow<ApiResult<Boolean>>
}
