package net.ib.mn.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.domain.testutil.createDummyIdol
import org.junit.Before
import org.junit.Test

class GetAllIdolsUseCaseTest {

    private val idolRepository: IdolRepository = mockk()
    private lateinit var useCase: GetAllIdolsUseCase

    @Before
    fun setup() {
        useCase = GetAllIdolsUseCase(idolRepository)
    }

    @Test
    fun `invoke returns Loading and then Success with list`() = runTest {
        val dummyList = listOf(createDummyIdol(1), createDummyIdol(2))

        coEvery { idolRepository.getAllIdols() } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.success(dummyList))
        }

        val result = useCase().toList()

        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
        assertThat((result[1] as DataResource.Success).data).isEqualTo(dummyList)
    }
}
