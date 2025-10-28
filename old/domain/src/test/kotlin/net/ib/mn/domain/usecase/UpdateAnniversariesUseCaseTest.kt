package net.ib.mn.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.Anniversary
import net.ib.mn.domain.repository.IdolRepository
import org.junit.Before
import org.junit.Test

class UpdateAnniversariesUseCaseTest {

    private val repository: IdolRepository = mockk()
    private lateinit var useCase: UpdateAnniversariesUseCase

    @Before
    fun setup() {
        useCase = UpdateAnniversariesUseCase(repository)
    }

    @Test
    fun `invoke should emit Loading then Success`() = runTest {
        val cdnUrl = "https://cdn.example.com"
        val imageSize = "300"
        val anniversaryList = listOf(
            Anniversary(
                idolId = 1,
                anniversary = "D",
                anniversaryDays = 10,
                burningDay = "2025-04-12",
                heart = 1000L,
                top3 = "1,2,3",
                top3Type = "P,P,V"
            )
        )

        coEvery {
            repository.updateAnniversaries(cdnUrl, imageSize, anniversaryList)
        } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.success(Unit))
        }

        val result = useCase(cdnUrl, imageSize, anniversaryList).toList()

        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
        assertThat((result[1] as DataResource.Success).data).isEqualTo(Unit)
    }
}
