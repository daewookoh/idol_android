package net.ib.mn.local.di

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.ib.mn.data.local.datastore.IdolPrefsDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
@ExperimentalCoroutinesApi
class IdolPrefsDataSourceHiltTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var dataSource: IdolPrefsDataSource

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun getIdolChartCodePrefs_should_return_emptyMap_when_no_data() = runTest {
        val result = dataSource.getIdolChartCodePrefs()
        assertTrue(result.isEmpty())
    }

    @Test
    fun save_and_getIdolChartCodePrefs_should_work_correctly() = runTest {
        // Given
        val testData = mapOf(
            "chart1" to listOf("idol1", "idol2"),
            "chart2" to listOf("idol3", "idol4", "idol5")
        )

        // When
        dataSource.saveIdolChartCodePrefs(testData)
        val result = dataSource.getIdolChartCodePrefs()

        // Then
        assertEquals(testData.keys, result.keys)
        testData.forEach { (key, value) ->
            assertEquals(value, result[key])
        }
    }
}
