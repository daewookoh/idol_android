package net.ib.mn.local.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.ib.mn.data.local.NotificationLocalDataSource
import net.ib.mn.data.model.NotificationEntity
import net.ib.mn.local.room.dao.NotificationDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import javax.inject.Inject

@HiltAndroidTest
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class NotificationLocalDataSourceImplTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var notificationDao: NotificationDao

    @Inject
    lateinit var dataSource: NotificationLocalDataSource

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun saveAndGetNotifications_returnsCorrectData() = runTest {
        val dummyList = listOf(createNotificationEntity(1), createNotificationEntity(2))
        dataSource.saveNotifications(dummyList)

        val loaded = dataSource.getNotifications().sortedBy { it.id }

        val expected = dummyList.sortedBy { it.id }

        assertEquals(expected.size, loaded.size)
        expected.zip(loaded).forEach { (e, l) ->
            assertEquals(e.id, l.id)
        }
    }

    @Test
    fun deleteNotification_removesNotification() = runTest {
        val dummy = createNotificationEntity(10)
        dataSource.saveNotifications(listOf(dummy))
        dataSource.deleteNotification(10)

        val all = dataSource.getNotifications()
        assertTrue(all.none { it.id == 10L })
    }

    @Test
    fun deleteAllNotifications_clearsTable() = runTest {
        dataSource.saveNotifications(listOf(createNotificationEntity(1), createNotificationEntity(2)))
        dataSource.deleteAllNotifications()

        val result = dataSource.getNotifications()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteOldNotifications_removesExpiredItems() = runTest {
        val now = Date().time
        val recent = createNotificationEntity(1, now)
        val old = createNotificationEntity(2, now - 100_000L)

        dataSource.saveNotifications(listOf(recent, old))
        dataSource.deleteOldNotifications(now - 50_000L)

        val result = dataSource.getNotifications()
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    private fun createNotificationEntity(id: Long, createdAt: Long = Date().time): NotificationEntity {
        return NotificationEntity(
            id = id,
            senderId = 1,
            type = "test",
            title = "Test Notification",
            message = "Test Message",
            value = "value",
            heart = 10,
            weakHeart = 5,
            createdAt = Date(createdAt),
            expiredAt = null,
            readAt = null,
            usedAt = null,
            extraType = "type",
            extraId = 123,
            link = "https://test.com"
        )
    }
}