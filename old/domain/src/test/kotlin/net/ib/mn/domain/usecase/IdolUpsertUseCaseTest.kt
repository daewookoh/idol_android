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

class IdolUpsertUseCaseTest {

    private val repository: IdolRepository = mockk()
    private lateinit var useCase: IdolUpsertUseCase

    @Before
    fun setup() {
        useCase = IdolUpsertUseCase(repository)
    }

    @Test
    fun `invoke should call repository and emit Loading then Success`() = runTest {
        val dummyIdol = createDummyIdol(1)
        coEvery { repository.upsert(dummyIdol) } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.success(Unit))
        }

        val result = useCase(dummyIdol).toList()

        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
        assertThat((result[1] as DataResource.Success).data).isEqualTo(Unit)
    }
}