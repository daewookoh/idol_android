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

class DeleteOldNotificationsUseCaseTest {

    private lateinit var repository: NotificationRepository
    private lateinit var useCase: DeleteOldNotificationsUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = DeleteOldNotificationsUseCase(repository)
    }

    @Test
    fun `invoke should emit success`() = runTest {
        val timestamp = 1712832000000L
        coEvery { repository.deleteOldNotifications(timestamp) } returns flowOf(DataResource.success(Unit))

        useCase(timestamp).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Success::class.java)
            awaitComplete()
        }
    }
}
