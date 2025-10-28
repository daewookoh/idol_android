package net.ib.mn.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.NotificationRepository
import org.junit.Before
import org.junit.Test

class DeleteAllNotificationUseCaseTest {

    private lateinit var repository: NotificationRepository
    private lateinit var useCase: DeleteAllNotificationUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = DeleteAllNotificationUseCase(repository)
    }

    @Test
    fun `invoke should emit success`() = runTest {
        coEvery { repository.deleteAllNotification() } returns flowOf(DataResource.success(Unit))

        useCase().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Success::class.java)
            awaitComplete()
        }
    }
}
