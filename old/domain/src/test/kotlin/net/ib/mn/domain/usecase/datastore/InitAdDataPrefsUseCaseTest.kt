package net.ib.mn.domain.usecase.datastore

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.AppRepository
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class InitAdDataPrefsUseCaseTest {

    private lateinit var useCase: InitAdDataPrefsUseCase
    private val repository: AppRepository = mockk()

    @Before
    fun setup() {
        useCase = InitAdDataPrefsUseCase(repository)
    }

    @Test
    fun `invoke emits initAdData when adDate is 0L`() = runTest {
        val kstMidnight = 1000L
        val adData = Triple(1, 3, 0L)

        coEvery { repository.getAdData() } returns flowOf(DataResource.Success(adData))
        coEvery { repository.initAdData(kstMidnight) } returns flowOf(DataResource.Success(Unit))

        useCase(kstMidnight).test {
            val loading = awaitItem()
            assertThat(loading).isInstanceOf(DataResource.Success::class.java)
            assertThat((loading as DataResource.Success).data).isEqualTo(Unit)
            awaitComplete()
        }
    }

    @Test
    fun `invoke skips initAdData when adDate is after kstMidnight`() = runTest {
        val kstMidnight = 1000L
        val adData = Triple(0, 0, 2000L)

        coEvery { repository.getAdData() } returns flowOf(DataResource.Success(adData))

        useCase(kstMidnight).test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(DataResource.Success::class.java)
            assertThat((result as DataResource.Success).data).isEqualTo(Unit)
            awaitComplete()
        }
    }

    @Test
    fun `invoke emits error when getAdData fails`() = runTest {
        val exception = RuntimeException("load failed")

        coEvery { repository.getAdData() } returns flowOf(DataResource.Error(exception))

        useCase(1234L).test {
            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).hasMessageThat().isEqualTo("load failed")
            awaitComplete()
        }
    }

    @Test
    fun `invoke emits error when adData is null`() = runTest {
        // mapDataResource 내부에서 null일 수도 있다는 가정을 반영
        coEvery { repository.getAdData() } returns flowOf(DataResource.Success(null))

        useCase(1234L).test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(DataResource.Error::class.java)
            assertThat((result as DataResource.Error).throwable).isInstanceOf(IllegalStateException::class.java)
            assertThat(result.throwable.message).isEqualTo("AdData is null")
            awaitComplete()
        }
    }

    @Test
    fun `invoke emits loading if getAdData returns loading`() = runTest {
        coEvery { repository.getAdData() } returns flowOf(DataResource.Loading())

        useCase(1234L).test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(DataResource.Loading::class.java)
            awaitComplete()
        }
    }
}
