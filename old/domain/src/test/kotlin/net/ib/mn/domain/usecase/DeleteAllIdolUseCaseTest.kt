package net.ib.mn.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.IdolRepository
import org.junit.Before
import org.junit.Test

class DeleteAllIdolUseCaseTest {

    private lateinit var repository: IdolRepository
    private lateinit var useCase: DeleteAllIdolUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = DeleteAllIdolUseCase(repository)
    }

    @Test
    fun `invoke should emit success`() = runTest {
        coEvery { repository.deleteAllIdol() } returns flowOf(DataResource.success(Unit))

        useCase().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Success::class.java)
            awaitComplete()
        }
    }
}
