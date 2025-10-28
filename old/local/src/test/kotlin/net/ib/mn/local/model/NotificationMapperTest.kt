package net.ib.mn.local.model

import net.ib.mn.data.model.NotificationEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class NotificationMapperTest {

    @Test
    fun `toLocal should map NotificationEntity to NotificationLocal correctly`() {
        val now = Date()
        val entity = NotificationEntity(
            id = 1L,
            senderId = 123,
            type = "INFO",
            title = "Test Title",
            message = "Test message",
            value = "Value",
            heart = 10,
            weakHeart = 5,
            createdAt = now,
            expiredAt = null,
            readAt = null,
            usedAt = null,
            extraType = "EXTRA",
            extraId = 999,
            link = "http://example.com"
        )

        val local = entity.toLocal()

        assertEquals(entity.id, local.id)
        assertEquals(entity.senderId, local.senderId)
        assertEquals(entity.type, local.type)
        assertEquals(entity.title, local.title)
        assertEquals(entity.message, local.message)
        assertEquals(entity.value, local.value)
        assertEquals(entity.heart, local.heart)
        assertEquals(entity.weakHeart, local.weakHeart)
        assertEquals(entity.createdAt, local.createdAt)
        assertEquals(entity.expiredAt, local.expiredAt)
        assertEquals(entity.readAt, local.readAt)
        assertEquals(entity.usedAt, local.usedAt)
        assertEquals(entity.extraType, local.extraType)
        assertEquals(entity.extraId, local.extraId)
        assertEquals(entity.link, local.link)
    }

    @Test
    fun `toData should map NotificationLocal to NotificationEntity correctly`() {
        val now = Date()
        val local = NotificationLocal(
            id = 2L,
            senderId = 456,
            type = "WARNING",
            title = "Local Title",
            message = "Local message",
            value = "Some value",
            heart = 20,
            weakHeart = 15,
            createdAt = now,
            expiredAt = now,
            readAt = null,
            usedAt = null,
            extraType = "BONUS",
            extraId = 888,
            link = null
        )

        val entity = local.toData()

        assertEquals(local.id, entity.id)
        assertEquals(local.senderId, entity.senderId)
        assertEquals(local.type, entity.type)
        assertEquals(local.title, entity.title)
        assertEquals(local.message, entity.message)
        assertEquals(local.value, entity.value)
        assertEquals(local.heart, entity.heart)
        assertEquals(local.weakHeart, entity.weakHeart)
        assertEquals(local.createdAt, entity.createdAt)
        assertEquals(local.expiredAt, entity.expiredAt)
        assertEquals(local.readAt, entity.readAt)
        assertEquals(local.usedAt, entity.usedAt)
        assertEquals(local.extraType, entity.extraType)
        assertEquals(local.extraId, entity.extraId)
        assertEquals(local.link, entity.link)
    }
}