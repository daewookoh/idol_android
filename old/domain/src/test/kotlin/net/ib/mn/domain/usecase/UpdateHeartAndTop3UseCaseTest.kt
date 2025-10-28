package net.ib.mn.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.IdolFiledData
import net.ib.mn.domain.repository.IdolRepository
import org.junit.Before
import org.junit.Test

class UpdateHeartAndTop3UseCaseTest {

    private val idolRepository: IdolRepository = mockk()
    private lateinit var useCase: UpdateHeartAndTop3UseCase

    @Before
    fun setup() {
        useCase = UpdateHeartAndTop3UseCase(idolRepository)
    }

    @Test
    fun `invoke should emit Loading then Success`() = runTest {
        // given
        val cdnUrl = "https://cdn.example.com"
        val imageSize = "300"
        val idolFiledDataList = listOf(
            IdolFiledData(
                id = 1,
                heart = 1000L,
                top3 = "1,2,3",
                top3Type = "P,V,P"
            )
        )

        coEvery {
            idolRepository.updateHeartAndTop3(cdnUrl, imageSize, idolFiledDataList)
        } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.success(Unit))
        }

        // when
        val result = useCase(cdnUrl, imageSize, idolFiledDataList).toList()

        // then
        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
        assertThat((result[1] as DataResource.Success).data).isEqualTo(Unit)
    }
}
