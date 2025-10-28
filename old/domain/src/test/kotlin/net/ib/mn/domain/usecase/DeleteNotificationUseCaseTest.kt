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

class DeleteNotificationUseCaseTest {

    private lateinit var repository: NotificationRepository
    private lateinit var useCase: DeleteNotificationUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = DeleteNotificationUseCase(repository)
    }

    @Test
    fun `invoke should emit success`() = runTest {
        val id = 123L
        coEvery { repository.deleteNotification(id) } returns flowOf(DataResource.success(Unit))

        useCase(id).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Success::class.java)
            awaitComplete()
        }
    }
}
