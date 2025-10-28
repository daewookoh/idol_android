package net.ib.mn.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.AwardsRepository
import net.ib.mn.domain.testutil.createDummyIdol
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DeleteAllAndSaveAwardsIdolUseCaseTest {

    private val repository = mockk<AwardsRepository>(relaxed = true)
    private lateinit var useCase: DeleteAllAndSaveAwardsIdolUseCase

    @Before
    fun setUp() {
        useCase = DeleteAllAndSaveAwardsIdolUseCase(repository)
    }

    @Test
    fun `invoke should call repository deleteAllAndInsert`() = runTest {
        // given
        val dummyIdolList = listOf(createDummyIdol(1), createDummyIdol(2))
        val dummyFlow = flowOf(DataResource.success(Unit))
        coEvery { repository.deleteAllAndInsert(dummyIdolList) } returns dummyFlow

        // when
        val result = useCase(dummyIdolList).toList()

        // then
        coVerify { repository.deleteAllAndInsert(dummyIdolList) }
        assertThat(result.first()).isInstanceOf(DataResource.Success::class.java)
    }
}
