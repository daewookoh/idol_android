package net.ib.mn.domain.usecase.datastore

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.FreeBoardPrefs
import net.ib.mn.domain.repository.FreeBoardRepository
import org.junit.Before
import org.junit.Test

class GetFreeBoardPrefsUseCaseTest {

    private lateinit var useCase: GetFreeBoardPrefsUseCase
    private val repository: FreeBoardRepository = mockk()

    @Before
    fun setUp() {
        useCase = GetFreeBoardPrefsUseCase(repository)
    }

    @Test
    fun `invoke should emit Loading and Success from repository`() = runTest {
        // given
        val dummyPrefs = FreeBoardPrefs(selectLanguage = "ko", selectLanguageId = "ko_id")
        coEvery { repository.getFreeBoardPreference() } returns flowOf(
            DataResource.Loading(),
            DataResource.success(dummyPrefs)
        )

        // when
        val result = useCase()

        // then
        result.test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem() as DataResource.Success
            assertThat(success.data).isEqualTo(dummyPrefs)
            awaitComplete()
        }
    }
}
