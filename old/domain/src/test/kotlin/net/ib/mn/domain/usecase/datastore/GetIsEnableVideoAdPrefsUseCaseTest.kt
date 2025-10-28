package net.ib.mn.domain.usecase.datastore

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.AppRepository
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class GetIsEnableVideoAdPrefsUseCaseTest {

    private lateinit var useCase: GetIsEnableVideoAdPrefsUseCase
    private val appRepository: AppRepository = mockk()

    @Before
    fun setup() {
        useCase = GetIsEnableVideoAdPrefsUseCase(appRepository)
    }

    @Test
    fun `invoke returns true when video ad is enabled`() = runTest {
        coEvery { appRepository.isEnabledVideoAd() } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.Success(true))
        }

        useCase().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val result = awaitItem()
            assertThat(result).isInstanceOf(DataResource.Success::class.java)
            assertThat((result as DataResource.Success).data).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `invoke returns false when video ad is disabled`() = runTest {
        coEvery { appRepository.isEnabledVideoAd() } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.Success(false))
        }

        useCase().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val result = awaitItem()
            assertThat(result).isInstanceOf(DataResource.Success::class.java)
            assertThat((result as DataResource.Success).data).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `invoke emits error when repository throws`() = runTest {
        val exception = RuntimeException("failed to check video ad status")

        coEvery { appRepository.isEnabledVideoAd() } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.Error(exception))
        }

        useCase().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val result = awaitItem()
            assertThat(result).isInstanceOf(DataResource.Error::class.java)
            assertThat((result as DataResource.Error).throwable).hasMessageThat().isEqualTo("failed to check video ad status")
            awaitComplete()
        }
    }
}
