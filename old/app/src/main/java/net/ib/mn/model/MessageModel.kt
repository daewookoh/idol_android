package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import net.ib.mn.domain.model.Notification
import java.io.Serializable
import java.util.Date

data class MessageModel(
    @SerializedName("id") var id: Long,
    @SerializedName("sender_id") var senderId: Int,
    @SerializedName("type") var type: String?,
    @SerializedName("title") var title: String,
    @SerializedName("message") var message: String,
    @SerializedName("value") var value: String,
    @SerializedName("heart") var heart: Int,
    @SerializedName("weak_heart") var weakHeart: Int,
    @SerializedName("created_at") var createdAt: Date?,
    @SerializedName("expired_at") var expiredAt: Date?,
    @SerializedName("read_at") var readAt: Date?,
    @SerializedName("used_at") var usedAt: Date?,
    @SerializedName("extra_type") var extraType: String,
    @SerializedName("extra_id") var extraId: Int,
    @SerializedName("link") var link: String? = null
) : Serializable

fun MessageModel.toDomain(): Notification =
    Notification(
        id,
        senderId,
        type,
        title,
        message,
        value,
        heart,
        weakHeart,
        createdAt,
        expiredAt,
        readAt,
        usedAt,
        extraType,
        extraId,
        link
    )

fun Notification.toPresentation(): MessageModel =
    MessageModel(
        id,
        senderId,
        type,
        title,
        message,
        value,
        heart,
        weakHeart,
        createdAt,
        expiredAt,
        readAt,
        usedAt,
        extraType,
        extraId,
        link
    )
