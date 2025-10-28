package net.ib.mn.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GiveHeartDTO(
    @SerialName("friend_id") val friendId: Long? = null,
    @SerialName("number") val number: Int,
)

@Serializable
data class FriendRequestDTO(
    @SerialName("partner_id") val partnerId: Long,
)

@Serializable
data class RespondRequestDTO(
    @SerialName("partner_id") val partnerId: Long,
    @SerialName("ans") val ans: String, // Y or N
)

@Serializable
data class DeleteFriendsDTO(
    @SerialName("ids") val ids: List<Int>,
)

@Serializable
data class CancelFriendRequestDTO(
    @SerialName("id") val id: Int,
)