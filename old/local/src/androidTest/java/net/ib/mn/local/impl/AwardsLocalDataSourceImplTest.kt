package net.ib.mn.local.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.ib.mn.data.local.AwardsLocalDataSource
import net.ib.mn.data.model.IdolEntity
import net.ib.mn.local.room.AwardsDatabase
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class AwardsLocalDataSourceImplTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var awardsDatabase: AwardsDatabase

    @Inject
    lateinit var dataSource: AwardsLocalDataSource

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun insertAndGetById_returnsCorrectData() = runTest {
        val dummy = createDummyEntity(1)
        dataSource.insert(listOf(dummy))

        val loaded = dataSource.getById(1)

        assertEquals(dummy.id, loaded!!.id)
        assertEquals(dummy.name, loaded.name)
    }

    @Test
    fun deleteAllAndInsert_replacesOldData() = runTest {
        dataSource.insert(listOf(createDummyEntity(1)))
        dataSource.deleteAllAndInsert(listOf(createDummyEntity(2)))

        val all = dataSource.getAll()
        assertEquals(1, all!!.size)
        assertEquals(2, all[0].id)
    }

    private fun createDummyEntity(id: Int): IdolEntity {
        return IdolEntity(
            id = id,
            miracleCount = 0,
            angelCount = 0,
            rookieCount = 0,
            anniversary = "anii",
            anniversaryDays = null,
            birthDay = null,
            burningDay = null,
            debutDay = null,
            category = "",
            comebackDay = null,
            imageUrl = null,
            imageUrl2 = null,
            imageUrl3 = null,
            description = "desc",
            groupId = 0,
            fairyCount = 0,
            heart = 0,
            isViewable = "Y",
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
            type = "",
            fdName = null,
            fdNameEn = null,
        )
    }
}