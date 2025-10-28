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

class IsSetAdNotificationPrefsUseCaseTest {

    private lateinit var isSetAdNotificationPrefsUseCase: IsSetAdNotificationPrefsUseCase
    private val appRepository: AppRepository = mockk()

    @Before
    fun setUp() {
        isSetAdNotificationPrefsUseCase = IsSetAdNotificationPrefsUseCase(appRepository)
    }

    @Test
    fun `isSetAdNotificationPrefsUseCase emits Loading and Success true`() = runTest {
        coEvery { appRepository.isSetAdNotification() } returns flowOf(
            DataResource.Loading(),
            DataResource.Success(true)
        )

        val result = isSetAdNotificationPrefsUseCase()

        result.test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem() as DataResource.Success
            assertThat(success.data).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `isSetAdNotificationPrefsUseCase emits Loading and Success false`() = runTest {
        coEvery { appRepository.isSetAdNotification() } returns flowOf(
            DataResource.Loading(),
            DataResource.Success(false)
        )

        val result = isSetAdNotificationPrefsUseCase()

        result.test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem() as DataResource.Success
            assertThat(success.data).isFalse()
            awaitComplete()
        }
    }
}