package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class FriendModel(
    @SerializedName("user") var user: UserModel,
    @SerializedName("is_friend") var isFriend: String,
    @SerializedName("user_type") var userType: String,
    @SerializedName("give_heart") var giveHeart: Int=0
) : Serializable {
    constructor() : this(UserModel(), "", "",0)

    companion object {
        // 내가 친구 요청을 한 사람
        const val RECV_USER = "recv_user"
        // 나한테 친구 요청을 한 사람
        const val SEND_USER = "send_user"
    }
}
