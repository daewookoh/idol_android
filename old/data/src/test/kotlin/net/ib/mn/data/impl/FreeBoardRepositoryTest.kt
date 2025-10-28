package net.ib.mn.data.impl

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.ib.mn.data.local.datastore.FreeBoardPrefsDataSource
import net.ib.mn.data.model.FreeBoardPrefsEntity
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.repository.FreeBoardRepository
import org.junit.Before
import org.junit.Test

class FreeBoardRepositoryTest {

    private lateinit var repository: FreeBoardRepository
    private val dataSource: FreeBoardPrefsDataSource = mockk(relaxed = true)

    @Before
    fun setup() {
        repository = FreeBoardRepositoryImpl(dataSource)
    }

    @Test
    fun `getFreeBoardPreference emits Loading and Success`() = runTest {
        val mockPrefs = FreeBoardPrefsEntity("en", "en_id")
        coEvery { dataSource.getFreeBoardPreference() } returns mockPrefs

        repository.getFreeBoardPreference().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem() as DataResource.Success
            assertThat(success.data?.selectLanguage).isEqualTo("en")
            assertThat(success.data?.selectLanguageId).isEqualTo("en_id")
            awaitComplete()
        }
    }

    @Test
    fun `setFreeBoardSelectLanguage completes successfully`() = runTest {
        coEvery { dataSource.setFreeBoardSelectLanguagePrefs("ja", "ja_id") } just Runs

        repository.setFreeBoardSelectLanguage("ja", "ja_id").test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val result = awaitItem()
            assertThat(result).isInstanceOf(DataResource.Success::class.java)
            awaitComplete()
        }

        coVerify(exactly = 1) { dataSource.setFreeBoardSelectLanguagePrefs("ja", "ja_id") }
    }
}

