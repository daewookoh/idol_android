package net.ib.mn.data.model

import net.ib.mn.data.DataMapper
import net.ib.mn.domain.model.Notification
import java.util.Date

data class NotificationEntity(
    val id: Long,
    var senderId: Int,
    var type: String?,
    var title: String,
    var message: String,
    var value: String,
    var heart: Int,
    var weakHeart: Int,
    var createdAt: Date?,
    var expiredAt: Date?,
    var readAt: Date?,
    var usedAt: Date?,
    var extraType: String,
    var extraId: Int,
    var link: String?
) : DataMapper<Notification> {

    override fun toDomain(): Notification =
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
}

fun Notification.toNotificationEntity(): NotificationEntity {
    return NotificationEntity(
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
}