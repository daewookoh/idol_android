package net.ib.mn.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.util.Date

@Parcelize
data class UserModel(
    var id: Int = 0,
    val userId: Int = 0,
    var diamond: Int = 0,
    val email: String? = null,
    var nickname: String = "",
    var imageUrl: String? = null,
    var strongHeart: Long = 0,
    var weakHeart: Long = 0,
    var most: IdolModel? = null,
    var subscriptions: ArrayList<SubscriptionModel> = ArrayList(),
    val resourceUri: String? = null,
    val pushKey: String? = null,
    val createdAt: String? = null,
    var heart: Int = 0,
    var level: Int = 0,
    var levelHeart: Long = 0,
    val lastAct: Date? = null,
    var power: Int = 0,
    // 이모티콘
    var emoticon: EmoticonModel? = null,
    // 푸시 설정
    var pushFilter: Int = 0,
    // 상태 메시지(친구의 user에만 사용)
    var statusMessage: String? = null,
    //친구삭제 체크 확인
    var deleteChecked: Boolean = false,
    var ts: Int = 0,
    // 구매한 뱃지 아이템 정보.
    // 0x01 : 보안관
    // 0x02 : 메신저
    // 0x04 : 몰빵일
    // 위 값들의 OR 연산값
    // 구매한 아이템
    // 2016.6.1 현재 1: 보안관, 2: 메신저, 4: 몰빵일 => bit or 값으로 들어온다
    var itemNo: Int = 0,
    @SerializedName("message_info") val messageInfo: String? = null,
    @SerializedName("message_last") val messageLast: Date? = null,
    var domain: String = "",
    var giveHeart: Int = 0
): Serializable, Parcelable {
    val imageUrlCommunity: String?
        get() = imageUrl?.let { "$it?v=" }
}
