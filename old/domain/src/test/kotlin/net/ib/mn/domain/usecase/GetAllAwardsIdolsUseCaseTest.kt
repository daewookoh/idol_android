package net.ib.mn.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.AwardsRepository
import net.ib.mn.domain.testutil.createDummyIdol
import org.junit.Before
import org.junit.Test

class GetAllAwardsIdolsUseCaseTest {

    private val repository: AwardsRepository = mockk()
    private lateinit var useCase: GetAllAwardsIdolsUseCase

    @Before
    fun setup() {
        useCase = GetAllAwardsIdolsUseCase(repository)
    }

    @Test
    fun `invoke returns idol list successfully`() = runTest {
        val dummyIdols = listOf(createDummyIdol(1), createDummyIdol(2))
        coEvery { repository.getAll() } returns flowOf(DataResource.success(dummyIdols))

        useCase().test {
            val result = awaitItem()
            assertThat((result as DataResource.Success).data).isEqualTo(dummyIdols)
            awaitComplete()
        }
    }
}