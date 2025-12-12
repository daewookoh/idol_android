package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.dto.HeartPickVoteResponse
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

    /**
     * 하트픽 오픈 알림 설정 조회
     * @param heartPickId 하트픽 ID
     * @return 알림 설정 여부
     */
    fun getOpenHeartPickNotification(heartPickId: Int): Flow<ApiResult<Boolean>>

    /**
     * 하트픽 오픈 알림 설정
     * @param heartPickId 하트픽 ID
     * @return 성공 여부
     */
    fun postOpenHeartPickNotification(heartPickId: Int): Flow<ApiResult<Boolean>>

    /**
     * 하트픽 투표
     *
     * old 프로젝트: HeartpickRepository.vote()와 동일
     *
     * @param heartPickId 하트픽 ID
     * @param heartPickIdolId 하트픽 아이돌 ID (HeartPickIdol.id)
     * @param number 투표할 하트 개수
     * @return HeartPickVoteResponse (bonusHeart, voted)
     */
    fun voteHeartPick(
        heartPickId: Int,
        heartPickIdolId: Int,
        number: Long
    ): Flow<ApiResult<HeartPickVoteResponse>>
}
