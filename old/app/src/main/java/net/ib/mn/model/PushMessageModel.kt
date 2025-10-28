package net.ib.mn.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import net.ib.mn.chatting.model.MessageModel

@Parcelize
data class PushMessageModel(
    @SerializedName("title") var title: String? = null,
    @SerializedName("subtitle") var subtitle: String? = null,
    @SerializedName("message") var message: String? = null,
    @SerializedName("type") var type: String? = null,
    @SerializedName("link") var link: String? = null,
    @SerializedName("room_id") var roomId: Int? = 0,
    @SerializedName("idol_id") var idolId: Int? = 0,
    @SerializedName("support_id") var supportId: Int? = 0,
    @SerializedName("status") var supportStatus: Int? = 0,
    @SerializedName("article") var article: ArticleModel? = ArticleModel(),
    @SerializedName("locale") var locale : String? = null,
    @SerializedName("messages") var messages: ArrayList<MessageModel> = arrayListOf(),
    @SerializedName("schedule_id") var scheduleId: Int? = 0,
    @SerializedName("analytics_click_label") var analyticsClickLabel: String? = null
) : Parcelable