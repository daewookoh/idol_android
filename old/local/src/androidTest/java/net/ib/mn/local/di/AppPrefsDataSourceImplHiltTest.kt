package net.ib.mn.local.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import net.ib.mn.data.model.InAppBannerPrefsEntity
import net.ib.mn.local.impl.datastore.AppPrefsDataSourceImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Named

@HiltAndroidTest
class AppPrefsDataSourceImplHiltTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    @Named(QualifierNames.APP_PREFS)
    lateinit var dataStore: DataStore<Preferences>

    private lateinit var dataSource: AppPrefsDataSourceImpl

    @Before
    fun setUp() {
        hiltRule.inject()
        dataSource = AppPrefsDataSourceImpl(dataStore)
    }

    @Test
    fun injected_dataSource_should_return_empty_banner_lists_when_not_set() = runTest {
        val menuResult = dataSource.getMenuBannerData()
        val searchResult = dataSource.getSearchBannerData()

        assertTrue(menuResult.isEmpty())
        assertTrue(searchResult.isEmpty())
    }

    @Test
    fun injected_dataSource_should_persist_banner_data() = runTest {
        val bannerData = mapOf(
            "M" to listOf(
                InAppBannerPrefsEntity(1, "url1", "link1", "M")
            ),
            "S" to listOf(
                InAppBannerPrefsEntity(2, "url2", "link2", "S")
            )
        )

        dataSource.setBannerData(bannerData)

        val resultMenu = dataSource.getMenuBannerData()
        val resultSearch = dataSource.getSearchBannerData()

        assertEquals(bannerData["M"], resultMenu)
        assertEquals(bannerData["S"], resultSearch)
    }

    @Test
    fun injected_dataSource_should_initialize_ad_data_correctly() = runTest {
        val testDate = 1234567890L

        dataSource.initAdData(testDate)

        val result = dataSource.getAdData()

        assertEquals(0, result.first)
        assertEquals(0, result.second)
        assertEquals(testDate, result.third)
    }

    @Test
    fun injected_dataSource_should_update_ad_count_correctly() = runTest {
        val currentCount = 2
        val maxCount = 7

        dataSource.updateAdCount(currentCount, maxCount)

        val result = dataSource.getAdData()

        assertEquals(currentCount, result.first)
        assertEquals(maxCount, result.second)
    }

    @Test
    fun injected_dataSource_should_return_correct_video_ad_enabled_status() = runTest {
        dataSource.updateAdCount(currentCount = 1, maxCount = 3)
        assertTrue(dataSource.isEnabledVideoAd())

        dataSource.updateAdCount(currentCount = 3, maxCount = 3)
        assertFalse(dataSource.isEnabledVideoAd())

        dataSource.updateAdCount(currentCount = 10, maxCount = 0)
        assertTrue(dataSource.isEnabledVideoAd())
    }
}
