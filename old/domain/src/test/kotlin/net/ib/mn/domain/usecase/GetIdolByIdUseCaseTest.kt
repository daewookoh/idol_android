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

class GetIdolByIdUseCaseTest {

    private val idolRepository: IdolRepository = mockk()
    private lateinit var useCase: GetIdolByIdUseCase

    @Before
    fun setUp() {
        useCase = GetIdolByIdUseCase(idolRepository)
    }

    @Test
    fun `invoke returns Flow with Loading and Success`() = runTest {
        // given
        val dummyIdol = createDummyIdol(id = 1)
        coEvery { idolRepository.getIdolById(1) } returns flow {
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
