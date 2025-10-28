package net.ib.mn.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.AwardsRepository
import net.ib.mn.domain.testutil.createDummyIdol
import org.junit.Before
import org.junit.Test

class GetAwardsIdolUseCaseTest {

    private val awardsRepository: AwardsRepository = mockk()
    private lateinit var useCase: GetAwardsIdolUseCase

    @Before
    fun setUp() {
        useCase = GetAwardsIdolUseCase(awardsRepository)
    }

    @Test
    fun `invoke returns Flow with Loading and Success`() = runTest {
        // given
        val dummyIdol = createDummyIdol(id = 1)
        coEvery { awardsRepository.getById(1) } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.success(dummyIdol))
        }

        // when
        val result = useCase(1).toList()

        // then
        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat((result[1] as DataResource.Success).data).isEqualTo(dummyIdol)
    }
}
