package net.ib.mn.local.impl.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class IdolPrefsDataSourceImplTest {

    private lateinit var context: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var dataSource: IdolPrefsDataSourceImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val file = File(context.filesDir, "test_idol_prefs_${UUID.randomUUID()}.preferences_pb")
        testDataStore = PreferenceDataStoreFactory.create(
            produceFile = { file }
        )
        dataSource = IdolPrefsDataSourceImpl(testDataStore)
    }

    @Test
    fun get_returns_empty_map_when_no_data_is_set() = runTest {
        val result = dataSource.getIdolChartCodePrefs()
        assertTrue(result.isEmpty())
    }

    @Test
    fun save_and_get_returns_correct_data() = runTest {
        val input = mapOf(
            "boyGroup" to arrayListOf("bts", "exo"),
            "girlGroup" to arrayListOf("blackpink", "newjeans")
        )

        dataSource.saveIdolChartCodePrefs(input)
        val result = dataSource.getIdolChartCodePrefs()

        assertEquals(input, result)
    }

    @Test
    fun get_returns_empty_map_when_data_is_malformed() = runTest {
        val key = stringPreferencesKey("IDOL_CHARTS_CODES")
        testDataStore.edit {
            it[key] = "invalid-json"
        }

        val result = dataSource.getIdolChartCodePrefs()
        assertTrue(result.isEmpty())
    }
}
