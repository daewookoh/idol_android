package net.ib.mn.local.room.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.ib.mn.local.model.IdolLocal
import net.ib.mn.local.room.IdolDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class IdolDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var db: IdolDatabase

    private lateinit var dao: IdolDao

    @Before
    fun setup() {
        hiltRule.inject()
        dao = db.idolDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun createIdol(
        id: Int,
        name: String = "Idol $id",
        heart: Long = 0L,
        viewable: Boolean = true
    ): IdolLocal {
        return IdolLocal(
            id = id,
            name = name,
            heart = heart,
            isViewable = if (viewable) "Y" else "N"
        )
    }

    @Test
    fun testInsertAndGetIdolById() = runTest {
        val idol = createIdol(1)
        dao.insert(idol)

        val result = dao.getIdolById(1)
        assertEquals(idol.id, result?.id)
    }

    @Test
    fun testInsertAndGetIdolsByIds() = runTest {
        val idols = listOf(createIdol(1, heart = 50), createIdol(2, heart = 100))
        dao.insert(idols)

        val result = dao.getIdolsByIds(listOf(1, 2))
        assertEquals(2, result.size)
        assertEquals(2, result[0].id) // Ordered by heart desc
    }

    @Test
    fun testGetViewableIdols() = runTest {
        dao.insert(listOf(createIdol(1), createIdol(2, viewable = false)))
        val result = dao.getViewableIdols()
        assertEquals(1, result.size)
        assertEquals(1, result[0].id)
    }

    @Test
    fun testGetAll() = runTest {
        dao.insert(listOf(createIdol(1), createIdol(2)))
        val result = dao.getAll()
        assertEquals(2, result.size)
    }

    @Test
    fun testGetByTypeAndCategory() = runTest {
        dao.insert(createIdol(1).copy(type = "A", category = "F"))
        val result = dao.getIdolByTypeAndCategory("A", "F")
        assertEquals(1, result.size)
    }

    @Test
    fun testGetByCategory() = runTest {
        dao.insert(createIdol(1).copy(category = "F"))
        val result = dao.getByCategory("F")
        assertEquals(1, result.size)
    }

    @Test
    fun testGetByType() = runTest {
        dao.insert(createIdol(1).copy(type = "A"))
        val result = dao.getByType("A")
        assertEquals(1, result.size)
    }

    @Test
    fun testGetByIdList() = runTest {
        dao.insert(listOf(createIdol(1), createIdol(2)))
        val result = dao.getId(listOf(1, 2))
        assertEquals(2, result.size)
    }

    @Test
    fun testDeleteAllAndInsert() = runTest {
        dao.insert(createIdol(1))
        dao.deleteAllAndInsert(listOf(createIdol(2)))
        val result = dao.getAll()
        assertEquals(1, result.size)
        assertEquals(2, result[0].id)
    }

    @Test
    fun testUpdate() = runTest {
        val idol = createIdol(1)
        dao.insert(idol)

        val updated = idol.copy(name = "Updated Idol")
        dao.update(updated)

        val result = dao.getIdolById(1)
        assertEquals("Updated Idol", result?.name)
    }

    @Test
    fun testUpdateIdols() = runTest {
        val idols = listOf(createIdol(1), createIdol(2))
        dao.insert(idols)

        val updated = idols.map { it.copy(name = "Updated ${it.id}") }
        dao.updateIdols(updated)

        val result = dao.getAll()
        assertEquals("Updated 1", result[0].name)
    }

    @Test
    fun testUpdateFields() = runTest {
        dao.insert(createIdol(1))
        dao.update(1, 999L, "top3", "top3Type", "url1", "url2", "url3")
        val result = dao.getIdolById(1)
        assertEquals(999L, result?.heart)
        assertEquals("top3", result?.top3)
    }

    @Test
    fun testUpsert() = runTest {
        dao.upsert(createIdol(1))
        dao.upsert(createIdol(1, name = "Updated"))
        val result = dao.getIdolById(1)
        assertEquals("Updated", result?.name)
    }
}