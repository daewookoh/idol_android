package net.ib.mn.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.NotificationRepository
import net.ib.mn.domain.testutil.createDummyNotification
import org.junit.Before
import org.junit.Test

class SaveNotificationsUseCaseTest {

    private val repository: NotificationRepository = mockk()
    private lateinit var useCase: SaveNotificationsUseCase

    @Before
    fun setup() {
        useCase = SaveNotificationsUseCase(repository)
    }

    @Test
    fun `invoke should emit Loading then Success`() = runTest {
        val notifications = listOf(
            createDummyNotification(1, "Welcome"),
            createDummyNotification(2, "Update Available")
        )

        coEvery { repository.saveNotifications(notifications) } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.success(Unit))
        }

        val result = useCase(notifications).toList()

        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
        assertThat((result[1] as DataResource.Success).data).isEqualTo(Unit)
    }
}
