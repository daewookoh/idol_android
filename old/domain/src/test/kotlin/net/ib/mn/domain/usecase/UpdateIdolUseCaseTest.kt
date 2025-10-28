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

class UpdateIdolUseCaseTest {

    private val idolRepository: IdolRepository = mockk()
    private lateinit var useCase: UpdateIdolUseCase

    @Before
    fun setup() {
        useCase = UpdateIdolUseCase(idolRepository)
    }

    @Test
    fun `invoke should emit Loading then Success`() = runTest {
        // given
        val idol = createDummyIdol(1)

        coEvery { idolRepository.updateIdol(idol) } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.success(Unit))
        }

        // when
        val result = useCase(idol).toList()

        // then
        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
        assertThat((result[1] as DataResource.Success).data).isEqualTo(Unit)
    }
}
