package net.ib.mn.local.impl.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class FreeBoardPrefsDataSourceImplTest {

    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var dataSource: FreeBoardPrefsDataSourceImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        val fileName = "test_prefs_${UUID.randomUUID()}.preferences_pb"
        testDataStore = PreferenceDataStoreFactory.create(
            produceFile = {
                File(context.filesDir, fileName)
            }
        )
        dataSource = FreeBoardPrefsDataSourceImpl(testDataStore)
    }

    @After
    fun teardown() {
        runBlocking {
            context.filesDir.resolve("test_free_board_prefs.preferences_pb").delete()
        }
    }

    @Test
    fun returns_default_values_when_no_data_is_set() = runTest {
        val result = dataSource.getFreeBoardPreference()
        assertEquals(null, result.selectLanguage)
        assertEquals("", result.selectLanguageId)
    }

    @Test
    fun saves_and_retrieves_language_and_languageId_correctly() = runTest {
        val language = "ko"
        val languageId = "5"

        dataSource.setFreeBoardSelectLanguagePrefs(language, languageId)
        val result = dataSource.getFreeBoardPreference()

        assertEquals(language, result.selectLanguage)
        assertEquals(languageId, result.selectLanguageId)
    }

    @Test
    fun returns_fallback_values_for_unset_preferences() = runTest {
        val result = dataSource.getFreeBoardPreference()

        assertNotNull(result)
        assertEquals(null, result.selectLanguage)
        assertEquals("", result.selectLanguageId)
    }

    @Test
    fun last_set_value_should_persist_correctly() = runTest {
        dataSource.setFreeBoardSelectLanguagePrefs("en", "en_id")
        dataSource.setFreeBoardSelectLanguagePrefs("ja", "ja_id")

        val result = dataSource.getFreeBoardPreference()

        assertEquals("ja", result.selectLanguage)
        assertEquals("ja_id", result.selectLanguageId)
    }
}
