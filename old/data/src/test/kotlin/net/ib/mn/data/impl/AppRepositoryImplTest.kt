package net.ib.mn.data.impl

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.ib.mn.data.local.datastore.AppPrefsDataSource
import net.ib.mn.data.model.InAppBannerPrefsEntity
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.model.InAppBannerType
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AppRepositoryImplTest {

    private lateinit var repository: AppRepositoryImpl
    private val appPrefsDataSource: AppPrefsDataSource = mockk()

    @Before
    fun setup() {
        repository = AppRepositoryImpl(appPrefsDataSource)
    }

    @Test
    fun `setInAppBannerData emits Loading and Success`() = runTest {
        val input = mapOf(
            "M" to listOf(
                InAppBanner(id = 1, imageUrl = "url1", link = "link1", section = "M")
            )
        )

        coEvery { appPrefsDataSource.setBannerData(any()) } just Runs

        repository.setInAppBannerData(input).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isEqualTo(Unit)
            awaitComplete()
        }
    }

    @Test
    fun `setInAppBannerData emits Loading and Error when exception thrown`() = runTest {
        val input = mapOf(
            "M" to listOf(
                InAppBanner(id = 1, imageUrl = "url1", link = "link1", section = "M")
            )
        )

        coEvery { appPrefsDataSource.setBannerData(any()) } throws RuntimeException("save failed")

        repository.setInAppBannerData(input).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).hasMessageThat().isEqualTo("save failed")
            awaitComplete()
        }
    }

    @Test
    fun `getInAppBannerData returns banner list for MENU`() = runTest {
        val prefsList = listOf(
            InAppBannerPrefsEntity(1, "url", "link", "M")
        )
        val expected = prefsList.map { it.toDomain() } // ← InAppBanner로 변환

        coEvery { appPrefsDataSource.getMenuBannerData() } returns prefsList

        repository.getInAppBannerData(InAppBannerType.MENU).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isEqualTo(expected)
            awaitComplete()
        }
    }

    @Test
    fun `getInAppBannerData emits Error for MENU when exception thrown`() = runTest {
        coEvery { appPrefsDataSource.getMenuBannerData() } throws RuntimeException("load failed")

        repository.getInAppBannerData(InAppBannerType.MENU).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).hasMessageThat().isEqualTo("load failed")
            awaitComplete()
        }
    }

    @Test
    fun `getInAppBannerData returns banner list for SEARCH`() = runTest {
        val prefsList = listOf(
            InAppBannerPrefsEntity(2, "url2", "link2", "S")
        )
        val expected = prefsList.map { it.toDomain() }

        coEvery { appPrefsDataSource.getSearchBannerData() } returns prefsList

        repository.getInAppBannerData(InAppBannerType.SEARCH).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isEqualTo(expected)
            awaitComplete()
        }
    }

    @Test
    fun `getInAppBannerData emits Error for SEARCH when exception thrown`() = runTest {
        coEvery { appPrefsDataSource.getSearchBannerData() } throws RuntimeException("load search failed")

        repository.getInAppBannerData(InAppBannerType.SEARCH).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).hasMessageThat().isEqualTo("load search failed")
            awaitComplete()
        }
    }

    @Test
    fun `initAdData emits Loading and Success`() = runTest {
        val date = 1234567890L
        coEvery { appPrefsDataSource.initAdData(date) } just Runs

        repository.initAdData(date).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isEqualTo(Unit)
            awaitComplete()
        }
    }

    @Test
    fun `initAdData emits Error when exception thrown`() = runTest {
        val date = 1234567890L
        coEvery { appPrefsDataSource.initAdData(date) } throws RuntimeException("init failed")

        repository.initAdData(date).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).hasMessageThat().isEqualTo("init failed")
            awaitComplete()
        }
    }

    @Test
    fun `updateAdCount emits Loading and Success`() = runTest {
        coEvery { appPrefsDataSource.updateAdCount(3, 5) } just Runs

        repository.updateAdCount(3, 5).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isEqualTo(Unit)
            awaitComplete()
        }
    }

    @Test
    fun `updateAdCount emits Error when exception thrown`() = runTest {
        coEvery { appPrefsDataSource.updateAdCount(3, 5) } throws RuntimeException("update failed")

        repository.updateAdCount(3, 5).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).hasMessageThat().isEqualTo("update failed")
            awaitComplete()
        }
    }

    @Test
    fun `getAdData emits Loading and Success`() = runTest {
        val expected = Triple(1, 3, 1234567890L)
        coEvery { appPrefsDataSource.getAdData() } returns expected

        repository.getAdData().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isEqualTo(expected)
            awaitComplete()
        }
    }

    @Test
    fun `getAdData emits Error when exception thrown`() = runTest {
        coEvery { appPrefsDataSource.getAdData() } throws RuntimeException("get failed")

        repository.getAdData().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).hasMessageThat().isEqualTo("get failed")
            awaitComplete()
        }
    }

    @Test
    fun `isEnabledVideoAd emits Loading and Success true`() = runTest {
        coEvery { appPrefsDataSource.isEnabledVideoAd() } returns true

        repository.isEnabledVideoAd().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `isEnabledVideoAd emits Error when exception thrown`() = runTest {
        coEvery { appPrefsDataSource.isEnabledVideoAd() } throws RuntimeException("check failed")

        repository.isEnabledVideoAd().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).hasMessageThat().isEqualTo("check failed")
            awaitComplete()
        }
    }

    @Test
    fun `getAdCount emits Loading and Success`() = runTest {
        val expectedCount = 3
        coEvery { appPrefsDataSource.getAdCount() } returns expectedCount

        repository.getAdCount().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isEqualTo(expectedCount)
            awaitComplete()
        }
    }

    @Test
    fun `getAdCount emits Loading and Error when exception thrown`() = runTest {
        coEvery { appPrefsDataSource.getAdCount() } throws RuntimeException("getAdCount failed")

        repository.getAdCount().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).hasMessageThat().isEqualTo("getAdCount failed")
            awaitComplete()
        }
    }

    @Test
    fun `setAdNotification emits Loading and Success`() = runTest {
        coEvery { appPrefsDataSource.setAdNotification(true) } just Runs

        repository.setAdNotification(true).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isEqualTo(Unit)
            awaitComplete()
        }
    }

    @Test
    fun `setAdNotification emits Error when exception thrown`() = runTest {
        coEvery { appPrefsDataSource.setAdNotification(true) } throws RuntimeException("set failed")

        repository.setAdNotification(true).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).hasMessageThat().isEqualTo("set failed")
            awaitComplete()
        }
    }

    @Test
    fun `isSetAdNotification emits Loading and Success true`() = runTest {
        coEvery { appPrefsDataSource.isSetAdNotification() } returns true

        repository.isSetAdNotification().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `isSetAdNotification emits Loading and Success false`() = runTest {
        coEvery { appPrefsDataSource.isSetAdNotification() } returns false

        repository.isSetAdNotification().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(DataResource.Success::class.java)
            assertThat((success as DataResource.Success).data).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `isSetAdNotification emits Error when exception thrown`() = runTest {
        coEvery { appPrefsDataSource.isSetAdNotification() } throws RuntimeException("check failed")

        repository.isSetAdNotification().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(DataResource.Error::class.java)
            assertThat((error as DataResource.Error).throwable).hasMessageThat().isEqualTo("check failed")
            awaitComplete()
        }
    }
}
