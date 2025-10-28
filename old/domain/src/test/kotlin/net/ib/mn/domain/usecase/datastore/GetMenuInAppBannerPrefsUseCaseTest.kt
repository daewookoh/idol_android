package net.ib.mn.domain.usecase.datastore

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.model.InAppBannerType
import net.ib.mn.domain.repository.AppRepository
import org.junit.Before
import org.junit.Test

class GetMenuInAppBannerPrefsUseCaseTest {

    private val appRepository: AppRepository = mockk()
    private lateinit var getMenuUseCase: GetMenuInAppBannerPrefsUseCase

    @Before
    fun setup() {
        getMenuUseCase = GetMenuInAppBannerPrefsUseCase(appRepository)
    }

    @Test
    fun `getMenuUseCase should emit Loading and Success`() = runTest {
        val dummy = listOf(
            InAppBanner(id = 1, imageUrl = "url1", link = "link1", section = "M")
        )

        coEvery {
            appRepository.getInAppBannerData(InAppBannerType.MENU)
        } returns flowOf(
            DataResource.Loading(),
            DataResource.success(dummy)
        )

        getMenuUseCase().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem() as DataResource.Success
            assertThat(success.data).isEqualTo(dummy)
            awaitComplete()
        }
    }

    @Test
    fun `invoke should emit Loading and Error when repository throws`() = runTest {
        val exception = RuntimeException("Failed to get menu banners")

        coEvery { appRepository.getInAppBannerData(InAppBannerType.MENU) } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.Error(exception))
        }

        getMenuUseCase().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem() as DataResource.Error
            assertThat(error.throwable).isEqualTo(exception)
            awaitComplete()
        }
    }
}