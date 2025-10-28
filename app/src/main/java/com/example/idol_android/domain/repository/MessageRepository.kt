package com.example.idol_android.domain.repository

import com.example.idol_android.data.remote.dto.MessageCouponResponse
import com.example.idol_android.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessageCoupon(): Flow<ApiResult<MessageCouponResponse>>
}
