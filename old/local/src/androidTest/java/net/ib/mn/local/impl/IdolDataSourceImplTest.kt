package net.ib.mn.local.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.ib.mn.data.model.AnniversaryEntity
import net.ib.mn.data.model.IdolEntity
import net.ib.mn.data.model.IdolFiledDataEntity
import net.ib.mn.local.room.IdolDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class IdolDataSourceImplTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var idolDatabase: IdolDatabase

    private lateinit var dataSource: IdolLocalDataSourceImpl

    @Before
    fun setup() {
        hiltRule.inject()
        dataSource = IdolLocalDataSourceImpl(idolDatabase)
    }

    private fun createIdol(id: Int, name: String = "아이돌$id", updateTs: Int = 0, viewable: String = "N", type: String = "", category: String = "") =
        IdolEntity(
            id = id,
            miracleCount = 0,
            angelCount = 0,
            rookieCount = 0,
            anniversary = "anii",
            anniversaryDays = null,
            birthDay = null,
            burningDay = null,
            debutDay = null,
            category = category,
            comebackDay = null,
            imageUrl = null,
            imageUrl2 = null,
            imageUrl3 = null,
            description = "desc",
            groupId = 0,
            fairyCount = 0,
            heart = 0,
            isViewable = viewable,
            nameEn = "",
            nameJp = "",
            nameZh = "",
            nameZhTw = "",
            name = "",
            resourceUri = "",
            top3Seq = 1,
            mostCount = 1,
            updateTs = 1,
            top3 = null,
            top3Type = null,
            isLunarBirthday = null,
            mostCountDesc = null,
            sourceApp = null,
            infoSeq = 0,
            type = type,
            fdName = null,
            fdNameEn = null,
        )

    @Test
    fun saveIdolAndGetById_success() = runTest {
        val idol = createIdol(1)
        dataSource.saveIdol(idol)

        val loaded = dataSource.getIdolById(1)

        assertEquals(idol.id, loaded?.id)
        assertEquals(idol.name, loaded?.name)
    }

    @Test
    fun saveIdolsAndGetAll_success() = runTest {
        val idols = listOf(createIdol(1), createIdol(2))
        dataSource.saveIdols(idols)

        val loaded = dataSource.getAll()
        assertEquals(2, loaded.size)
    }

    @Test
    fun deleteAllAndInsert_overwritesData() = runTest {
        dataSource.saveIdols(listOf(createIdol(1)))
        dataSource.deleteAllAndInsert(listOf(createIdol(2)))

        val loaded = dataSource.getAll()
        assertEquals(1, loaded.size)
        assertEquals(2, loaded[0].id)
    }

    @Test
    fun update_success() = runTest {
        val idol = createIdol(1, name = "Before")
        dataSource.saveIdol(idol)

        val updated = idol.copy(name = "After")
        dataSource.update(updated)

        val loaded = dataSource.getIdolById(1)
        assertEquals("After", loaded?.name)
    }

    @Test
    fun upsertWithTs_insertsAndUpdatesCorrectly() = runTest {
        val newIdol = createIdol(1, updateTs = 0)
        val updatedIdol = newIdol.copy(updateTs = 10, name = "Updated")

        // 첫 insert
        val first = dataSource.upsertWithTs(listOf(newIdol), ts = 5)
        assertTrue(first)

        // 업데이트 됨 (updateTs < ts)
        val second = dataSource.upsertWithTs(listOf(updatedIdol), ts = 10)
        assertTrue(second)

        // 업데이트 안됨 (같거나 더 큼)
        val third = dataSource.upsertWithTs(listOf(updatedIdol.copy(updateTs = 10)), ts = 10)
        assertFalse(third)
    }

    @Test
    fun update_shouldGenerateCorrectUrlsAndUpdateDao() = runTest {
        val entity = createIdol(1, name = "Before")
        dataSource.saveIdol(entity)

        val filedData = IdolFiledDataEntity(
            id = 1,
            heart = 100,
            top3 = "101,102",
            top3Type = "P,V"
        )

        dataSource.update(
            cdnUrl = "https://cdn.example.com",
            reqImageSize = "small",
            idolFiledDataList = listOf(filedData)
        )

        val updated = dataSource.getIdolById(1)!!
        assertEquals("https://cdn.example.com/a/101.1_small.webp", updated.imageUrl)
        assertEquals("https://cdn.example.com/a/102.1_small.mp4", updated.imageUrl2)
        assertEquals(null, updated.imageUrl3)
        assertEquals(100, updated.heart)
        assertEquals("101,102", updated.top3)
        assertEquals("P,V", updated.top3Type)
    }

    @Test
    fun updateAnniversaries_shouldUpdateImageUrlsAndAnniversaryFields() = runTest {
        val entity = createIdol(1)
        dataSource.saveIdol(entity)

        val model = AnniversaryEntity(
            idolId = 1,
            top3 = "301,302,303",
            top3Type = "P,,V",
            anniversary = "D",
            anniversaryDays = 5,
            heart = 888,
            burningDay = "2025-04-01"
        )

        dataSource.updateAnniversaries(
            cdnUrl = "https://cdn.example.com",
            reqImageSize = "large",
            anniversaries = listOf(model)
        )

        val updated = dataSource.getIdolById(1)!!
        assertEquals("https://cdn.example.com/a/301.1_large.webp", updated.imageUrl)
        assertEquals("https://cdn.example.com/a/303.1_large.mp4", updated.imageUrl3)
        assertEquals(888, updated.heart)
        assertEquals("D", updated.anniversary)
        assertEquals(5, updated.anniversaryDays)
        assertEquals("2025-04-01", updated.burningDay)
    }

    @Test
    fun getIdolById_returnsCorrectData() = runTest {
        val idol = createIdol(1)
        dataSource.saveIdol(idol)

        val result = dataSource.getIdolById(1)
        assertEquals(idol.id, result?.id)
    }

    @Test
    fun getIdolsByIds_returnsSortedByHeart() = runTest {
        val idols = listOf(createIdol(1, "name1"), createIdol(2, "name2").copy(heart = 1000))
        dataSource.saveIdols(idols)

        val result = dataSource.getIdolsByIds(listOf(1, 2))
        assertEquals(2, result.size)
        assertEquals(2, result[0].id)
    }

    @Test
    fun getViewableIdols_returnsOnlyVisible() = runTest {
        dataSource.saveIdols(listOf(createIdol(1, viewable = "Y"), createIdol(2, viewable = "N")))
        val result = dataSource.getViewableIdols()
        assertEquals(1, result.size)
        assertEquals(1, result[0].id)
    }

    @Test
    fun getIdolByTypeAndCategory_returnsCorrect() = runTest {
        dataSource.saveIdol(createIdol(1, viewable = "Y", type = "A", category = "F"))
        val result = dataSource.getIdolByTypeAndCategory("A", "F")
        assertEquals(1, result.size)
    }

    @Test
    fun getIdolByCategory_returnsCorrect() = runTest {
        dataSource.saveIdol(createIdol(1, viewable = "Y" , category = "Solo"))
        val result = dataSource.getIdolByCategory("Solo")
        assertEquals(1, result.size)
    }

    @Test
    fun getIdolByType_returnsCorrect() = runTest {
        dataSource.saveIdol(createIdol(1, viewable = "Y", type = "Group"))
        val result = dataSource.getIdolByType("Group")
        assertEquals(1, result.size)
    }

    @Test
    fun getIdolsByIdList_returnsAll() = runTest {
        dataSource.saveIdols(listOf(createIdol(1), createIdol(2)))
        val result = dataSource.getIdolsByIdList(listOf(1, 2))
        assertEquals(2, result.size)
    }

    @Test
    fun upsert_shouldInsertOrUpdate() = runTest {
        val idol = createIdol(1)
        dataSource.upsert(idol)
        val updated = idol.copy(name = "Updated Idol")
        dataSource.upsert(updated)

        val result = dataSource.getIdolById(1)
        assertEquals("Updated Idol", result?.name)
    }

    @Test
    fun deleteAll_removesEverything() = runTest {
        dataSource.saveIdol(createIdol(1))
        dataSource.deleteAll()
        val result = dataSource.getAll()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteAllAndInsert_replacesCorrectly() = runTest {
        dataSource.saveIdol(createIdol(1))
        dataSource.deleteAllAndInsert(listOf(createIdol(2)))
        val result = dataSource.getAll()
        assertEquals(1, result.size)
        assertEquals(2, result[0].id)
    }
}