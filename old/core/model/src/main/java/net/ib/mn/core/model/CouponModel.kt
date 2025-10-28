package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class CouponModel(
    @SerialName("created_at") @Serializable(with = DateSerializer::class) val createdAt: Date,
    @SerialName("expired_at") @Serializable(with = DateSerializer::class) val expiredAt: Date,
    @SerialName("heart") val heart: Int = 0,
    @SerialName("message") val message: String? = null,
    @SerialName("value") val value: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("used_at") val usedAt: String? = null,
    @SerialName("weak_heart") val weakHeart: Int = 0,
    @SerialName("id") val id: Int = 0,
)
