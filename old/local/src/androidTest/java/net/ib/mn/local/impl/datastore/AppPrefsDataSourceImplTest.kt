package net.ib.mn.local.impl.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import net.ib.mn.data.model.InAppBannerPrefsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AppPrefsDataSourceImplTest {

    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var dataSource: AppPrefsDataSourceImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val file = File(context.filesDir, "test_app_prefs_${UUID.randomUUID()}.preferences_pb")
        testDataStore = PreferenceDataStoreFactory.create(
            produceFile = { file }
        )
        dataSource = AppPrefsDataSourceImpl(testDataStore)
    }

    @Test
    fun get_returns_empty_list_when_no_data_set() = runTest {
        val resultMenu = dataSource.getMenuBannerData()
        val resultSearch = dataSource.getSearchBannerData()

        assertTrue(resultMenu.isEmpty())
        assertTrue(resultSearch.isEmpty())
    }

    @Test
    fun save_and_get_menu_and_search_banner_data() = runTest {
        val inputMap = mapOf(
            "M" to listOf(
                InAppBannerPrefsEntity(1, "url1", "link1", "M"),
                InAppBannerPrefsEntity(2, "url2", "link2", "M")
            ),
            "S" to listOf(
                InAppBannerPrefsEntity(3, "url3", "link3", "S")
            )
        )

        dataSource.setBannerData(inputMap)

        val menuResult = dataSource.getMenuBannerData()
        val searchResult = dataSource.getSearchBannerData()

        assertEquals(inputMap["M"], menuResult)
        assertEquals(inputMap["S"], searchResult)
    }

    @Test
    fun get_returns_empty_list_when_malformed_json_exists() = runTest {
        val key = stringPreferencesKey("type_menu")
        testDataStore.edit {
            it[key] = "malformed-json"
        }

        val result = dataSource.getMenuBannerData()
        assertTrue(result.isEmpty())
    }

    @Test
    fun initAdData_sets_ad_count_zero_and_date_correctly() = runTest {
        val testDate = 1234567890L

        dataSource.initAdData(testDate)

        val result = dataSource.getAdData()

        assertEquals(0, result.first)
        assertEquals(0, result.second)
        assertEquals(testDate, result.third)
    }

    @Test
    fun updateAdCount_updates_ad_count_and_max_count_correctly() = runTest {
        val currentCount = 2
        val maxCount = 5

        dataSource.updateAdCount(currentCount, maxCount)

        val result = dataSource.getAdData()

        assertEquals(currentCount, result.first)
        assertEquals(maxCount, result.second)
    }

    @Test
    fun getAdData_returns_defaults_when_no_values_set() = runTest {
        val result = dataSource.getAdData()

        assertEquals(0, result.first)
        assertEquals(0, result.second)
        assertEquals(0L, result.third)
    }

    @Test
    fun isEnabledVideoAd_returns_true_when_adCount_less_than_maxCount() = runTest {
        dataSource.updateAdCount(currentCount = 1, maxCount = 5)

        val result = dataSource.isEnabledVideoAd()

        assertTrue(result)
    }

    @Test
    fun isEnabledVideoAd_returns_false_when_adCount_equals_maxCount() = runTest {
        dataSource.updateAdCount(currentCount = 3, maxCount = 3)

        val result = dataSource.isEnabledVideoAd()

        assertFalse(result)
    }

    @Test
    fun isEnabledVideoAd_returns_true_when_maxCount_is_zero() = runTest {
        dataSource.updateAdCount(currentCount = 3, maxCount = 0)

        val result = dataSource.isEnabledVideoAd()

        assertTrue(result)
    }

    @Test
    fun getAdCount_returns_zero_when_no_value_set() = runTest {
        val result = dataSource.getAdCount()
        assertEquals(0, result)
    }

    @Test
    fun getAdCount_returns_correct_value_when_value_is_set() = runTest {
        testDataStore.edit {
            it[intPreferencesKey("ad_count")] = 4
        }

        val result = dataSource.getAdCount()
        assertEquals(4, result)
    }

    @Test(expected = Exception::class)
    fun getAdCount_throws_exception_when_datastore_throws() = runTest {
        // 예외 상황 테스트를 위해 mock DataStore를 사용하는 것이 일반적이나,
        // 여기서는 간단하게 재구성하여 던지는 예외를 시뮬레이션
        val faultyDataStore = object : DataStore<Preferences> {
            override val data: Flow<Preferences>
                get() = flow { throw IOException("DataStore failure") }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                throw NotImplementedError()
            }
        }

        val faultyDataSource = AppPrefsDataSourceImpl(faultyDataStore)
        faultyDataSource.getAdCount() // 예외 발생 예상
    }

    @Test
    fun setAdNotification_sets_value_correctly() = runTest {
        dataSource.setAdNotification(true)

        val result = dataSource.isSetAdNotification()

        assertTrue(result)
    }

    @Test
    fun isSetAdNotification_returns_false_when_no_value_set() = runTest {
        val result = dataSource.isSetAdNotification()

        assertFalse(result)
    }
}