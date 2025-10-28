package net.ib.mn.domain.usecase.datastore

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.AppRepository
import org.junit.Before
import org.junit.Test

class SetAdNotificationPrefsUseCaseTest {

    private lateinit var setAdNotificationPrefsUseCase: SetAdNotificationPrefsUseCase
    private val appRepository: AppRepository = mockk()

    @Before
    fun setUp() {
        setAdNotificationPrefsUseCase = SetAdNotificationPrefsUseCase(appRepository)
    }

    @Test
    fun `setAdNotificationPrefsUseCase emits Loading and Success`() = runTest {
        coEvery { appRepository.setAdNotification(true) } returns flowOf(
            DataResource.Loading(),
            DataResource.Success(Unit)
        )

        val result = setAdNotificationPrefsUseCase(true)

        result.test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem() as DataResource.Success
            assertThat(success.data).isEqualTo(Unit)
            awaitComplete()
        }
    }

    @Test
    fun `setAdNotificationPrefsUseCase emits Error when exception thrown`() = runTest {
        coEvery { appRepository.setAdNotification(true) } returns flowOf(
            DataResource.Loading(),
            DataResource.Error(RuntimeException("set failed"))
        )

        val result = setAdNotificationPrefsUseCase(true)

        result.test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem() as DataResource.Error
            assertThat(error.throwable).hasMessageThat().isEqualTo("set failed")
            awaitComplete()
        }
    }
}