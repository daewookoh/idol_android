package net.ib.mn.domain.repository

import net.ib.mn.data.remote.dto.MessageCouponResponse
import net.ib.mn.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessageCoupon(): Flow<ApiResult<MessageCouponResponse>>
}
