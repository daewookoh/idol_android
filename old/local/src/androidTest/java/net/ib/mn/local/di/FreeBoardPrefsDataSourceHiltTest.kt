package net.ib.mn.local.di

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import net.ib.mn.data.local.datastore.FreeBoardPrefsDataSource
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class FreeBoardPrefsDataSourceHiltTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var dataSource: FreeBoardPrefsDataSource

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun injected_dataSource_should_return_default_values() = runTest {
        val result = dataSource.getFreeBoardPreference()

        assertEquals(null, result.selectLanguage)
        assertEquals("", result.selectLanguageId)
    }

    @Test
    fun injected_dataSource_should_persist_and_read_values() = runTest {
        // Given
        val testLanguage = "ko"
        val testLanguageId = "100"

        // When
        dataSource.setFreeBoardSelectLanguagePrefs(testLanguage, testLanguageId)
        val result = dataSource.getFreeBoardPreference()

        // Then
        assertEquals(testLanguage, result.selectLanguage)
        assertEquals(testLanguageId, result.selectLanguageId)
    }
}
