package net.ib.mn.local.room.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.ib.mn.local.model.NotificationLocal
import net.ib.mn.local.room.IdolDatabase
import org.junit.After
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
class NotificationDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var db: IdolDatabase

    private lateinit var dao: NotificationDao

    @Before
    fun setup() {
        hiltRule.inject()
        dao = db.notificationDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun createNotification(id: Long, createdAt: Date? = null): NotificationLocal {
        return NotificationLocal(
            id = id,
            senderId = 1,
            type = "test",
            title = "Test Title",
            message = "Test Message",
            value = "value",
            heart = 10,
            weakHeart = 5,
            createdAt = createdAt,
            expiredAt = null,
            readAt = null,
            usedAt = null,
            extraType = "type",
            extraId = 100,
            link = "link"
        )
    }

    @Test
    fun insertAndGetNotifications_returnsCorrect() = runTest {
        val notification1 = createNotification(1, Date(System.currentTimeMillis() - 10000))
        val notification2 = createNotification(2, Date(System.currentTimeMillis()))
        dao.insertNotifications(listOf(notification1, notification2))

        val result = dao.getLocalNotifications()

        assertEquals(2, result.size)
        assertTrue(result[0].createdAt!! >= result[1].createdAt!!)
    }

    @Test
    fun deleteNotification_removesSingleNotification() = runTest {
        val notification = createNotification(1)
        dao.insertNotifications(listOf(notification))

        dao.deleteNotification(1)

        val result = dao.getLocalNotifications()
        assertTrue(result.isEmpty())
    }

    @Test
    fun clearLocalNotifications_removesAll() = runTest {
        dao.insertNotifications(listOf(createNotification(1), createNotification(2)))
        dao.clearLocalNotifications()

        val result = dao.getLocalNotifications()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteOldNotifications_removesCorrectly() = runTest {
        val now = System.currentTimeMillis()
        val oldNotification = createNotification(1, Date(now - 100000))
        val recentNotification = createNotification(2, Date(now))

        dao.insertNotifications(listOf(oldNotification, recentNotification))

        dao.deleteOldNotifications(now - 1000)

        val result = dao.getLocalNotifications()
        assertEquals(1, result.size)
        assertEquals(2, result[0].id)
    }
}