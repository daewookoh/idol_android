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
import net.ib.mn.domain.repository.AppRepository
import org.junit.Before
import org.junit.Test

class SetInAppBannerPrefsUseCaseTest {

    private val appRepository: AppRepository = mockk()
    private lateinit var useCase: SetInAppBannerPrefsUseCase

    @Before
    fun setup() {
        useCase = SetInAppBannerPrefsUseCase(appRepository)
    }

    @Test
    fun `setUseCase should call repository and emit Loading and Success`() = runTest {
        val input = mapOf(
            "M" to listOf(InAppBanner(1, "url1", "link1", "M")),
            "S" to listOf(InAppBanner(2, "url2", "link2", "S"))
        )

        coEvery {
            appRepository.setInAppBannerData(input)
        } returns flowOf(
            DataResource.Loading(),
            DataResource.success(Unit)
        )

        useCase(input).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem() as DataResource.Success
            assertThat(success.data).isEqualTo(Unit)
            awaitComplete()
        }
    }

    @Test
    fun `invoke should emit Loading and Error when repository throws`() = runTest {
        val exception = RuntimeException("set error")
        val bannerMap = mapOf(
            "M" to listOf(
                InAppBanner(id = 1, imageUrl = "url", link = "link", section = "M")
            )
        )

        coEvery { appRepository.setInAppBannerData(bannerMap) } returns flow {
            emit(DataResource.Loading())
            emit(DataResource.Error(exception))
        }

        useCase(bannerMap).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val error = awaitItem() as DataResource.Error
            assertThat(error.throwable).isEqualTo(exception)
            awaitComplete()
        }
    }
}