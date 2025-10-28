package com.example.idol_android.domain.usecase

import com.example.idol_android.data.remote.dto.*
import com.example.idol_android.domain.model.ApiResult
import com.example.idol_android.domain.repository.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 광고 타입 리스트 조회 UseCase
 */
class GetAdTypeListUseCase @Inject constructor(
    private val adRepository: AdRepository
) {
    operator fun invoke(): Flow<ApiResult<AdTypeListResponse>> {
        return adRepository.getAdTypeList()
    }
}

/**
 * 쿠폰 메시지 조회 UseCase
 */
class GetMessageCouponUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(): Flow<ApiResult<MessageCouponResponse>> {
        return messageRepository.getMessageCoupon()
    }
}

/**
 * 타임존 업데이트 UseCase
 */
class UpdateTimezoneUseCase @Inject constructor(
    private val utilityRepository: UtilityRepository
) {
    operator fun invoke(timezone: String): Flow<ApiResult<TimezoneUpdateResponse>> {
        return utilityRepository.updateTimezone(timezone)
    }
}

/**
 * Idol 리스트 조회 UseCase
 */
class GetIdolsUseCase @Inject constructor(
    private val idolRepository: IdolRepository
) {
    operator fun invoke(type: Int? = null, category: String? = null): Flow<ApiResult<IdolListResponse>> {
        return idolRepository.getIdols(type, category)
    }
}

/**
 * IAB 공개키 조회 UseCase
 */
class GetIabKeyUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<ApiResult<IabKeyResponse>> {
        return userRepository.getIabKey()
    }
}

/**
 * 차단 사용자 목록 조회 UseCase
 */
class GetBlocksUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<ApiResult<BlockListResponse>> {
        return userRepository.getBlocks()
    }
}
