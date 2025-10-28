package net.ib.mn.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import net.ib.mn.data.model.NotificationEntity
import net.ib.mn.local.LocalMapper
import net.ib.mn.local.room.IdolRoomConstant
import java.util.Date

@Entity(
    tableName = IdolRoomConstant.Table.NOTIFICATION,
    indices = [Index("createdAt")]
)
data class NotificationLocal(
    @PrimaryKey val id: Long,
    var senderId: Int = 0,
    var type: String? = null,
    var title: String = "",
    var message: String = "",
    var value: String = "",
    var heart: Int = 0,
    var weakHeart: Int = 0,
    var createdAt: Date? = null,
    var expiredAt: Date? = null,
    var readAt: Date? = null,
    var usedAt: Date? = null,
    var extraType: String = "",
    var extraId: Int = 0,
    var link: String? = null
) : LocalMapper<NotificationEntity> {

    override fun toData(): NotificationEntity =
        NotificationEntity(
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

fun NotificationEntity.toLocal(): NotificationLocal =
    NotificationLocal(
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