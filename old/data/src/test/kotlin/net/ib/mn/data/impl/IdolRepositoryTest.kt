package net.ib.mn.data.impl

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.ib.mn.data.local.IdolLocalDataSource
import net.ib.mn.data.local.datastore.IdolPrefsDataSource
import net.ib.mn.data.model.toIdolEntity
import net.ib.mn.data.toDomain
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.*
import net.ib.mn.domain.repository.IdolRepository
import org.junit.Before
import org.junit.Test

class IdolRepositoryTest {

    private lateinit var repository: IdolRepository
    private val localDataSource: IdolLocalDataSource = mockk(relaxed = true)
    private val idolPrefsDataSource: IdolPrefsDataSource = mockk(relaxed = true)

    @Before
    fun setup() {
        repository = IdolRepositoryImpl(localDataSource, idolPrefsDataSource)
    }

    @Test
    fun `getAllIdols emits Loading and Success`() = runTest {
        val idols = listOf(createDummyIdol(1).toIdolEntity())
        coEvery { localDataSource.getAll() } returns idols

        repository.getAllIdols().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem() as DataResource.Success
            assertThat(success.data).isEqualTo(idols.toDomain())
            awaitComplete()
        }
    }

    @Test
    fun `saveIdols calls localDataSource with mapped data`() = runTest {
        val idols = listOf(createDummyIdol(1))
        coEvery { localDataSource.saveIdols(any()) } returns Unit

        repository.saveIdols(idols).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(DataResource.Success::class.java)
            awaitComplete()
        }
        coVerify { localDataSource.saveIdols(idols.map { it.toIdolEntity() }) }
    }

    @Test
    fun `deleteAllAndSaveIdols calls deleteAllAndInsert`() = runTest {
        val idols = listOf(createDummyIdol(1))
        coEvery { localDataSource.deleteAllAndInsert(any()) } returns Unit

        repository.deleteAllAndSaveIdols(idols).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(DataResource.Success::class.java)
            awaitComplete()
        }
        coVerify { localDataSource.deleteAllAndInsert(idols.map { it.toIdolEntity() }) }
    }

    @Test
    fun `getIdolById returns idol`() = runTest {
        val idol = createDummyIdol(1).toIdolEntity()
        coEvery { localDataSource.getIdolById(1) } returns idol

        repository.getIdolById(1).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val result = awaitItem() as DataResource.Success
            assertThat(result.data).isEqualTo(idol.toDomain())
            awaitComplete()
        }
    }

    @Test
    fun `updateHeartAndTop3 calls update`() = runTest {
        val filed = IdolFiledData(id = 1, heart = 100, top3 = "1,2,3", top3Type = "P,P,P", top3ImageVer = "img1", imageUrl = null, imageUrl2 = null, imageUrl3 = null)
        val list = listOf(filed)
        coEvery { localDataSource.update(any(), any(), any()) } returns Unit

        repository.updateHeartAndTop3("cdn", "300", list).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(DataResource.Success::class.java)
            awaitComplete()
        }
    }

    @Test
    fun `getViewableIdols returns idol list`() = runTest {
        val idols = listOf(createDummyIdol(1).toIdolEntity())
        coEvery { localDataSource.getViewableIdols() } returns idols

        repository.getViewableIdols().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val success = awaitItem() as DataResource.Success
            assertThat(success.data).isEqualTo(idols.toDomain())
            awaitComplete()
        }
    }

    private fun createDummyIdol(id: Int = 1): Idol {
        return Idol(
            id = id,
            miracleCount = 0,
            angelCount = 0,
            rookieCount = 0,
            anniversary = "N",
            anniversaryDays = null,
            birthDay = null,
            burningDay = null,
            category = "",
            comebackDay = null,
            debutDay = null,
            description = "desc",
            fairyCount = 0,
            groupId = 0,
            heart = 0L,
            imageUrl = null,
            imageUrl2 = null,
            imageUrl3 = null,
            isViewable = "Y",
            name = "Idol $id",
            nameEn = "",
            nameJp = "",
            nameZh = "",
            nameZhTw = "",
            resourceUri = "",
            top3 = null,
            top3Type = null,
            top3Seq = -1,
            type = "",
            infoSeq = -1,
            isLunarBirthday = null,
            mostCount = 0,
            mostCountDesc = null,
            updateTs = 0,
            sourceApp = null,
            fdName = null,
            fdNameEn = null,
            top3ImageVer = ""
        )
    }

    @Test
    fun `getIdolChartCodes emits Loading and Success`() = runTest {
        val chartCodes = mapOf("code1" to arrayListOf("a", "b"), "code2" to arrayListOf("c"))
        coEvery { idolPrefsDataSource.getIdolChartCodePrefs() } returns chartCodes

        repository.getIdolChartCodes().test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            val result = awaitItem() as DataResource.Success
            assertThat(result.data).isEqualTo(chartCodes)
            awaitComplete()
        }
    }

    @Test
    fun `saveIdolChartCodes saves data and emits Loading and Success`() = runTest {
        val input = mapOf("group1" to listOf("1", "2"), "group2" to listOf("3"))
        coEvery { idolPrefsDataSource.saveIdolChartCodePrefs(input) } returns Unit

        repository.saveIdolChartCodes(input).test {
            assertThat(awaitItem()).isInstanceOf(DataResource.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(DataResource.Success::class.java)
            awaitComplete()
        }
        coVerify { idolPrefsDataSource.saveIdolChartCodePrefs(input) }
    }
}
