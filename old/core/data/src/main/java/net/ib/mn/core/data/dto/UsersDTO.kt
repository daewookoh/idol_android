package net.ib.mn.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class SignUpDTO(
    @SerialName("domain") val domain: String? = null,
    @SerialName("email") val email: String,
    @SerialName("passwd") val passwd: String,
    @SerialName("nickname") val name: String,
    @SerialName("referral_code") val referralCode: String,
    @SerialName("push_key") val deviceKey: String,
    @SerialName("gmail") val gmail: String,
    @SerialName("version") val version: String,
    @SerialName("app_id") val appId: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("google_account") val googleAccount: String,
    @SerialName("time") val time: Long,
    @SerialName("facebook_id") val facebookId: Long? = null,
)

@Serializable
data class SignInDTO(
    @SerialName("domain") val domain: String? = null,
    @SerialName("email") val email: String,
    @SerialName("passwd") val passwd: String,
    @SerialName("push_key") val deviceKey: String,
    @SerialName("gmail") val gmail: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("app_id") val appId: String,
)

@Serializable
data class FindPasswordDTO(
    @SerialName("email") val email: String,
)

@Serializable
data class ChangePasswordDTO(
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class SetProfileImageDTO(
    @SerialName("image") val image: String, // base64
)

@Serializable
data class UpdateMostDTO(
    @SerialName("most") val mostResourceUri: String, // resource uri
)

@Serializable
data class AlterNicknameDTO(
    @SerialName("nickname") val nickname: String,
    @SerialName("use_coupon") val useCoupon: String? = null,
)

@Serializable
data class ProvideHeartDTO(
    @SerialName("type") val type: String,
)

@Serializable
data class StatusDTO(
    @SerialName("status_message") val statusMessage: String? = null,
    @SerialName("feed_is_viewable") val feedIsViewable: String? = null,
    @SerialName("friend_allow") val friendAllow: String? = null,
    @SerialName("new_friends") val newFriends: String? = null,
)

@Serializable
data class IabVerifyDTO(
    @SerialName("receipt") val receipt: String,
    @SerialName("signature") val signature: String,
    @SerialName("state") val state: String, // normal, abnormal
    @SerialName("subscription") val subscription: String? = null,
)

@Serializable
data class UpdatePushKeyDTO(
    @SerialName("push_key") val pushKey: String? = null,
    @SerialName("device_id") val deviceId: String,
    @SerialName("timezone") val timezone: String,
)

@Serializable
data class PushFilterDTO(
    @SerialName("filter") val filter: Int,
)

@Serializable
data class InhouseOfferwallDTO(
    @SerialName("package") val packageName: String,
)

@Serializable
data class BlockUserDTO(
    @SerialName("target_id") val targetId: Int,
    @SerialName("reason") val reason: Int,
    @SerialName("block") val block: String,
)

@Serializable
data class DailyRewardDTO(
    @SerialName("key") val key: String,
)

// 구시대의 유물이나 api 호출을 위해 남겨둠
@Serializable
data class UpdateProfileDTO(
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class UpdateTutorialDTO(
    @SerialName("index") val index: Int,
)

