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
class UpdateAdCountPrefsUseCaseTest {

    private lateinit var useCase: UpdateAdCountPrefsUseCase
    private val repository: AppRepository = mockk()

    @Before
    fun setup() {
        useCase = UpdateAdCountPrefsUseCase(repository)
    }

    @Test
    fun `invoke emits Loading and Success`() = runTest {
        val current = 2
        val max = 5

        coEvery { repository.updateAdCount(current, max) } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.Success(Unit))
        }

        useCase(current, max).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val result = awaitItem()
            assertThat(result).isInstanceOf(DataResource.Success::class.java)
            assertThat((result as DataResource.Success).data).isEqualTo(Unit)
            awaitComplete()
        }
    }

    @Test
    fun `invoke emits Loading and Error when repository fails`() = runTest {
        val current = 3
        val max = 6
        val exception = RuntimeException("Update failed")

        coEvery { repository.updateAdCount(current, max) } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.Error(exception))
        }

        useCase(current, max).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).hasMessageThat().isEqualTo("Update failed")
            awaitComplete()
        }
    }
}
