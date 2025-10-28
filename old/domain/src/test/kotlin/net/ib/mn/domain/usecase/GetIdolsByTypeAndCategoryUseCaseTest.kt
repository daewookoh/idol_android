package net.ib.mn.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.domain.testutil.createDummyIdol
import org.junit.Before
import org.junit.Test

class GetIdolsByTypeAndCategoryUseCaseTest {

    private val idolRepository: IdolRepository = mockk()
    private lateinit var useCase: GetIdolsByTypeAndCategoryUseCase

    @Before
    fun setUp() {
        useCase = GetIdolsByTypeAndCategoryUseCase(idolRepository)
    }

    @Test
    fun `invoke with type and category returns Loading and Success`() = runTest {
        // given
        val dummyIdols = listOf(
            createDummyIdol(id = 1),
            createDummyIdol(id = 2)
        )
        coEvery { idolRepository.getIdolByTypeAndCategory("A", "K") } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.success(dummyIdols))
        }

        // when
        val result = useCase("A", "K").toList()

        // then
        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat((result[1] as DataResource.Success).data).containsExactlyElementsIn(dummyIdols)
    }
}
