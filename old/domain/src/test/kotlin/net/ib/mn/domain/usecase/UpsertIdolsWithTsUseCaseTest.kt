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

class UpsertIdolsWithTsUseCaseTest {

    private val idolRepository: IdolRepository = mockk()
    private lateinit var useCase: UpsertIdolsWithTsUseCase

    @Before
    fun setup() {
        useCase = UpsertIdolsWithTsUseCase(idolRepository)
    }

    @Test
    fun `invoke should emit Loading then Success with true`() = runTest {
        // given
        val dummyIdols = listOf(createDummyIdol(1), createDummyIdol(2))
        val ts = 1234567890

        coEvery { idolRepository.upsertWithTs(dummyIdols, ts) } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.success(true))
        }

        // when
        val result = useCase(dummyIdols, ts).toList()

        // then
        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
        assertThat((result[1] as DataResource.Success).data).isTrue()
    }
}
