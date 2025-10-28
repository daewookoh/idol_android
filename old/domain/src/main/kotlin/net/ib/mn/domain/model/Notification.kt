package net.ib.mn.domain.model

import java.util.Date

data class Notification(
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
)
