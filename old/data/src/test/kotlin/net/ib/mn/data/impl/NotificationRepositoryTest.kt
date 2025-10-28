package net.ib.mn.data.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ib.mn.data.local.NotificationLocalDataSource
import net.ib.mn.data.model.toNotificationEntity
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.Notification
import net.ib.mn.domain.repository.NotificationRepository
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepositoryTest {

    private lateinit var repository: NotificationRepository
    private val localDataSource: NotificationLocalDataSource = mockk(relaxed = true)

    @Before
    fun setup() {
        repository = NotificationRepositoryImpl(localDataSource)
    }

    private fun createNotification(id: Long) = Notification(
        id = id,
        senderId = 0,
        type = null,
        title = "Test",
        message = "Message",
        value = "value",
        heart = 1,
        weakHeart = 1,
        createdAt = null,
        expiredAt = null,
        readAt = null,
        usedAt = null,
        extraType = "",
        extraId = 0,
        link = null
    )

    @Test
    fun `saveNotifications emits Loading and Success`() = runTest {
        val list = listOf(createNotification(1), createNotification(2))
        val result = repository.saveNotifications(list).toList()

        coVerify { localDataSource.saveNotifications(any()) }
        assertThat(result.first()).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result.last()).isInstanceOf(DataResource.Success::class.java)
    }

    @Test
    fun `getNotifications emits correct data`() = runTest {
        val dummyList = listOf(createNotification(1), createNotification(2))
        coEvery { localDataSource.getNotifications() } returns dummyList.map { it.toNotificationEntity() }

        val result = repository.getNotifications().toList()

        coVerify { localDataSource.getNotifications() }
        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat((result[1] as DataResource.Success).data).containsExactlyElementsIn(dummyList)
    }

    @Test
    fun `deleteNotification emits Loading and Success`() = runTest {
        val result = repository.deleteNotification(1L).toList()

        coVerify { localDataSource.deleteNotification(1L) }
        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
    }

    @Test
    fun `deleteAllNotification emits Loading and Success`() = runTest {
        val result = repository.deleteAllNotification().toList()

        coVerify { localDataSource.deleteAllNotifications() }
        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
    }

    @Test
    fun `deleteOldNotifications emits Loading and Success`() = runTest {
        val result = repository.deleteOldNotifications(123456789).toList()

        coVerify { localDataSource.deleteOldNotifications(123456789) }
        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
    }
}
