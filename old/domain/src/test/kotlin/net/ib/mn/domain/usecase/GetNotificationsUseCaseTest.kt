package net.ib.mn.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.Notification
import net.ib.mn.domain.repository.NotificationRepository
import net.ib.mn.domain.testutil.createDummyNotification
import org.junit.Before
import org.junit.Test

class GetNotificationsUseCaseTest {

    private val notificationRepository: NotificationRepository = mockk()
    private lateinit var useCase: GetNotificationsUseCase

    @Before
    fun setUp() {
        useCase = GetNotificationsUseCase(notificationRepository)
    }

    @Test
    fun `invoke returns Loading and Success with notifications`() = runTest {
        // given
        val dummyNotifications = listOf(
            createDummyNotification(id = 1L, message = "Message 1"),
            createDummyNotification(id = 2L, message = "Message 2"),
        )
        coEvery { notificationRepository.getNotifications() } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.success(dummyNotifications))
        }

        // when
        val result = useCase().toList()

        // then
        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat((result[1] as DataResource.Success).data).containsExactlyElementsIn(dummyNotifications)
    }
}
