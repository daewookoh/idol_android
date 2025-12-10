package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 친구 모델
 *
 * old 프로젝트: app/src/main/java/net/ib/mn/model/FriendModel.kt
 */
data class FriendModel(
    @SerializedName("user")
    val user: FriendUser,

    @SerializedName("is_friend")
    val isFriend: String = "N",

    @SerializedName("user_type")
    val userType: String = "",

    @SerializedName("give_heart")
    val giveHeart: Int = 0
) {
    companion object {
        // 내가 친구 요청을 한 사람
        const val RECV_USER = "recv_user"
        // 나한테 친구 요청을 한 사람
        const val SEND_USER = "send_user"
    }

    /**
     * 친구인지 여부
     */
    fun isFriendYes(): Boolean = isFriend.equals("Y", ignoreCase = true)

    /**
     * 내가 친구 요청을 한 사람인지 여부
     */
    fun isRequesterByMe(): Boolean = userType == RECV_USER

    /**
     * 나한테 친구 요청을 한 사람인지 여부
     */
    fun isRequesterToMe(): Boolean = userType == SEND_USER
}

/**
 * 친구 유저 정보
 *
 * old 프로젝트의 UserModel을 간소화
 */
data class FriendUser(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("nickname")
    val nickname: String = "",

    @SerializedName("picture")
    val picture: String? = null,

    @SerializedName("level")
    val level: Int = 0,

    @SerializedName("most")
    val most: FriendMostIdol? = null,

    @SerializedName("level_heart")
    val levelHeart: Long = 0,

    @SerializedName("status_message")
    val statusMessage: String? = null,

    @SerializedName("heart")
    val heart: Int = 0
)

/**
 * 친구의 최애 아이돌
 */
data class FriendMostIdol(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("name")
    val name: String = ""
)
