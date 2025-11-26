package net.ib.mn.domain.repository

import net.ib.mn.data.remote.dto.MessageCouponResponse
import net.ib.mn.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessageCoupon(): Flow<ApiResult<MessageCouponResponse>>

    /**
     * 새 알림이 있는지 체크
     * @param afterDate 특정 시점 이후의 알림만 조회 (UTC date string, null이면 전체)
     * @return 새 알림 존재 여부
     */
    suspend fun checkNewNotification(afterDate: String?): Boolean
}
