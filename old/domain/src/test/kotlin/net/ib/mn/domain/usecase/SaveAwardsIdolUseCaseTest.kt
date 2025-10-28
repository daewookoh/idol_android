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

class SaveAwardsIdolUseCaseTest {

    private val repository: AwardsRepository = mockk()
    private lateinit var useCase: SaveAwardsIdolUseCase

    @Before
    fun setup() {
        useCase = SaveAwardsIdolUseCase(repository)
    }

    @Test
    fun `invoke should call repository and emit Loading then Success`() = runTest {
        val dummyIdols = listOf(createDummyIdol(1), createDummyIdol(2))

        coEvery { repository.insert(dummyIdols) } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.success(Unit))
        }

        val result = useCase(dummyIdols).toList()

        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
        assertThat((result[1] as DataResource.Success).data).isEqualTo(Unit)
    }
}
