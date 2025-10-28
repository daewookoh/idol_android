package net.ib.mn.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.AwardsRepository
import org.junit.Before
import org.junit.Test

class DeleteAllAwardsIdolUseCaseTest {

    private lateinit var useCase: DeleteAllAwardsIdolUseCase
    private val repository: AwardsRepository = mockk()

    @Before
    fun setUp() {
        useCase = DeleteAllAwardsIdolUseCase(repository)
    }

    @Test
    fun `invoke should emit Loading and Success`() = runTest {
        // given
        coEvery { repository.deleteAll() } returns flowOf(
            DataResource.Loading(),
            DataResource.success(Unit)
        )

        // when
        val flow = useCase()

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
