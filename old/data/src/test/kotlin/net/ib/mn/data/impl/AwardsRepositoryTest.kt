package net.ib.mn.data.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ib.mn.data.local.AwardsLocalDataSource
import net.ib.mn.data.model.toIdolEntity
import net.ib.mn.data_resource.DataResource
import net.ib.mn.domain.model.Idol
import net.ib.mn.domain.repository.AwardsRepository
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AwardsRepositoryTest {

    private lateinit var repository: AwardsRepository
    private lateinit var localDataSource: AwardsLocalDataSource

    @Before
    fun setup() {
        localDataSource = mockk()
        repository = AwardsRepositoryImpl(localDataSource)
    }

    @Test
    fun `getById emits Loading and Success with expected data`() = runTest {
        val dummyIdol = createDummyIdol(1)
        coEvery { localDataSource.getById(1) } returns dummyIdol.toIdolEntity()

        val result = repository.getById(1).toList()

        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
        assertThat((result[1] as DataResource.Success).data).isEqualTo(dummyIdol)
    }

    @Test
    fun `getAll emits Loading and Success with list`() = runTest {
        val dummyList = listOf(createDummyIdol(1).toIdolEntity(), createDummyIdol(2).toIdolEntity())
        coEvery { localDataSource.getAll() } returns dummyList

        val result = repository.getAll().toList()

        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)

        val actualData = (result[1] as DataResource.Success).data
        val expectedData = dummyList.map { it.toDomain() }

        assertThat(actualData).containsExactlyElementsIn(expectedData)
    }

    @Test
    fun `insert emits Loading and Success`() = runTest {
        val idols = listOf(createDummyIdol(1))
        coEvery { localDataSource.insert(any()) } returns Unit

        val result = repository.insert(idols).toList()

        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
        assertThat((result[1] as DataResource.Success).data).isEqualTo(Unit)
    }

    @Test
    fun `deleteAll emits Loading and Success`() = runTest {
        coEvery { localDataSource.deleteAll() } returns Unit

        val result = repository.deleteAll().toList()

        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat((result[1] as DataResource.Success).data).isEqualTo(Unit)
    }

    @Test
    fun `deleteAllAndInsert emits Loading and Success`() = runTest {
        val idols = listOf(createDummyIdol(1))
        coEvery { localDataSource.deleteAllAndInsert(any()) } returns Unit

        val result = repository.deleteAllAndInsert(idols).toList()

        assertThat(result[0]).isInstanceOf(DataResource.Loading::class.java)
        assertThat(result[1]).isInstanceOf(DataResource.Success::class.java)
    }

    private fun createDummyIdol(id: Int): Idol {
        return Idol(
            id = id,
            miracleCount = 0,
            angelCount = 0,
            rookieCount = 0,
            anniversary = "N",
            anniversaryDays = null,
            birthDay = "",
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
}
