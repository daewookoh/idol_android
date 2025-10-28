package net.ib.mn.local.room.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.ib.mn.local.model.IdolLocal
import net.ib.mn.local.room.AwardsDatabase
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AwardsIdolDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var db: AwardsDatabase

    private lateinit var dao: AwardsIdolDao

    @Before
    fun setup() {
        hiltRule.inject()
        dao = db.awardsIdolDao()
        runBlocking {
            dao.deleteAll()
        }
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetById_returnsCorrectData() = runTest {
        val idol = createDummyIdol(1)
        dao.insert(listOf(idol))

        val loaded = dao.getById(1)

        assertEquals(idol.id, loaded!!.id)
        assertEquals(idol.name, loaded.name)
    }

    @Test
    fun deleteAllAndInsert_replacesOldData() = runTest {
        dao.insert(listOf(createDummyIdol(1)))
        dao.deleteAllAndInsert(listOf(createDummyIdol(2)))

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals(2, all[0].id)
    }

    @Test
    fun getAll_onEmptyDb_returnsEmptyList() = runTest {
        val all = dao.getAll()
        assertTrue(all.isEmpty())
//        assertEquals(0, all.size)
    }

    @Test
    fun getIdolById_onEmptyDb_returnsNull() = runTest {
        val idol = dao.getById(-1)
        assertNull(idol)
    }

    private fun createDummyIdol(id: Int): IdolLocal {
        return IdolLocal(id = id, name = "Test Idol", description = "desc")
    }
}
