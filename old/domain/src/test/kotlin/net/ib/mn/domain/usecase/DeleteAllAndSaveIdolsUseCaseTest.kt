package net.ib.mn.domain.usecase

import net.ib.mn.domain.testutil.createDummyIdol
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.data_resource.DataResource
import org.junit.Before
import org.junit.Test

class DeleteAllAndSaveIdolsUseCaseTest {

    private lateinit var useCase: DeleteAllAndSaveIdolsUseCase
    private val repository: IdolRepository = mockk()

    @Before
    fun setUp() {
        useCase = DeleteAllAndSaveIdolsUseCase(repository)
    }

    @Test
    fun `invoke should call repository and emit Loading and Success`() = runTest {
        // given
        val dummyIdols = listOf(createDummyIdol(1), createDummyIdol(2))
        coEvery { repository.deleteAllAndSaveIdols(dummyIdols) } returns flowOf(
            DataResource.Loading(),
            DataResource.success(Unit)
        )

        // when
        val flow = useCase(dummyIdols)

        // then
        flow.test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isEqualTo(Unit)
            awaitComplete()
        }
    }
}
