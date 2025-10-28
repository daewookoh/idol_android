package net.ib.mn.domain.usecase.datastore

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.FreeBoardRepository
import org.junit.Before
import org.junit.Test

class SetFreeBoardSelectLangPrefsUseCaseTest {

    private lateinit var useCase: SetFreeBoardSelectLangPrefsUseCase
    private val repository: FreeBoardRepository = mockk()

    @Before
    fun setUp() {
        useCase = SetFreeBoardSelectLangPrefsUseCase(repository)
    }

    @Test
    fun `invoke should emit Loading and Success from repository`() = runTest {
        // given
        val language = "ja"
        val languageId = "ja_id"

        coEvery { repository.setFreeBoardSelectLanguage(language, languageId) } returns flowOf(
            DataResource.Loading(),
            DataResource.Success(Unit)
        )

        // when
        val result = useCase(language, languageId)

        // then
        result.test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isEqualTo(Unit)
            awaitComplete()
        }
    }

    @Test
    fun `invoke with null language should default to empty string`() = runTest {
        // given
        val languageId = "id"

        coEvery { repository.setFreeBoardSelectLanguage("", languageId) } returns flowOf(
            DataResource.Loading(),
            DataResource.Success(Unit)
        )

        // when
        val result = useCase(null, languageId)

        // then
        result.test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isEqualTo(Unit)
            awaitComplete()
        }
    }
}
