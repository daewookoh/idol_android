package net.ib.mn.data.model

import net.ib.mn.domain.model.Notification
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class NotificationEntityMapperTest {

    private val sampleDate = Date()

    @Test
    fun `toDomain should map NotificationEntity to Notification correctly`() {
        // given
        val entity = NotificationEntity(
            id = 1L,
            senderId = 100,
            type = "ALERT",
            title = "Welcome",
            message = "You received a message",
            value = "value",
            heart = 10,
            weakHeart = 5,
            createdAt = sampleDate,
            expiredAt = sampleDate,
            readAt = sampleDate,
            usedAt = sampleDate,
            extraType = "info",
            extraId = 42,
            link = "http://example.com"
        )

        // when
        val domain = entity.toDomain()

        // then
        assertEquals(entity.id, domain.id)
        assertEquals(entity.senderId, domain.senderId)
        assertEquals(entity.type, domain.type)
        assertEquals(entity.title, domain.title)
        assertEquals(entity.message, domain.message)
        assertEquals(entity.value, domain.value)
        assertEquals(entity.heart, domain.heart)
        assertEquals(entity.weakHeart, domain.weakHeart)
        assertEquals(entity.createdAt, domain.createdAt)
        assertEquals(entity.expiredAt, domain.expiredAt)
        assertEquals(entity.readAt, domain.readAt)
        assertEquals(entity.usedAt, domain.usedAt)
        assertEquals(entity.extraType, domain.extraType)
        assertEquals(entity.extraId, domain.extraId)
        assertEquals(entity.link, domain.link)
    }

    @Test
    fun `toNotificationEntity should map Notification to NotificationEntity correctly`() {
        // given
        val domain = Notification(
            id = 1L,
            senderId = 100,
            type = "ALERT",
            title = "Welcome",
            message = "You received a message",
            value = "value",
            heart = 10,
            weakHeart = 5,
            createdAt = sampleDate,
            expiredAt = sampleDate,
            readAt = sampleDate,
            usedAt = sampleDate,
            extraType = "info",
            extraId = 42,
            link = "http://example.com"
        )

        // when
        val entity = domain.toNotificationEntity()

        // then
        assertEquals(domain.id, entity.id)
        assertEquals(domain.senderId, entity.senderId)
        assertEquals(domain.type, entity.type)
        assertEquals(domain.title, entity.title)
        assertEquals(domain.message, entity.message)
        assertEquals(domain.value, entity.value)
        assertEquals(domain.heart, entity.heart)
        assertEquals(domain.weakHeart, entity.weakHeart)
        assertEquals(domain.createdAt, entity.createdAt)
        assertEquals(domain.expiredAt, entity.expiredAt)
        assertEquals(domain.readAt, entity.readAt)
        assertEquals(domain.usedAt, entity.usedAt)
        assertEquals(domain.extraType, entity.extraType)
        assertEquals(domain.extraId, entity.extraId)
        assertEquals(domain.link, entity.link)
    }
}
