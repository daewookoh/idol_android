package net.ib.mn.domain.testutil

import net.ib.mn.domain.model.Idol
import net.ib.mn.domain.model.Notification
import java.util.Date

fun createDummyIdol(id: Int = 1): Idol {
    return Idol(
        id = id,
        name = "Idol $id",
        description = "desc",
        miracleCount = 0,
        angelCount = 0,
        rookieCount = 0,
        anniversary = "N",
        anniversaryDays = null,
        birthDay = null,
        burningDay = null,
        category = "",
        comebackDay = null,
        debutDay = null,
        fairyCount = 0,
        groupId = 0,
        heart = 0,
        imageUrl = null,
        imageUrl2 = null,
        imageUrl3 = null,
        isViewable = "Y",
        nameEn = "",
        nameJp = "",
        nameZh = "",
        nameZhTw = "",
        resourceUri = "",
        top3 = null,
        top3Type = null,
        top3Seq = -1,
        type = "",
        infoSeq = -1,
        isLunarBirthday = null,
        mostCount = 0,
        mostCountDesc = null,
        updateTs = 0,
        sourceApp = null,
        fdName = "",
        fdNameEn = ""
    )
}

fun createDummyNotification(id: Long = 1L, message: String = "Message $id"): Notification {
    return Notification(
        id = id,
        senderId = 100,
        type = "INFO",
        title = "Notification Title $id",
        message = message,
        value = "value-$id",
        heart = 10,
        weakHeart = 5,
        createdAt = Date(),
        expiredAt = null,
        readAt = null,
        usedAt = null,
        extraType = "EXTRA",
        extraId = 999,
        link = "https://example.com/notification/$id"
    )
}